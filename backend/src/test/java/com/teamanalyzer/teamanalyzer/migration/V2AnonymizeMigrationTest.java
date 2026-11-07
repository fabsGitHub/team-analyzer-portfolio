package com.teamanalyzer.teamanalyzer.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Upgrade path for databases that already hold responses linked to tokens: after V2 the link
 * and the precise times are gone, and the rows were rewritten (MySQL's INSTANT column drop
 * would otherwise keep the old token_id bytes inside every existing row).
 */
@Testcontainers
class V2AnonymizeMigrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.6");

    @Test
    void v2_unlinks_and_rewrites_existing_responses() throws Exception {
        flyway("1").migrate();
        try (Connection c = root(); Statement st = c.createStatement()) {
            st.execute("SET @u = uuid_to_bin(uuid()), @t = uuid_to_bin(uuid()), @s = uuid_to_bin(uuid()),"
                    + " @q = uuid_to_bin(uuid()), @tok = uuid_to_bin(uuid()), @r = uuid_to_bin(uuid())");
            st.execute("INSERT INTO users (id, email, password_hash) VALUES (@u, 'm@example.test', 'h')");
            // A hand-inserted, non-canonical address (the application always stored lower case).
            st.execute("INSERT INTO users (id, email, password_hash) VALUES (uuid_to_bin(uuid()), ' Mixed.Case@Example.test', 'h')");
            st.execute("INSERT INTO teams (id, name) VALUES (@t, 'T')");
            st.execute("INSERT INTO surveys (id, team_id, title, created_by) VALUES (@s, @t, 'S', @u)");
            st.execute("INSERT INTO survey_questions (id, survey_id, idx, text) VALUES (@q, @s, 1, 'Q')");
            st.execute("INSERT INTO survey_tokens (id, survey_id, issued_to_user_id, token_hash, redeemed, redeemed_at)"
                    + " VALUES (@tok, @s, @u, unhex(sha2('p', 256)), true, '2026-03-04 10:11:12')");
            st.execute("INSERT INTO survey_responses (id, survey_id, token_id, created_at, updated_at)"
                    + " VALUES (@r, @s, @tok, '2026-03-04 10:11:12', '2026-03-04 10:11:12')");
            st.execute("INSERT INTO survey_answers (id, response_id, question_id, value, answer_order, created_at, updated_at)"
                    + " VALUES (uuid_to_bin(uuid()), @r, @q, 4, 0, '2026-03-04 10:11:12', '2026-03-04 10:11:12')");
        }

        flyway(null).migrate();

        try (Connection c = root(); Statement st = c.createStatement()) {
            assertThat(scalar(st, "select count(*) from information_schema.columns where table_schema = database()"
                    + " and table_name = 'survey_responses' and column_name = 'token_id'")).isZero();
            assertThat(scalar(st, "select count(*) from survey_responses")).isEqualTo(1);
            assertThat(scalar(st, "select count(*) from survey_responses where unix_timestamp(created_at) % 86400 = 0"
                    + " and unix_timestamp(updated_at) % 86400 = 0")).isEqualTo(1);
            assertThat(scalar(st, "select count(*) from survey_answers where value = 4"
                    + " and unix_timestamp(created_at) % 86400 = 0")).isEqualTo(1);
            assertThat(scalar(st, "select total_row_versions from information_schema.innodb_tables"
                    + " where name = concat(database(), '/survey_responses')")).isZero();
            // V3: account emails are compared byte-for-byte, not accent-insensitively.
            try (ResultSet rs = st.executeQuery("select collation_name from information_schema.columns"
                    + " where table_schema = database() and table_name = 'users' and column_name = 'email'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("utf8mb4_0900_bin");
            }
            assertThat(scalar(st, "select count(*) from users where email = 'm@ex\u00e4mple.test'")).isZero();
            assertThat(scalar(st, "select count(*) from users where email = 'mixed.case@example.test'")).isEqualTo(1);
            assertThat(scalar(st, "select count(*) from users where email = 'm@example.test '")).isZero(); // NO PAD
        }
    }

    private static Flyway flyway(String target) {
        var config = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword())
                .locations("classpath:db/migration");
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    private static Connection root() throws Exception {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword());
    }

    private static int scalar(Statement st, String sql) throws Exception {
        try (ResultSet rs = st.executeQuery(sql)) {
            assertThat(rs.next()).as(sql).isTrue();
            return rs.getInt(1);
        }
    }
}
