package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Makes account email addresses canonical and compares them byte-for-byte.
 *
 * <p>MySQL's default collation (utf8mb4_0900_ai_ci) is accent- and case-insensitive, so e.g.
 * {@code owner@exämple.test} matched the {@code owner@example.test} row. The application now
 * stores one canonical form per address and looks accounts up with exact comparisons, so the
 * column uses utf8mb4_0900_bin (binary and NO PAD).
 *
 * <ol>
 * <li>Every stored address is brought into the canonical form in Java, with the same rules as
 * {@code User.normalizeEmail} (SQL LOWER/TRIM differ from Java and do no Unicode normalisation).
 * If two rows would end up with the same address, the migration stops <em>before changing
 * anything</em> and lists them, so an operator can merge or remove one.</li>
 * <li>The collation change runs only where needed: TiDB already uses utf8mb4_bin and does not
 * support changing a column's collation. Step 1 runs everywhere and is idempotent.</li>
 * </ol>
 */
public class V3__users_email_binary_collation extends BaseJavaMigration {

    /** Frozen copy of User.normalizeEmail at the time of this migration. */
    static String canonical(String email) {
        String nfc = Normalizer.normalize(email.trim(), Normalizer.Form.NFC);
        return Normalizer.normalize(nfc.toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        // 1. Plan the canonicalisation and refuse on collisions before touching anything.
        Map<String, List<String>> idsByCanonical = new LinkedHashMap<>();
        Map<String, String> updates = new HashMap<>(); // hex(id) -> canonical email
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery("select hex(id), email from users")) {
            while (rs.next()) {
                String id = rs.getString(1);
                String email = rs.getString(2);
                String canonical = canonical(email);
                idsByCanonical.computeIfAbsent(canonical, k -> new ArrayList<>()).add(id + " '" + email + "'");
                if (!canonical.equals(email)) {
                    updates.put(id, canonical);
                }
            }
        }
        List<String> conflicts = idsByCanonical.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> e.getKey() + " <- " + String.join(", ", e.getValue()))
                .toList();
        if (!conflicts.isEmpty()) {
            throw new FlywayException("V3 stopped before changing anything: these accounts would share one email"
                    + " address. Merge or delete the duplicates (users.id in hex), then run the migration again:\n  "
                    + String.join("\n  ", conflicts));
        }

        // 2. Binary, NO PAD comparison (skipped where the column is already binary, e.g. TiDB).
        String collation = null;
        try (PreparedStatement ps = connection.prepareStatement("""
                select collation_name from information_schema.columns
                 where table_schema = database() and table_name = 'users' and column_name = 'email'""");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                collation = rs.getString(1);
            }
        }
        if (collation != null && !collation.toLowerCase(Locale.ROOT).endsWith("_bin")) {
            try (Statement st = connection.createStatement()) {
                st.execute("ALTER TABLE users MODIFY email VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL");
            }
        }

        // 3. Apply the canonical forms (no collisions possible, checked in step 1).
        try (PreparedStatement ps = connection.prepareStatement("update users set email = ? where id = unhex(?)")) {
            for (var update : updates.entrySet()) {
                ps.setString(1, update.getValue());
                ps.setString(2, update.getKey());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
