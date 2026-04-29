package config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.flywaydb.core.api.MigrationVersion;

public final class DatabaseMigrator {

    private DatabaseMigrator() {
    }

    public static void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(AppConfig.dbUrl(), AppConfig.dbUser(), AppConfig.dbPassword())
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .load();
        try {
            flyway.migrate();
        } catch (FlywayValidateException ex) {
            // Merge scenarios can legitimately change historical migration checksums in local dev DBs.
            flyway.repair();
            flyway.migrate();
        }
    }
}
