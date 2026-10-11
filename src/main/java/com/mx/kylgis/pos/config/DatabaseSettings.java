//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.config;

import com.mx.kylgis.pos.forms.AppProperties;
import java.io.File;
import java.util.Locale;
import java.util.Properties;

/**
 * Compatibility adapter between the provisioned database.* settings and the
 * historical db.* / db1.* JDBC properties.
 *
 * <p>New nodes default to MariaDB Connector/J from the application class path.
 * Existing legacy JDBC URLs, driver classes and external driver libraries are
 * preserved so an installed node is not silently migrated just by upgrading
 * KylGis POS.</p>
 */
public final class DatabaseSettings {

    public static final String ENGINE_MARIADB = "MariaDB";
    public static final String ENGINE_MYSQL = "MySQL";
    public static final String ENGINE_POSTGRESQL = "PostgreSQL";

    public static final String MARIADB_DRIVER = "org.mariadb.jdbc.Driver";
    public static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";

    private final String label;
    private final String engine;
    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final String driver;
    private final String driverLibrary;
    private final boolean simplified;

    private DatabaseSettings(String label, String engine, String jdbcUrl,
            String user, String password, String driver, String driverLibrary,
            boolean simplified) {
        this.label = label;
        this.engine = engine;
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.driver = driver;
        this.driverLibrary = trimToNull(driverLibrary);
        this.simplified = simplified;
    }

    public static DatabaseSettings primary(AppProperties properties) {
        return create(properties, "database", "db");
    }

    public static DatabaseSettings secondary(AppProperties properties) {
        return create(properties, "database.secondary", "db1");
    }

    private static DatabaseSettings create(AppProperties p,
            String modernPrefix, String legacyPrefix) {
        String server = trimToNull(p.getProperty(modernPrefix + ".server"));
        String databaseName = trimToNull(p.getProperty(modernPrefix + ".name"));
        boolean simplified = server != null || databaseName != null;

        if (!simplified) {
            String url = valueOrEmpty(p.getProperty(legacyPrefix + ".URL"))
                    + valueOrEmpty(p.getProperty(legacyPrefix + ".schema"))
                    + valueOrEmpty(p.getProperty(legacyPrefix + ".options"));
            String engine = normalizeEngine(firstNonBlank(
                    p.getProperty(legacyPrefix + ".engine"),
                    p.getProperty("db.engine"),
                    inferEngineFromUrl(url)));
            return new DatabaseSettings(
                    firstNonBlank(p.getProperty(legacyPrefix + ".name"), legacyPrefix),
                    engine,
                    url,
                    p.getProperty(legacyPrefix + ".user"),
                    p.getProperty(legacyPrefix + ".password"),
                    firstNonBlank(p.getProperty(legacyPrefix + ".driver"),
                            p.getProperty("db.driver"), defaultDriver(engine)),
                    firstNonBlank(p.getProperty(legacyPrefix + ".driverlib"),
                            p.getProperty("db.driverlib")),
                    false);
        }

        String engine = normalizeEngine(firstNonBlank(
                p.getProperty(modernPrefix + ".engine"),
                p.getProperty(legacyPrefix + ".engine"),
                p.getProperty("db.engine"), ENGINE_MARIADB));
        String host = firstNonBlank(server, "localhost");
        String port = firstNonBlank(p.getProperty(modernPrefix + ".port"),
                defaultPort(engine));
        String name = firstNonBlank(databaseName,
                p.getProperty(legacyPrefix + ".schema"), "");
        String options = firstNonBlank(p.getProperty(modernPrefix + ".options"),
                p.getProperty(legacyPrefix + ".options"), "");

        String jdbcUrl = jdbcPrefix(engine) + host + ":" + port + "/" + name + options;
        return new DatabaseSettings(
                firstNonBlank(p.getProperty(modernPrefix + ".label"),
                        p.getProperty(legacyPrefix + ".name"), name, legacyPrefix),
                engine,
                jdbcUrl,
                firstNonBlank(p.getProperty(modernPrefix + ".user"),
                        p.getProperty(legacyPrefix + ".user")),
                firstNonBlank(p.getProperty(modernPrefix + ".password"),
                        p.getProperty(legacyPrefix + ".password")),
                firstNonBlank(p.getProperty(modernPrefix + ".driver"),
                        p.getProperty(legacyPrefix + ".driver"),
                        p.getProperty("db.driver"), defaultDriver(engine)),
                firstNonBlank(p.getProperty(modernPrefix + ".driverlib"),
                        p.getProperty(legacyPrefix + ".driverlib"),
                        p.getProperty("db.driverlib")),
                true);
    }

