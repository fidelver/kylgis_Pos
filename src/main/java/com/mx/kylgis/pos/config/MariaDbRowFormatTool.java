//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.config;

import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.forms.JdbcDriverLoader;
import com.mx.kylgis.pos.util.AltEncrypter;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Preflight and explicit migration helper for legacy InnoDB COMPACT row format.
 *
 * <p>Historical KylGis databases can contain wide tables created with
 * ROW_FORMAT=COMPACT. Current MariaDB releases may refuse to recreate those
 * tables from a logical backup with ERROR 1118. This tool is deliberately
 * opt-in: checking is the default, while applying changes requires both
 * --apply and --backup-confirmed.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * java ... MariaDbRowFormatTool /path/to/kylgispos.properties
 * java ... MariaDbRowFormatTool /path/to/kylgispos.properties --apply --backup-confirmed
 * </pre>
 */
public final class MariaDbRowFormatTool {

    private static final int EXIT_ACTION_REQUIRED = 3;

    private MariaDbRowFormatTool() {
    }

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        AppConfig config = new AppConfig(options.configFile);
        DatabaseSettings settings = DatabaseSettings.primary(config);

        JdbcDriverLoader.ensureLoaded(settings.getDriver(), settings.getDriverLibraryFile());
        String password = resolvePassword(settings.getUser(), settings.getPassword());
        try (Connection connection = DriverManager.getConnection(
                settings.getJdbcUrl(), settings.getUser(), password)) {
            run(connection, options);
        }
    }

    static String resolvePassword(String user, String password) {
        if (user != null && password != null && password.startsWith("crypt:")) {
            AltEncrypter cypher = new AltEncrypter("cypherkey" + user);
            return cypher.decrypt(password.substring(6));
        }
        return password;
    }

    static void run(Connection connection, Options options) throws Exception {
        String product = connection.getMetaData().getDatabaseProductName();
        String version = connection.getMetaData().getDatabaseProductVersion();
        String normalizedProduct = product == null ? "" : product.toLowerCase(Locale.ROOT);
        if (!normalizedProduct.contains("mariadb") && !normalizedProduct.contains("mysql")) {
            throw new IllegalStateException(
                    "ROW_FORMAT preflight only supports MariaDB/MySQL; detected " + product);
        }

        String database = currentDatabase(connection);
        if (database == null || database.trim().isEmpty()) {
            throw new IllegalStateException("No database/schema is selected by the JDBC configuration");
        }

        List<String> compactTables = compactTables(connection, database);
        System.out.println("KYLGIS_DB_ROW_FORMAT_PREFLIGHT");
        System.out.println("server=" + product + " " + version);
        System.out.println("database=" + database);
        System.out.println("compact_tables=" + compactTables.size());

        if (compactTables.isEmpty()) {
            System.out.println("status=OK");
            return;
        }

        for (String table : compactTables) {
            System.out.println("ALTER TABLE " + quoteIdentifier(table) + " ROW_FORMAT=DYNAMIC;");
        }

        if (!options.apply) {
            System.out.println("status=ACTION_REQUIRED");
            System.out.println("hint=Re-run with --apply --backup-confirmed only during an authorized maintenance window.");
            System.exit(EXIT_ACTION_REQUIRED);
            return;
        }

        if (!options.backupConfirmed) {
            throw new IllegalArgumentException(
                    "--apply requires --backup-confirmed; no changes were executed");
        }

        int changed = 0;
        try (Statement statement = connection.createStatement()) {
            for (String table : compactTables) {
                System.out.println("migrating=" + table);
                statement.execute("ALTER TABLE " + quoteIdentifier(table) + " ROW_FORMAT=DYNAMIC");
                changed++;
            }
        }

        List<String> remaining = compactTables(connection, database);
        if (!remaining.isEmpty()) {
            throw new IllegalStateException(
                    "Migration incomplete; COMPACT tables remain: " + remaining);
        }

        System.out.println("migrated_tables=" + changed);
        System.out.println("status=OK_DYNAMIC");
    }

    private static String currentDatabase(Connection connection) throws Exception {
        String catalog = connection.getCatalog();
        if (catalog != null && !catalog.trim().isEmpty()) {
            return catalog;
        }
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT DATABASE()")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static List<String> compactTables(Connection connection, String database) throws Exception {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT TABLE_NAME FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = ? AND ENGINE = 'InnoDB' "
                + "AND UPPER(ROW_FORMAT) = 'COMPACT' ORDER BY TABLE_NAME";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, database);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
        }
        return tables;
    }

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    static final class Options {
        final File configFile;
        final boolean apply;
        final boolean backupConfirmed;

        private Options(File configFile, boolean apply, boolean backupConfirmed) {
            this.configFile = configFile;
            this.apply = apply;
            this.backupConfirmed = backupConfirmed;
        }

        static Options parse(String[] args) {
            if (args == null || args.length < 1) {
                throw new IllegalArgumentException(
                        "Usage: MariaDbRowFormatTool <config.properties> [--apply --backup-confirmed]");
            }

            File config = new File(args[0]);
            if (!config.isFile()) {
                throw new IllegalArgumentException("Configuration file does not exist: " + config);
            }

            boolean apply = false;
            boolean backupConfirmed = false;
            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "--apply":
                        apply = true;
                        break;
                    case "--backup-confirmed":
                        backupConfirmed = true;
                        break;
                    case "--check":
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }
            return new Options(config, apply, backupConfirmed);
        }
    }
}
