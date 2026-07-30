package dev.buildtrace.project;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyLobMigration implements ApplicationRunner {

    private static final String MIGRATION_ID = "20260730-inline-text-lobs";
    private static final List<TextColumn> TEXT_COLUMNS = List.of(
        new TextColumn("projects", "current_html"),
        new TextColumn("messages", "content"),
        new TextColumn("generation_runs", "prompt"),
        new TextColumn("generation_runs", "error_message"),
        new TextColumn("project_versions", "html"),
        new TextColumn("project_versions", "files_json"),
        new TextColumn("project_versions", "prompt")
    );

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public LegacyLobMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                return;
            }
        }

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS app_schema_migrations (
                id VARCHAR(120) PRIMARY KEY,
                applied_at TIMESTAMPTZ NOT NULL
            )
            """);
        Integer applied = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM app_schema_migrations WHERE id = ?", Integer.class, MIGRATION_ID);
        if (applied != null && applied > 0) {
            return;
        }

        TEXT_COLUMNS.forEach(target -> jdbcTemplate.update("""
            UPDATE %s
               SET %s = convert_from(lo_get((%s)::oid), 'UTF8')
             WHERE %s ~ '^[0-9]+$'
               AND EXISTS (
                   SELECT 1 FROM pg_largeobject_metadata
                    WHERE oid = (%s)::oid
               )
            """.formatted(
                target.table(), target.column(), target.column(), target.column(), target.column())));
        jdbcTemplate.update(
            "INSERT INTO app_schema_migrations(id, applied_at) VALUES (?, now())", MIGRATION_ID);
    }

    private record TextColumn(String table, String column) {
    }
}