    /**
     * Adds legacy aliases for code paths that have not migrated yet. Values are
     * copied in raw form so !secret references remain references until the
     * effective configuration is resolved.
     */
    public static void applyLegacyCompatibility(Properties properties) {
        if (properties == null) {
            return;
        }
        applyLegacyCompatibility(properties, "database", "db");
        applyLegacyCompatibility(properties, "database.secondary", "db1");
    }

    private static void applyLegacyCompatibility(Properties p,
            String modernPrefix, String legacyPrefix) {
        String server = trimToNull(p.getProperty(modernPrefix + ".server"));
        String databaseName = trimToNull(p.getProperty(modernPrefix + ".name"));
        if (server == null && databaseName == null) {
            return;
        }

        String engine = normalizeEngine(firstNonBlank(
                p.getProperty(modernPrefix + ".engine"),
                p.getProperty("db.engine"), ENGINE_MARIADB));
        String host = firstNonBlank(server, "localhost");
        String port = firstNonBlank(p.getProperty(modernPrefix + ".port"),
                defaultPort(engine));
        String schema = valueOrEmpty(firstNonBlank(databaseName,
                p.getProperty(legacyPrefix + ".schema")));
        String options = valueOrEmpty(firstNonBlank(
                p.getProperty(modernPrefix + ".options"),
                p.getProperty(legacyPrefix + ".options")));

        p.setProperty(legacyPrefix + ".URL", jdbcPrefix(engine) + host + ":" + port + "/");
        p.setProperty(legacyPrefix + ".schema", schema);
        p.setProperty(legacyPrefix + ".options", options);
        p.setProperty(legacyPrefix + ".engine", engine);
        copyIfPresent(p, modernPrefix + ".user", legacyPrefix + ".user");
        copyIfPresent(p, modernPrefix + ".password", legacyPrefix + ".password");
        copyIfPresent(p, modernPrefix + ".label", legacyPrefix + ".name");
        copyIfPresent(p, modernPrefix + ".dialect", legacyPrefix + ".dialect");

        String driver = firstNonBlank(p.getProperty(modernPrefix + ".driver"),
                defaultDriver(engine));
        p.setProperty(legacyPrefix + ".driver", driver);
        copyIfPresent(p, modernPrefix + ".driverlib", legacyPrefix + ".driverlib");

        if ("db".equals(legacyPrefix)) {
            p.setProperty("db.engine", engine);
            p.setProperty("db.driver", driver);
            String driverlib = p.getProperty(modernPrefix + ".driverlib");
            if (driverlib != null) {
                p.setProperty("db.driverlib", driverlib);
            }
        }
    }

    private static void copyIfPresent(Properties p, String source, String target) {
        String value = p.getProperty(source);
        if (value != null) {
            p.setProperty(target, value);
        }
    }

    public String getLabel() {
        return label;
    }

    public String getEngine() {
        return engine;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDriver() {
        return driver;
    }

    public String getDriverLibrary() {
        return driverLibrary;
    }

    public boolean isSimplified() {
        return simplified;
    }

    public File getDriverLibraryFile() {
        return driverLibrary == null ? null : new File(driverLibrary);
    }

    public static String defaultDriver(String engine) {
        String normalized = normalizeEngine(engine);
        if (ENGINE_POSTGRESQL.equals(normalized)) {
            return POSTGRESQL_DRIVER;
        }
        if (ENGINE_MYSQL.equals(normalized)) {
            return MYSQL_DRIVER;
        }
        return MARIADB_DRIVER;
    }

    public static String defaultPort(String engine) {
        return ENGINE_POSTGRESQL.equals(normalizeEngine(engine)) ? "5432" : "3306";
    }

    public static String jdbcPrefix(String engine) {
        String normalized = normalizeEngine(engine);
        if (ENGINE_POSTGRESQL.equals(normalized)) {
            return "jdbc:postgresql://";
        }
        if (ENGINE_MYSQL.equals(normalized)) {
            return "jdbc:mysql://";
        }
        return "jdbc:mariadb://";
    }

    public static String normalizeEngine(String engine) {
        String normalized = trimToNull(engine);
        if (normalized == null) {
            return ENGINE_MARIADB;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("postgres")) {
            return ENGINE_POSTGRESQL;
        }
        if (lower.contains("mysql")) {
            return ENGINE_MYSQL;
        }
        return ENGINE_MARIADB;
    }

    private static String inferEngineFromUrl(String url) {
        String normalized = trimToNull(url);
        if (normalized == null) {
            return ENGINE_MARIADB;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("jdbc:postgresql:")) {
            return ENGINE_POSTGRESQL;
        }
        if (normalized.startsWith("jdbc:mysql:")) {
            return ENGINE_MYSQL;
        }
        return ENGINE_MARIADB;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
