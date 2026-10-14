//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.forms;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Session;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Idempotent upgrades for KylGis-native database versions.
 *
 * Historical Chromis/uniCenta versions keep using their legacy script path.
 * KylGis releases should add narrowly-scoped, validated transitions here
 * instead of extending upgrade_master.sql.
 */
public final class DatabaseUpgradeService {

    public static final String VERSION_1_0 = "1.0";
    public static final String VERSION_1_1 = "1.1";

    private DatabaseUpgradeService() {
    }

    public static boolean supports(String fromVersion, String toVersion) {
        return VERSION_1_0.equals(fromVersion) && VERSION_1_1.equals(toVersion);
    }

    public static void upgrade(Session session, AppProperties properties,
            String fromVersion, String toVersion) throws BasicException {
        if (!supports(fromVersion, toVersion)) {
            throw new BasicException("Unsupported KylGis database upgrade: "
                    + fromVersion + " -> " + toVersion);
        }

        try {
            Connection connection = session.getConnection();
            DatabaseMetaData meta = connection.getMetaData();
            if (!isSupportedDatabase(meta)) {
                throw new SQLException("Unsupported database engine for KylGis 1.0 -> 1.1 migration: "
                        + meta.getDatabaseProductName());
            }
            if (!tableExists(meta, "vouchers")) {
                throw new SQLException("Required vouchers table does not exist.");
            }

            validateVoucherNumbers(connection);
            ensureVoucherLifecycle(connection, meta);
            ensureVoucherAudit(connection, connection.getMetaData());
            updateApplicationVersion(connection, properties, toVersion);
        } catch (SQLException ex) {
            throw new BasicException("KylGis database upgrade failed.", ex);
        }
    }

