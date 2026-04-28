package config;

import org.flywaydb.core.Flyway;
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
        
        // Repair any checksum mismatches before migrating
        flyway.repair();
        
        flyway.migrate();
    }
}
