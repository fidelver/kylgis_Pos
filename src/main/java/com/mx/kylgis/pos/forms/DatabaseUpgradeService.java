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
    public static final String VERSION_1_2 = "1.2";

    private DatabaseUpgradeService() {
    }

    public static boolean supports(String fromVersion, String toVersion) {
        return (VERSION_1_0.equals(fromVersion) && VERSION_1_1.equals(toVersion))
                || (VERSION_1_1.equals(fromVersion) && VERSION_1_2.equals(toVersion))
                || (VERSION_1_0.equals(fromVersion) && VERSION_1_2.equals(toVersion));
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
                throw new SQLException("Unsupported database engine for KylGis migration: "
                        + meta.getDatabaseProductName());
            }
            if (VERSION_1_0.equals(fromVersion)) {
                if (!tableExists(meta, "vouchers")) {
                    throw new SQLException("Required vouchers table does not exist.");
                }
                validateVoucherNumbers(connection);
                ensureVoucherLifecycle(connection, meta);
                ensureVoucherAudit(connection, connection.getMetaData());
                meta = connection.getMetaData();
            }

            if (VERSION_1_2.equals(toVersion)) {
                ensureProductSuppliers(connection, meta);
            }

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

    private static void ensureProductSuppliers(Connection connection, DatabaseMetaData meta)
            throws SQLException {
        if (!tableExists(meta, "products") || !tableExists(meta, "suppliers")) {
            throw new SQLException("Required products/suppliers tables do not exist.");
        }

        validateLegacyProductSuppliers(connection);

        if (!tableExists(meta, "product_suppliers")) {
            if (isPostgreSql(meta)) {
                execute(connection, "CREATE TABLE product_suppliers ("
                        + "product_id VARCHAR NOT NULL, "
                        + "supplier_id VARCHAR NOT NULL, "
                        + "is_preferred BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "active BOOLEAN NOT NULL DEFAULT TRUE, "
                        + "supplier_sku VARCHAR NULL, "
                        + "purchase_uom VARCHAR NULL, "
                        + "last_cost DOUBLE PRECISION NULL, "
                        + "priority INTEGER NULL, "
                        + "last_purchase TIMESTAMP NULL, "
                        + "PRIMARY KEY (product_id, supplier_id), "
                        + "CONSTRAINT product_suppliers_product_fk FOREIGN KEY (product_id) "
                        + "REFERENCES products (id) ON DELETE CASCADE, "
                        + "CONSTRAINT product_suppliers_supplier_fk FOREIGN KEY (supplier_id) "
                        + "REFERENCES suppliers (id) ON DELETE CASCADE)");
            } else {
                execute(connection, "CREATE TABLE product_suppliers ("
                        + "product_id VARCHAR(255) NOT NULL, "
                        + "supplier_id VARCHAR(255) NOT NULL, "
                        + "is_preferred BIT(1) NOT NULL DEFAULT b'0', "
                        + "active BIT(1) NOT NULL DEFAULT b'1', "
                        + "supplier_sku VARCHAR(255) NULL, "
                        + "purchase_uom VARCHAR(255) NULL, "
                        + "last_cost DOUBLE NULL, "
                        + "priority INT NULL, "
                        + "last_purchase DATETIME NULL, "
                        + "PRIMARY KEY (product_id, supplier_id), "
                        + "KEY product_suppliers_supplier_inx (supplier_id), "
                        + "CONSTRAINT product_suppliers_product_fk FOREIGN KEY (product_id) "
                        + "REFERENCES products (id) ON DELETE CASCADE, "
                        + "CONSTRAINT product_suppliers_supplier_fk FOREIGN KEY (supplier_id) "
                        + "REFERENCES suppliers (id) ON DELETE CASCADE) "
                        + "ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=Dynamic");
            }
            meta = connection.getMetaData();
        }

        String[] required = {"product_id", "supplier_id", "is_preferred", "active",
            "supplier_sku", "purchase_uom", "last_cost", "priority", "last_purchase"};
        for (String column : required) {
            if (!columnExists(meta, "product_suppliers", column)) {
                throw new SQLException("Product suppliers table is incomplete: missing " + column);
            }
        }

        validateCompositePrimaryKey(meta, "product_suppliers", "product_id", "supplier_id");
        validateCascadeForeignKey(meta, "product_suppliers", "product_id", "products", "id");
        validateCascadeForeignKey(meta, "product_suppliers", "supplier_id", "suppliers", "id");
        validatePreferredSupplierUniqueness(connection);

        if (!indexExists(meta, "product_suppliers", "product_suppliers_supplier_inx")) {
            execute(connection, "CREATE INDEX product_suppliers_supplier_inx "
                    + "ON product_suppliers (supplier_id)");
        }

        backfillLegacyProductSuppliers(connection);
        validatePreferredSupplierUniqueness(connection);
    }

    private static void validateLegacyProductSuppliers(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT p.id, p.supplier FROM products p "
                        + "LEFT JOIN suppliers s ON s.id = p.supplier "
                        + "WHERE p.supplier IS NOT NULL AND TRIM(p.supplier) <> '' "
                        + "AND s.id IS NULL")) {
            if (rows.next()) {
                throw new SQLException("Product supplier migration blocked: product "
                        + rows.getString(1) + " references unknown supplier " + rows.getString(2));
            }
        }
    }

    private static void backfillLegacyProductSuppliers(Connection connection) throws SQLException {
        String selectSql = "SELECT p.id, p.supplier FROM products p "
                + "JOIN suppliers s ON s.id = p.supplier "
                + "WHERE p.supplier IS NOT NULL AND TRIM(p.supplier) <> ''";
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(selectSql);
                PreparedStatement clearPreferred = connection.prepareStatement(
                        "UPDATE product_suppliers SET is_preferred = ? WHERE product_id = ?");
                PreparedStatement preferExisting = connection.prepareStatement(
                        "UPDATE product_suppliers SET is_preferred = ?, active = ? "
                        + "WHERE product_id = ? AND supplier_id = ?");
                PreparedStatement relationExists = connection.prepareStatement(
                        "SELECT 1 FROM product_suppliers WHERE product_id = ? AND supplier_id = ?");
                PreparedStatement insertMissing = connection.prepareStatement(
                        "INSERT INTO product_suppliers "
                        + "(product_id, supplier_id, is_preferred, active) VALUES (?, ?, ?, ?)") ) {
            while (rows.next()) {
                String productId = rows.getString(1);
                String supplierId = rows.getString(2);

                clearPreferred.setBoolean(1, false);
                clearPreferred.setString(2, productId);
                clearPreferred.executeUpdate();

                preferExisting.setBoolean(1, true);
                preferExisting.setBoolean(2, true);
                preferExisting.setString(3, productId);
                preferExisting.setString(4, supplierId);
                if (preferExisting.executeUpdate() == 0) {
                    relationExists.setString(1, productId);
                    relationExists.setString(2, supplierId);
                    try (ResultSet existing = relationExists.executeQuery()) {
                        if (!existing.next()) {
                            insertMissing.setString(1, productId);
                            insertMissing.setString(2, supplierId);
                            insertMissing.setBoolean(3, true);
                            insertMissing.setBoolean(4, true);
                            insertMissing.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    private static void validateCompositePrimaryKey(DatabaseMetaData meta, String table,
            String firstColumn, String secondColumn) throws SQLException {
        String actualTable = resolveTable(meta, table);
        if (actualTable == null) {
            throw new SQLException("Required table not found in current schema: " + table);
        }
        Set<String> columns = new HashSet<>();
        try (ResultSet rows = meta.getPrimaryKeys(currentCatalog(meta), currentSchema(meta), actualTable)) {
            while (rows.next()) {
                String column = rows.getString("COLUMN_NAME");
                if (column != null) {
                    columns.add(column.toLowerCase(Locale.ROOT));
                }
            }
        }
        if (columns.size() != 2
                || !columns.contains(firstColumn.toLowerCase(Locale.ROOT))
                || !columns.contains(secondColumn.toLowerCase(Locale.ROOT))) {
            throw new SQLException("Product suppliers table has an invalid primary key.");
        }
    }

    private static void validateCascadeForeignKey(DatabaseMetaData meta, String table,
            String foreignColumn, String referencedTable, String referencedColumn)
            throws SQLException {
        String actualTable = resolveTable(meta, table);
        if (actualTable == null) {
            throw new SQLException("Required table not found in current schema: " + table);
        }
        boolean relationshipFound = false;
        try (ResultSet rows = meta.getImportedKeys(currentCatalog(meta), currentSchema(meta), actualTable)) {
            while (rows.next()) {
                String fkColumn = rows.getString("FKCOLUMN_NAME");
                String pkTable = rows.getString("PKTABLE_NAME");
                String pkColumn = rows.getString("PKCOLUMN_NAME");
                if (foreignColumn.equalsIgnoreCase(fkColumn)
                        && referencedTable.equalsIgnoreCase(pkTable)
                        && referencedColumn.equalsIgnoreCase(pkColumn)) {
                    relationshipFound = true;
                    if (rows.getShort("DELETE_RULE") != DatabaseMetaData.importedKeyCascade) {
                        throw new SQLException("Foreign key " + table + "." + foreignColumn
                                + " must use ON DELETE CASCADE.");
                    }
                    return;
                }
            }
        }
        if (!relationshipFound) {
            throw new SQLException("Missing foreign key " + table + "." + foreignColumn
                    + " -> " + referencedTable + "." + referencedColumn);
        }
    }

    private static void validatePreferredSupplierUniqueness(Connection connection)
            throws SQLException {
        Set<String> preferredProducts = new HashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT product_id, is_preferred, active FROM product_suppliers")) {
            while (rows.next()) {
                if (rows.getBoolean(2) && rows.getBoolean(3)) {
                    String productId = rows.getString(1);
                    if (!preferredProducts.add(productId)) {
                        throw new SQLException("Product supplier migration blocked: product "
                                + productId + " has more than one preferred active supplier.");
                    }
                }
            }
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
