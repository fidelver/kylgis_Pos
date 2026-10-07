//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.config;

import com.mx.kylgis.pos.forms.AppProperties;
import java.io.File;

/**
 * Compatibility adapter between human-readable database.* settings used by
 * KylGis MASTER and the historical db.* / db1.* JDBC properties.
 */
public final class DatabaseSettings {

    private final String label;
    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final String driver;
    private final String driverLibrary;
    private final boolean simplified;

    private DatabaseSettings(String label, String jdbcUrl, String user,
            String password, String driver, String driverLibrary,
            boolean simplified) {
        this.label = label;
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.driver = driver;
        this.driverLibrary = driverLibrary;
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
            return new DatabaseSettings(
                    firstNonBlank(p.getProperty(legacyPrefix + ".name"), legacyPrefix),
                    url,
                    p.getProperty(legacyPrefix + ".user"),
                    p.getProperty(legacyPrefix + ".password"),
                    firstNonBlank(p.getProperty("db.driver"), "com.mysql.jdbc.Driver"),
                    p.getProperty("db.driverlib"),
                    false);
        }

        String host = firstNonBlank(server, "localhost");
        String port = firstNonBlank(p.getProperty(modernPrefix + ".port"), "3306");
        String name = firstNonBlank(databaseName, p.getProperty(legacyPrefix + ".schema"));
        String options = firstNonBlank(p.getProperty(modernPrefix + ".options"),
                p.getProperty(legacyPrefix + ".options"));
        if (name == null) {
            name = "";
        }
        if (options == null) {
            options = "";
        }

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + name + options;
        return new DatabaseSettings(
                firstNonBlank(p.getProperty(modernPrefix + ".label"),
                        p.getProperty(legacyPrefix + ".name"), name),
                jdbcUrl,
                firstNonBlank(p.getProperty(modernPrefix + ".user"),
                        p.getProperty(legacyPrefix + ".user")),
                firstNonBlank(p.getProperty(modernPrefix + ".password"),
                        p.getProperty(legacyPrefix + ".password")),
                firstNonBlank(p.getProperty(modernPrefix + ".driver"),
                        p.getProperty("db.driver"), "com.mysql.jdbc.Driver"),
                firstNonBlank(p.getProperty(modernPrefix + ".driverlib"),
                        p.getProperty("db.driverlib")),
                true);
    }

    public String getLabel() {
        return label;
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
