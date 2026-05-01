package sk.tuke.gamestudio.infrastructure.db;

import org.flywaydb.core.Flyway;

import java.util.Locale;

public final class FlywayMaintenance {

    private FlywayMaintenance() {
    }

    public static void main(String[] args) {
        String action = args.length == 0 ? "validate" : args[0].trim().toLowerCase(Locale.ROOT);

        String dbName = requiredEnv("DB_NAME");
        String dbUser = requiredEnv("DB_USER");
        String dbPassword = requiredEnv("DB_PASSWORD");
        String instanceConnectionName = requiredEnv("INSTANCE_CONNECTION_NAME");

        String jdbcUrl = "jdbc:postgresql:///" + dbName
                + "?socketFactory=com.google.cloud.sql.postgres.SocketFactory"
                + "&cloudSqlInstance=" + instanceConnectionName;

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, dbUser, dbPassword)
                .locations("classpath:db/migration")
                .load();

        System.out.println("[flyway-maintenance] action=" + action + " db=" + dbName + " instance=" + instanceConnectionName);

        switch (action) {
            case "validate" -> flyway.validate();
            case "repair" -> flyway.repair();
            case "migrate" -> flyway.migrate();
            default -> throw new IllegalArgumentException("Unsupported action: " + action);
        }

        System.out.println("[flyway-maintenance] completed action=" + action);
    }

    private static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }
}