    private static void validateVoucherNumbers(Connection connection) throws SQLException {
        Set<String> normalized = new HashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT VOUCHER_NUMBER FROM vouchers")) {
            while (rows.next()) {
                String value = rows.getString(1);
                String key = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
                if (key.isEmpty()) {
                    throw new SQLException("Voucher migration blocked: blank voucher number found.");
                }
                if (!normalized.add(key)) {
                    throw new SQLException("Voucher migration blocked: duplicate voucher number found: " + key);
                }
            }
        }
    }

    private static void ensureVoucherLifecycle(Connection connection, DatabaseMetaData meta)
            throws SQLException {
        if (!columnExists(meta, "vouchers", "expires_at")) {
            execute(connection, "ALTER TABLE vouchers ADD COLUMN expires_at "
                    + (isPostgreSql(meta) ? "TIMESTAMP NULL" : "DATETIME NULL"));
            meta = connection.getMetaData();
        }

        if (columnNullable(meta, "vouchers", "voucher_number")) {
            if (isPostgreSql(meta)) {
                execute(connection, "ALTER TABLE vouchers ALTER COLUMN voucher_number SET NOT NULL");
            } else {
                execute(connection, "ALTER TABLE vouchers MODIFY voucher_number VARCHAR(100) NOT NULL");
            }
            meta = connection.getMetaData();
        }

        if (!indexExists(meta, "vouchers", "vouchers_number_uq")) {
            execute(connection, "CREATE UNIQUE INDEX vouchers_number_uq ON vouchers (voucher_number)");
        }
    }

    private static void ensureVoucherAudit(Connection connection, DatabaseMetaData meta)
            throws SQLException {
        if (!tableExists(meta, "voucher_reactivation_audit")) {
            if (isPostgreSql(meta)) {
                execute(connection, "CREATE TABLE voucher_reactivation_audit ("
                        + "id VARCHAR NOT NULL PRIMARY KEY, "
                        + "voucher_number VARCHAR NOT NULL, "
                        + "original_receipt VARCHAR NOT NULL, "
                        + "new_receipt VARCHAR NOT NULL, "
                        + "operator_id VARCHAR NOT NULL, "
                        + "authorized_by VARCHAR NOT NULL, "
                        + "authorized_at TIMESTAMP NOT NULL, "
                        + "reason VARCHAR NOT NULL)");
            } else {
                execute(connection, "CREATE TABLE voucher_reactivation_audit ("
                        + "id VARCHAR(100) NOT NULL PRIMARY KEY, "
                        + "voucher_number VARCHAR(100) NOT NULL, "
                        + "original_receipt VARCHAR(255) NOT NULL, "
                        + "new_receipt VARCHAR(255) NOT NULL, "
                        + "operator_id VARCHAR(255) NOT NULL, "
                        + "authorized_by VARCHAR(255) NOT NULL, "
                        + "authorized_at DATETIME NOT NULL, "
                        + "reason VARCHAR(50) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8");
            }
            meta = connection.getMetaData();
        }

        String[] required = {"id", "voucher_number", "original_receipt", "new_receipt",
            "operator_id", "authorized_by", "authorized_at", "reason"};
        for (String column : required) {
            if (!columnExists(meta, "voucher_reactivation_audit", column)) {
                throw new SQLException("Voucher audit table is incomplete: missing " + column);
            }
        }

        if (!indexExists(meta, "voucher_reactivation_audit", "voucher_reactivation_original_uq")) {
            execute(connection, "CREATE UNIQUE INDEX voucher_reactivation_original_uq "
                    + "ON voucher_reactivation_audit (original_receipt, voucher_number)");
        }
        if (!indexExists(connection.getMetaData(), "voucher_reactivation_audit", "voucher_reactivation_new_inx")) {
            execute(connection, "CREATE INDEX voucher_reactivation_new_inx "
                    + "ON voucher_reactivation_audit (new_receipt)");
        }
        if (!indexExists(connection.getMetaData(), "voucher_reactivation_audit", "voucher_reactivation_authorized_inx")) {
            execute(connection, "CREATE INDEX voucher_reactivation_authorized_inx "
                    + "ON voucher_reactivation_audit (authorized_by, authorized_at)");
        }
    }

    private static void updateApplicationVersion(Connection connection, AppProperties properties,
            String version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE applications SET NAME = ?, VERSION = ? WHERE ID = ?")) {
            statement.setString(1, ApplicationIdentity.getDatabaseName(properties));
            statement.setString(2, version);
            statement.setString(3, ApplicationIdentity.getDatabaseId(properties));
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Application identity row was not updated exactly once.");
            }
        }
    }

    private static boolean tableExists(DatabaseMetaData meta, String table) throws SQLException {
        return resolveTable(meta, table) != null;
    }

    private static boolean columnExists(DatabaseMetaData meta, String table, String column)
            throws SQLException {
        String actualTable = resolveTable(meta, table);
        if (actualTable == null) {
            return false;
        }
        try (ResultSet rows = meta.getColumns(currentCatalog(meta), currentSchema(meta), actualTable, null)) {
            while (rows.next()) {
                if (column.equalsIgnoreCase(rows.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean columnNullable(DatabaseMetaData meta, String table, String column)
            throws SQLException {
        String actualTable = resolveTable(meta, table);
        if (actualTable == null) {
            throw new SQLException("Required table not found in current schema: " + table);
        }
        try (ResultSet rows = meta.getColumns(currentCatalog(meta), currentSchema(meta), actualTable, null)) {
            while (rows.next()) {
                if (column.equalsIgnoreCase(rows.getString("COLUMN_NAME"))) {
                    return rows.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                }
            }
        }
        throw new SQLException("Required column not found: " + table + "." + column);
    }

    private static boolean indexExists(DatabaseMetaData meta, String table, String index)
            throws SQLException {
        String actualTable = resolveTable(meta, table);
        if (actualTable == null) {
            return false;
        }
        try (ResultSet rows = meta.getIndexInfo(currentCatalog(meta), currentSchema(meta),
                actualTable, false, false)) {
            while (rows.next()) {
                String name = rows.getString("INDEX_NAME");
                if (name != null && index.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String resolveTable(DatabaseMetaData meta, String table) throws SQLException {
        try (ResultSet rows = meta.getTables(currentCatalog(meta), currentSchema(meta),
                null, new String[]{"TABLE"})) {
            while (rows.next()) {
                String actual = rows.getString("TABLE_NAME");
                if (table.equalsIgnoreCase(actual)) {
                    return actual;
                }
            }
        }
        return null;
    }

    private static String currentCatalog(DatabaseMetaData meta) throws SQLException {
        String catalog = meta.getConnection().getCatalog();
        return catalog == null || catalog.isBlank() ? null : catalog;
    }

    private static String currentSchema(DatabaseMetaData meta) throws SQLException {
        if (!isPostgreSql(meta)) {
            return null;
        }
        String schema = meta.getConnection().getSchema();
        return schema == null || schema.isBlank() ? "public" : schema;
    }

    private static boolean isSupportedDatabase(DatabaseMetaData meta) throws SQLException {
        String product = meta.getDatabaseProductName().toLowerCase(Locale.ROOT);
        return product.contains("mysql") || product.contains("mariadb") || product.contains("postgres");
    }

    private static boolean isPostgreSql(DatabaseMetaData meta) throws SQLException {
        return meta.getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgres");
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
