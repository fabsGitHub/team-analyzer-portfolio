package com.teamanalyzer.teamanalyzer.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.teamanalyzer.teamanalyzer.domain.User;

/**
 * Upgrade paths for V3 (canonical, byte-compared emails) and V4 (LEADER role backfill), each on
 * its own fresh database in one MySQL container.
 */
@Testcontainers
class V3V4MigrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6");

    @Test
    void v3_canonicalises_legacy_addresses_like_the_application_does() throws Exception {
        String db = freshDatabase("v3_canonical");
        flyway(db, "2").migrate();
        List<String> legacy = List.of(
                "josé@example.test",      // NFD, stored as typed before NFC normalisation existed
                "ΟΔΟΣ@example.test", // Greek capital: Java gives final sigma, SQL LOWER does not
                "İNCİ@example.test",   // dotted capital I
                " Mixed.Case@Example.test ");
        try (Connection c = connect(db); Statement st = c.createStatement()) {
            for (String email : legacy) {
                st.execute("insert into users (id, email, password_hash) values (uuid_to_bin(uuid()), '" + email + "', 'h')");
            }
        }

        flyway(db, null).migrate();

        List<String> stored = emails(db);
        for (String email : legacy) {
            assertThat(stored).contains(User.normalizeEmail(email));
        }
        assertThat(collation(db)).isEqualTo("utf8mb4_0900_bin");
    }

    @Test
    void v3_refuses_before_changing_anything_when_two_rows_would_collide() throws Exception {
        String db = freshDatabase("v3_conflict");
        flyway(db, "2").migrate();
        try (Connection c = connect(db); Statement st = c.createStatement()) {
            // Distinct under the old NO PAD collation, identical once trimmed.
            st.execute("insert into users (id, email, password_hash) values (uuid_to_bin(uuid()), 'carol@example.test', 'h')");
            st.execute("insert into users (id, email, password_hash) values (uuid_to_bin(uuid()), 'carol@example.test ', 'h')");
        }

        assertThatThrownBy(() -> flyway(db, null).migrate())
                .rootCause()
                .hasMessageContaining("carol@example.test")
                .hasMessageContaining("Merge or delete the duplicates");

        assertThat(collation(db)).isEqualTo("utf8mb4_0900_ai_ci"); // nothing was altered
        assertThat(emails(db)).containsExactlyInAnyOrder("carol@example.test", "carol@example.test ");
    }

    @Test
    void v3_also_canonicalises_when_the_column_is_already_binary() throws Exception {
        String db = freshDatabase("v3_binary");
        flyway(db, "2").migrate();
        try (Connection c = connect(db); Statement st = c.createStatement()) {
            // Like TiDB, whose default collation is utf8mb4_bin.
            st.execute("alter table users modify email varchar(254) character set utf8mb4 collate utf8mb4_bin not null");
            st.execute("insert into users (id, email, password_hash) values (uuid_to_bin(uuid()), 'Upper@Example.test', 'h')");
        }

        flyway(db, null).migrate();

        assertThat(emails(db)).containsExactly("upper@example.test");
        assertThat(collation(db)).isEqualTo("utf8mb4_bin"); // left as it was
    }

    @Test
    void v4_grants_the_leader_role_to_existing_team_leaders() throws Exception {
        String db = freshDatabase("v4_leader");
        flyway(db, "3").migrate();
        try (Connection c = connect(db); Statement st = c.createStatement()) {
            st.execute("set @lead = uuid_to_bin(uuid()), @member = uuid_to_bin(uuid()), @team = uuid_to_bin(uuid())");
            st.execute("insert into users (id, email, password_hash) values (@lead, 'lead@example.test', 'h'), (@member, 'member@example.test', 'h')");
            st.execute("insert into user_roles (user_id, role) values (@lead, 'USER'), (@member, 'USER')");
            st.execute("insert into teams (id, name) values (@team, 'T')");
            st.execute("insert into team_members (team_id, user_id, leader) values (@team, @lead, true), (@team, @member, false)");
        }

        flyway(db, null).migrate();

        try (Connection c = connect(db); Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("select u.email from user_roles r join users u on u.id = r.user_id where r.role = 'LEADER'")) {
            List<String> leaders = new ArrayList<>();
            while (rs.next()) {
                leaders.add(rs.getString(1));
            }
            assertThat(leaders).containsExactly("lead@example.test");
        }
    }

    // --- helpers ---

    private static String freshDatabase(String name) throws Exception {
        try (Connection c = DriverManager.getConnection(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword());
                Statement st = c.createStatement()) {
            st.execute("create database " + name);
        }
        return name;
    }

    private static String url(String db) {
        return MYSQL.getJdbcUrl().replaceFirst("/" + MYSQL.getDatabaseName(), "/" + db);
    }

    private static Connection connect(String db) throws Exception {
        return DriverManager.getConnection(url(db), "root", MYSQL.getPassword());
    }

    private static Flyway flyway(String db, String target) {
        var config = Flyway.configure()
                .dataSource(url(db), "root", MYSQL.getPassword())
                .locations("classpath:db/migration");
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    private static List<String> emails(String db) throws Exception {
        List<String> out = new ArrayList<>();
        try (Connection c = connect(db); Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("select email from users")) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
    }

    private static String collation(String db) throws Exception {
        try (Connection c = connect(db); Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("select collation_name from information_schema.columns"
                        + " where table_schema = database() and table_name = 'users' and column_name = 'email'")) {
            rs.next();
            return rs.getString(1);
        }
    }
}
