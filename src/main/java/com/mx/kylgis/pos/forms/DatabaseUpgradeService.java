//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.forms;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Session;
import java.nio.charset.StandardCharsets;
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
    public static final String VERSION_1_2_1 = "1.2.1";
    public static final String VERSION_1_3 = "1.3";

    private DatabaseUpgradeService() {
    }

    public static boolean supports(String fromVersion, String toVersion) {
        return (VERSION_1_0.equals(fromVersion) && VERSION_1_1.equals(toVersion))
                || (VERSION_1_1.equals(fromVersion) && VERSION_1_2.equals(toVersion))
                || (VERSION_1_0.equals(fromVersion) && VERSION_1_2.equals(toVersion))
                || (VERSION_1_2.equals(fromVersion) && VERSION_1_2_1.equals(toVersion))
                || (VERSION_1_1.equals(fromVersion) && VERSION_1_2_1.equals(toVersion))
                || (VERSION_1_0.equals(fromVersion) && VERSION_1_2_1.equals(toVersion))
                || (VERSION_1_2_1.equals(fromVersion) && VERSION_1_3.equals(toVersion))
                || (VERSION_1_2.equals(fromVersion) && VERSION_1_3.equals(toVersion))
                || (VERSION_1_1.equals(fromVersion) && VERSION_1_3.equals(toVersion))
                || (VERSION_1_0.equals(fromVersion) && VERSION_1_3.equals(toVersion));
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

            // Product/supplier backfill belongs only to upgrades that have not yet
            // reached schema 1.2. Re-running it from 1.2/1.2.1 would reset
            // legitimate N:M preferred-supplier choices back to products.supplier.
            if (!VERSION_1_2.equals(fromVersion) && !VERSION_1_2_1.equals(fromVersion)
                    && (VERSION_1_2.equals(toVersion) || VERSION_1_2_1.equals(toVersion)
                    || VERSION_1_3.equals(toVersion))) {
                ensureProductSuppliers(connection, meta);
                meta = connection.getMetaData();
            }

            if (VERSION_1_2_1.equals(toVersion) || VERSION_1_3.equals(toVersion)) {
                validateVoucherNumbers(connection);
                ensureVoucherLifecycle(connection, meta);
                ensureVoucherAudit(connection, connection.getMetaData());
                ensureVoucherDiscountCampaigns(connection, connection.getMetaData());
                meta = connection.getMetaData();
            }

            if (VERSION_1_3.equals(toVersion)) {
                ensureRecipesAndTransformations(connection, meta);
                ensureRecipeUiAccess(connection);
            }

            updateApplicationVersion(connection, properties, toVersion);
        } catch (SQLException ex) {
            throw new BasicException("KylGis database upgrade failed.", ex);
        }
    }

    /**
     * Fail-closed validation used immediately after a fresh KylGis schema is
     * created and stamped with the current application version. It performs no
     * DDL/DML and prevents startup when the create script drifted from the
     * KylGis-native schema contract.
     */
    public static void validateFreshSchema(Session session, AppProperties properties,
            String version) throws BasicException {
        if (!VERSION_1_3.equals(version)) {
            return;
        }
        try {
            Connection connection = session.getConnection();
            DatabaseMetaData meta = connection.getMetaData();
            if (!isSupportedDatabase(meta)) {
                throw new SQLException("Unsupported database engine for KylGis schema validation: "
                        + meta.getDatabaseProductName());
            }
            validateFreshApplicationIdentity(connection, properties, version);
            validateFreshVoucherSchema(meta);
            validateFreshProductSuppliersSchema(connection, meta);
            validateFreshRecipeSchema(connection, meta);
        } catch (SQLException ex) {
            throw new BasicException("Fresh KylGis database schema validation failed.", ex);
        }
    }

    private static void validateFreshApplicationIdentity(Connection connection,
            AppProperties properties, String version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT NAME, VERSION FROM applications WHERE ID = ?")) {
            statement.setString(1, ApplicationIdentity.getDatabaseId(properties));
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("Fresh database application identity row is missing.");
                }
                if (!ApplicationIdentity.getDatabaseName(properties).equals(row.getString(1))
                        || !version.equals(row.getString(2))) {
                    throw new SQLException("Fresh database application identity/version mismatch.");
                }
                if (row.next()) {
                    throw new SQLException("Fresh database application identity is not unique.");
                }
            }
        }
    }

    private static void validateFreshVoucherSchema(DatabaseMetaData meta) throws SQLException {
        validateColumns(meta, "vouchers", "id", "voucher_number", "customer", "amount",
                "status", "expires_at");
        if (!indexExists(meta, "vouchers", "vouchers_number_uq")) {
            throw new SQLException("Fresh vouchers schema is missing vouchers_number_uq.");
        }
        validateColumns(meta, "voucher_reactivation_audit", "id", "voucher_number",
                "original_receipt", "new_receipt", "operator_id", "authorized_by",
                "authorized_at", "reason");
        String[] indexes = {"voucher_reactivation_original_uq",
            "voucher_reactivation_new_inx", "voucher_reactivation_authorized_inx"};
        for (String index : indexes) {
            if (!indexExists(meta, "voucher_reactivation_audit", index)) {
                throw new SQLException("Fresh voucher audit schema is missing " + index + ".");
            }
        }
    }

    private static void validateFreshProductSuppliersSchema(Connection connection,
            DatabaseMetaData meta) throws SQLException {
        validateColumns(meta, "product_suppliers", "product_id", "supplier_id",
                "is_preferred", "active", "supplier_sku", "purchase_uom", "last_cost",
                "priority", "last_purchase");
        validateCompositePrimaryKey(meta, "product_suppliers", "product_id", "supplier_id");
        validateCascadeForeignKey(meta, "product_suppliers", "product_id", "products", "id");
        validateCascadeForeignKey(meta, "product_suppliers", "supplier_id", "suppliers", "id");
        if (!indexExists(meta, "product_suppliers", "product_suppliers_supplier_inx")) {
            throw new SQLException("Fresh product_suppliers schema is missing supplier index.");
        }
        validatePreferredSupplierUniqueness(connection);
    }

    private static void validateFreshRecipeSchema(Connection connection, DatabaseMetaData meta)
            throws SQLException {
        String[] baseTables = {"products", "uom", "locations", "stockcurrent", "stockdiary"};
        for (String table : baseTables) {
            if (!tableExists(meta, table)) {
                throw new SQLException("Fresh recipe schema is missing base table " + table + ".");
            }
        }

        validateColumns(meta, "recipe_versions", "id", "product_id", "version_no", "status",
                "yield_quantity", "yield_uom_id", "created_at", "created_by", "notes");
        validatePrimaryKey(meta, "recipe_versions", "id");
        requireIndex(meta, "recipe_versions", "recipe_versions_product_version_uq");
        requireIndex(meta, "recipe_versions", "recipe_versions_product_status_inx");
        validateForeignKey(meta, "recipe_versions", "product_id", "products", "id");
        validateForeignKey(meta, "recipe_versions", "yield_uom_id", "uom", "id");

        validateColumns(meta, "recipe_components", "recipe_version_id", "line_no",
                "component_product_id", "quantity", "uom_id", "waste_allowance");
        validatePrimaryKey(meta, "recipe_components", "recipe_version_id", "line_no");
        requireIndex(meta, "recipe_components", "recipe_components_product_inx");
        validateForeignKey(meta, "recipe_components", "recipe_version_id", "recipe_versions", "id");
        validateForeignKey(meta, "recipe_components", "component_product_id", "products", "id");
        validateForeignKey(meta, "recipe_components", "uom_id", "uom", "id");

        validateColumns(meta, "product_uom_conversions", "product_id", "from_uom_id",
                "to_uom_id", "factor", "active");
        validatePrimaryKey(meta, "product_uom_conversions", "product_id", "from_uom_id", "to_uom_id");
        validateForeignKey(meta, "product_uom_conversions", "product_id", "products", "id");
        validateForeignKey(meta, "product_uom_conversions", "from_uom_id", "uom", "id");
        validateForeignKey(meta, "product_uom_conversions", "to_uom_id", "uom", "id");

        validateColumns(meta, "inventory_transformations", "id", "idempotency_key",
                "request_hash", "type", "status", "location_id", "recipe_version_id",
                "reverses_id", "expected_output", "actual_output", "created_at", "created_by",
                "notes");
        validatePrimaryKey(meta, "inventory_transformations", "id");
        requireIndex(meta, "inventory_transformations", "inventory_transform_idempotency_uq");
        requireIndex(meta, "inventory_transformations", "inventory_transform_reverses_uq");
        requireIndex(meta, "inventory_transformations", "inventory_transform_location_date_inx");
        requireIndex(meta, "inventory_transformations", "inventory_transform_recipe_inx");
        validateForeignKey(meta, "inventory_transformations", "location_id", "locations", "id");
        validateForeignKey(meta, "inventory_transformations", "recipe_version_id", "recipe_versions", "id");
        validateForeignKey(meta, "inventory_transformations", "reverses_id", "inventory_transformations", "id");

        validateColumns(meta, "inventory_transformation_lines", "transformation_id", "line_no",
                "direction", "product_id", "attributesetinstance_id", "quantity", "uom_id",
                "base_quantity", "unit_cost", "total_cost", "stockdiary_id");
        validatePrimaryKey(meta, "inventory_transformation_lines", "transformation_id", "line_no");
        requireIndex(meta, "inventory_transformation_lines", "inventory_transform_line_diary_uq");
        requireIndex(meta, "inventory_transformation_lines", "inventory_transform_line_product_inx");
        validateForeignKey(meta, "inventory_transformation_lines", "transformation_id",
                "inventory_transformations", "id");
        validateForeignKey(meta, "inventory_transformation_lines", "product_id", "products", "id");
        validateForeignKey(meta, "inventory_transformation_lines", "uom_id", "uom", "id");
        validateForeignKey(meta, "inventory_transformation_lines", "stockdiary_id", "stockdiary", "id");
        validateRecipeUniqueIndexes(meta);
        validateRecipeIntegrity(connection);
    }

    private static void requireIndex(DatabaseMetaData meta, String table, String index)
            throws SQLException {
        if (!indexExists(meta, table, index)) {
            throw new SQLException("Required index missing: " + table + "." + index);
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

    private static void ensureVoucherDiscountCampaigns(Connection connection, DatabaseMetaData meta)
            throws SQLException {
        if (!tableExists(meta, "vouchers") || !tableExists(meta, "customers")
                || !tableExists(meta, "receipts")) {
            throw new SQLException("Required voucher/customer/receipt tables do not exist.");
        }

        if (!tableExists(meta, "voucher_campaigns")) {
            if (isPostgreSql(meta)) {
                execute(connection, "CREATE TABLE voucher_campaigns ("
                        + "id VARCHAR NOT NULL PRIMARY KEY, "
                        + "name VARCHAR NOT NULL, "
                        + "code_prefix VARCHAR NOT NULL, "
                        + "assignment_mode VARCHAR(16) NOT NULL, "
                        + "amount DOUBLE PRECISION NOT NULL, "
                        + "quantity INTEGER NOT NULL, "
                        + "status VARCHAR(16) NOT NULL, "
                        + "expires_at TIMESTAMP NULL, "
                        + "created_at TIMESTAMP NOT NULL, "
                        + "created_by VARCHAR NOT NULL)");
            } else {
                execute(connection, "CREATE TABLE voucher_campaigns ("
                        + "id VARCHAR(100) NOT NULL PRIMARY KEY, "
                        + "name VARCHAR(255) NOT NULL, "
                        + "code_prefix VARCHAR(100) NOT NULL, "
                        + "assignment_mode VARCHAR(16) NOT NULL, "
                        + "amount DOUBLE NOT NULL, "
                        + "quantity INT NOT NULL, "
                        + "status VARCHAR(16) NOT NULL, "
                        + "expires_at DATETIME NULL, "
                        + "created_at DATETIME NOT NULL, "
                        + "created_by VARCHAR(255) NOT NULL) "
                        + "ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=Dynamic");
            }
            meta = connection.getMetaData();
        }
        requireColumns(meta, "voucher_campaigns", "id", "name", "code_prefix",
                "assignment_mode", "amount", "quantity", "status", "expires_at",
                "created_at", "created_by");
        if (!indexExists(meta, "voucher_campaigns", "voucher_campaign_created_inx")) {
            execute(connection, "CREATE INDEX voucher_campaign_created_inx "
                    + "ON voucher_campaigns (created_at)");
        }

        meta = connection.getMetaData();
        if (!columnExists(meta, "vouchers", "campaign_id")) {
            execute(connection, "ALTER TABLE vouchers ADD COLUMN campaign_id "
                    + (isPostgreSql(meta) ? "VARCHAR NULL" : "VARCHAR(100) NULL"));
            meta = connection.getMetaData();
        }
        ensureForeignKey(connection, meta, "vouchers", "campaign_id",
                "voucher_campaigns", "id", "vouchers_campaign_fk", "SET NULL");
        if (!indexExists(connection.getMetaData(), "vouchers", "vouchers_campaign_inx")) {
            execute(connection, "CREATE INDEX vouchers_campaign_inx ON vouchers (campaign_id)");
        }

        meta = connection.getMetaData();
        if (!tableExists(meta, "voucher_redemptions")) {
            if (isPostgreSql(meta)) {
                execute(connection, "CREATE TABLE voucher_redemptions ("
                        + "id VARCHAR NOT NULL PRIMARY KEY, "
                        + "voucher_id VARCHAR NOT NULL, "
                        + "voucher_number VARCHAR NOT NULL, "
                        + "receipt_id VARCHAR NOT NULL, "
                        + "customer_id VARCHAR NULL, "
                        + "voucher_customer_before VARCHAR NULL, "
                        + "discount_amount DOUBLE PRECISION NOT NULL, "
                        + "redeemed_at TIMESTAMP NOT NULL, "
                        + "operator_id VARCHAR NOT NULL, "
                        + "status VARCHAR(16) NOT NULL, "
                        + "reversed_at TIMESTAMP NULL, "
                        + "replacement_receipt_id VARCHAR NULL, "
                        + "CONSTRAINT voucher_redemption_voucher_fk FOREIGN KEY (voucher_id) REFERENCES vouchers(id), "
                        + "CONSTRAINT voucher_redemption_customer_fk FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL)");
            } else {
                execute(connection, "CREATE TABLE voucher_redemptions ("
                        + "id VARCHAR(100) NOT NULL PRIMARY KEY, "
                        + "voucher_id VARCHAR(100) NOT NULL, "
                        + "voucher_number VARCHAR(100) NOT NULL, "
                        + "receipt_id VARCHAR(255) NOT NULL, "
                        + "customer_id VARCHAR(255) NULL, "
                        + "voucher_customer_before VARCHAR(255) NULL, "
                        + "discount_amount DOUBLE NOT NULL, "
                        + "redeemed_at DATETIME NOT NULL, "
                        + "operator_id VARCHAR(255) NOT NULL, "
                        + "status VARCHAR(16) NOT NULL, "
                        + "reversed_at DATETIME NULL, "
                        + "replacement_receipt_id VARCHAR(255) NULL, "
                        + "CONSTRAINT voucher_redemption_voucher_fk FOREIGN KEY (voucher_id) REFERENCES vouchers(id), "
                        + "CONSTRAINT voucher_redemption_customer_fk FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL) "
                        + "ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=Dynamic");
            }
            meta = connection.getMetaData();
        }
        requireColumns(meta, "voucher_redemptions", "id", "voucher_id", "voucher_number",
                "receipt_id", "customer_id", "voucher_customer_before", "discount_amount",
                "redeemed_at", "operator_id", "status", "reversed_at", "replacement_receipt_id");
        ensureForeignKey(connection, meta, "voucher_redemptions", "voucher_id",
                "vouchers", "id", "voucher_redemption_voucher_fk", "RESTRICT");
        ensureForeignKey(connection, connection.getMetaData(), "voucher_redemptions", "customer_id",
                "customers", "id", "voucher_redemption_customer_fk", "SET NULL");
        if (!indexExists(connection.getMetaData(), "voucher_redemptions", "voucher_redemption_voucher_inx")) {
            execute(connection, "CREATE INDEX voucher_redemption_voucher_inx "
                    + "ON voucher_redemptions (voucher_id, status)");
        }
        if (!indexExists(connection.getMetaData(), "voucher_redemptions", "voucher_redemption_receipt_inx")) {
            execute(connection, "CREATE INDEX voucher_redemption_receipt_inx "
                    + "ON voucher_redemptions (receipt_id)");
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
        ensureCascadeForeignKey(connection, "product_suppliers", "product_id",
                "products", "id", "product_suppliers_product_fk");
        ensureCascadeForeignKey(connection, "product_suppliers", "supplier_id",
                "suppliers", "id", "product_suppliers_supplier_fk");
        meta = connection.getMetaData();
        validatePreferredSupplierUniqueness(connection);

        if (!indexExists(meta, "product_suppliers", "product_suppliers_supplier_inx")) {
            execute(connection, "CREATE INDEX product_suppliers_supplier_inx "
                    + "ON product_suppliers (supplier_id)");
        }

        backfillLegacyProductSuppliers(connection);
        validatePreferredSupplierUniqueness(connection);
    }

    private static void ensureRecipesAndTransformations(Connection connection, DatabaseMetaData meta)
            throws SQLException {
        String[] requiredBaseTables = {"products", "uom", "locations", "stockdiary"};
        for (String table : requiredBaseTables) {
            if (!tableExists(meta, table)) {
                throw new SQLException("Required recipe/transform base table does not exist: " + table);
            }
        }

        if (!tableExists(meta, "recipe_versions")) {
            if (isPostgreSql(meta)) {
                execute(connection, "CREATE TABLE recipe_versions ("
                        + "id VARCHAR NOT NULL PRIMARY KEY, "
                        + "product_id VARCHAR NOT NULL, "
                        + "version_no INTEGER NOT NULL, "
                        + "status VARCHAR(16) NOT NULL, "
                        + "yield_quantity NUMERIC(19,6) NOT NULL, "
                        + "yield_uom_id VARCHAR NOT NULL, "
                        + "created_at TIMESTAMP NOT NULL, "
                        + "created_by VARCHAR NOT NULL, "
                        + "notes VARCHAR(1000) NULL, "
                        + "CONSTRAINT recipe_versions_product_version_uq UNIQUE (product_id, version_no), "
                        + "CONSTRAINT recipe_versions_product_fk FOREIGN KEY (product_id) REFERENCES products(id), "
                        + "CONSTRAINT recipe_versions_uom_fk FOREIGN KEY (yield_uom_id) REFERENCES uom(id))");
            } else {
                execute(connection, "CREATE TABLE recipe_versions ("
                        + "id VARCHAR(255) NOT NULL PRIMARY KEY, "
                        + "product_id VARCHAR(255) NOT NULL, "
                        + "version_no INT NOT NULL, "
                        + "status VARCHAR(16) NOT NULL, "
                        + "yield_quantity DECIMAL(19,6) NOT NULL, "
                        + "yield_uom_id VARCHAR(255) NOT NULL, "
                        + "created_at DATETIME NOT NULL, "
                        + "created_by VARCHAR(255) NOT NULL, "
                        + "notes VARCHAR(1000) NULL, "
                        + "UNIQUE KEY recipe_versions_product_version_uq (product_id, version_no), "
                        + "KEY recipe_versions_product_status_inx (product_id, status), "
                        + "CONSTRAINT recipe_versions_product_fk FOREIGN KEY (product_id) REFERENCES products(id), "
                        + "CONSTRAINT recipe_versions_uom_fk FOREIGN KEY (yield_uom_id) REFERENCES uom(id)) "
                        + "ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=Dynamic");
            }
            meta = connection.getMetaData();
        }
        validateColumns(meta, "recipe_versions", "id", "product_id", "version_no", "status",
                "yield_quantity", "yield_uom_id", "created_at", "created_by", "notes");
        validatePrimaryKey(meta, "recipe_versions", "id");
        if (!indexExists(meta, "recipe_versions", "recipe_versions_product_version_uq")) {
            execute(connection, "CREATE UNIQUE INDEX recipe_versions_product_version_uq "
                    + "ON recipe_versions (product_id, version_no)");
        }
        if (!indexExists(connection.getMetaData(), "recipe_versions", "recipe_versions_product_status_inx")) {
            execute(connection, "CREATE INDEX recipe_versions_product_status_inx "
                    + "ON recipe_versions (product_id, status)");
        }
        meta = connection.getMetaData();
        validateForeignKey(meta, "recipe_versions", "product_id", "products", "id");
        validateForeignKey(meta, "recipe_versions", "yield_uom_id", "uom", "id");

        if (!tableExists(meta, "recipe_components")) {
            if (isPostgreSql(meta)) {
                execute(connection, "CREATE TABLE recipe_components ("
                        + "recipe_version_id VARCHAR NOT NULL, "
                        + "line_no INTEGER NOT NULL, "
                        + "component_product_id VARCHAR NOT NULL, "
                        + "quantity NUMERIC(19,6) NOT NULL, "
                        + "uom_id VARCHAR NOT NULL, "
                        + "waste_allowance NUMERIC(9,6) NULL, "
                        + "PRIMARY KEY (recipe_version_id, line_no), "
                        + "CONSTRAINT recipe_components_recipe_fk FOREIGN KEY (recipe_version_id) "
                        + "REFERENCES recipe_versions(id) ON DELETE CASCADE, "
                        + "CONSTRAINT recipe_components_product_fk FOREIGN KEY (component_product_id) REFERENCES products(id), "
                        + "CONSTRAINT recipe_components_uom_fk FOREIGN KEY (uom_id) REFERENCES uom(id))");
            } else {
                execute(connection, "CREATE TABLE recipe_components ("
                        + "recipe_version_id VARCHAR(255) NOT NULL, "
                        + "line_no INT NOT NULL, "
                        + "component_product_id VARCHAR(255) NOT NULL, "
                        + "quantity DECIMAL(19,6) NOT NULL, "
                        + "uom_id VARCHAR(255) NOT NULL, "
                        + "waste_allowance DECIMAL(9,6) NULL, "
                        + "PRIMARY KEY (recipe_version_id, line_no), "
                        + "KEY recipe_components_product_inx (component_product_id), "
                        + "CONSTRAINT recipe_components_recipe_fk FOREIGN KEY (recipe_version_id) "
                        + "REFERENCES recipe_versions(id) ON DELETE CASCADE, "
                        + "CONSTRAINT recipe_components_product_fk FOREIGN KEY (component_product_id) REFERENCES products(id), "
                        + "CONSTRAINT recipe_components_uom_fk FOREIGN KEY (uom_id) REFERENCES uom(id)) "
                        + "ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=Dynamic");
            }
            meta = connection.getMetaData();
        }
        validateColumns(meta, "recipe_components", "recipe_version_id", "line_no",
                "component_product_id", "quantity", "uom_id", "waste_allowance");
        validatePrimaryKey(meta, "recipe_components", "recipe_version_id", "line_no");
        if (!indexExists(meta, "recipe_components", "recipe_components_product_inx")) {
            execute(connection, "CREATE INDEX recipe_components_product_inx "
                    + "ON recipe_components (component_product_id)");
        }
        meta = connection.getMetaData();
        validateForeignKey(meta, "recipe_components", "recipe_version_id", "recipe_versions", "id");
        validateForeignKey(meta, "recipe_components", "component_product_id", "products", "id");
        validateForeignKey(meta, "recipe_components", "uom_id", "uom", "id");

        if (!tableExists(meta, "product_uom_conversions")) {
            if (isPostgreSql(meta)) {
                execute(connection, "CREATE TABLE product_uom_conversions ("
                        + "product_id VARCHAR NOT NULL, "
                        + "from_uom_id VARCHAR NOT NULL, "
                        + "to_uom_id VARCHAR NOT NULL, "
                        + "factor NUMERIC(19,9) NOT NULL, "
                        + "active BOOLEAN NOT NULL DEFAULT TRUE, "
                        + "PRIMARY KEY (product_id, from_uom_id, to_uom_id), "
                        + "CONSTRAINT product_uom_product_fk FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE, "
                        + "CONSTRAINT product_uom_from_fk FOREIGN KEY (from_uom_id) REFERENCES uom(id), "
                        + "CONSTRAINT product_uom_to_fk FOREIGN KEY (to_uom_id) REFERENCES uom(id))");
            } else {
                execute(connection, "CREATE TABLE product_uom_conversions ("
                        + "product_id VARCHAR(255) NOT NULL, "
                        + "from_uom_id VARCHAR(255) NOT NULL, "
                        + "to_uom_id VARCHAR(255) NOT NULL, "
                        + "factor DECIMAL(19,9) NOT NULL, "
                        + "active BIT(1) NOT NULL DEFAULT b'1', "
                        + "PRIMARY KEY (product_id, from_uom_id, to_uom_id), "
                        + "KEY product_uom_from_inx (from_uom_id), "
                        + "KEY product_uom_to_inx (to_uom_id), "
                        + "CONSTRAINT product_uom_product_fk FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE, "
                        + "CONSTRAINT product_uom_from_fk FOREIGN KEY (from_uom_id) REFERENCES uom(id), "
                        + "CONSTRAINT product_uom_to_fk FOREIGN KEY (to_uom_id) REFERENCES uom(id)) "
                        + "ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=Dynamic");
            }
            meta = connection.getMetaData();
        }
        validateColumns(meta, "product_uom_conversions", "product_id", "from_uom_id",
                "to_uom_id", "factor", "active");
        validatePrimaryKey(meta, "product_uom_conversions", "product_id", "from_uom_id", "to_uom_id");
        validateForeignKey(meta, "product_uom_conversions", "product_id", "products", "id");
        validateForeignKey(meta, "product_uom_conversions", "from_uom_id", "uom", "id");
        validateForeignKey(meta, "product_uom_conversions", "to_uom_id", "uom", "id");

        if (!tableExists(meta, "inventory_transformations")) {
            if (isPostgreSql(meta)) {
                execute(connection, "CREATE TABLE inventory_transformations ("
                        + "id VARCHAR NOT NULL PRIMARY KEY, "
                        + "idempotency_key VARCHAR NOT NULL, "
                        + "request_hash VARCHAR(64) NOT NULL, "
                        + "type VARCHAR(24) NOT NULL, "
                        + "status VARCHAR(16) NOT NULL, "
                        + "location_id VARCHAR NOT NULL, "
                        + "recipe_version_id VARCHAR NULL, "
                        + "reverses_id VARCHAR NULL, "
                        + "expected_output NUMERIC(19,6) NULL, "
                        + "actual_output NUMERIC(19,6) NULL, "
                        + "created_at TIMESTAMP NOT NULL, "
                        + "created_by VARCHAR NOT NULL, "
                        + "notes VARCHAR(1000) NULL, "
                        + "CONSTRAINT inventory_transform_idempotency_uq UNIQUE (idempotency_key), "
                        + "CONSTRAINT inventory_transform_reverses_uq UNIQUE (reverses_id), "
                        + "CONSTRAINT inventory_transform_location_fk FOREIGN KEY (location_id) REFERENCES locations(id), "
                        + "CONSTRAINT inventory_transform_recipe_fk FOREIGN KEY (recipe_version_id) REFERENCES recipe_versions(id), "
                        + "CONSTRAINT inventory_transform_reverse_fk FOREIGN KEY (reverses_id) REFERENCES inventory_transformations(id))");
            } else {
                execute(connection, "CREATE TABLE inventory_transformations ("
                        + "id VARCHAR(255) NOT NULL PRIMARY KEY, "
                        + "idempotency_key VARCHAR(255) NOT NULL, "
                        + "request_hash VARCHAR(64) NOT NULL, "
                        + "type VARCHAR(24) NOT NULL, "
                        + "status VARCHAR(16) NOT NULL, "
                        + "location_id VARCHAR(255) NOT NULL, "
                        + "recipe_version_id VARCHAR(255) NULL, "
                        + "reverses_id VARCHAR(255) NULL, "
                        + "expected_output DECIMAL(19,6) NULL, "
                        + "actual_output DECIMAL(19,6) NULL, "
                        + "created_at DATETIME NOT NULL, "
                        + "created_by VARCHAR(255) NOT NULL, "
                        + "notes VARCHAR(1000) NULL, "
                        + "UNIQUE KEY inventory_transform_idempotency_uq (idempotency_key), "
                        + "UNIQUE KEY inventory_transform_reverses_uq (reverses_id), "
                        + "KEY inventory_transform_location_date_inx (location_id, created_at), "
                        + "KEY inventory_transform_recipe_inx (recipe_version_id), "
                        + "CONSTRAINT inventory_transform_location_fk FOREIGN KEY (location_id) REFERENCES locations(id), "
                        + "CONSTRAINT inventory_transform_recipe_fk FOREIGN KEY (recipe_version_id) REFERENCES recipe_versions(id), "
                        + "CONSTRAINT inventory_transform_reverse_fk FOREIGN KEY (reverses_id) REFERENCES inventory_transformations(id)) "
                        + "ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=Dynamic");
            }
            meta = connection.getMetaData();
        }
        validateColumns(meta, "inventory_transformations", "id", "idempotency_key", "request_hash",
                "type", "status", "location_id", "recipe_version_id", "reverses_id",
                "expected_output", "actual_output", "created_at", "created_by", "notes");
        validatePrimaryKey(meta, "inventory_transformations", "id");
        if (!indexExists(meta, "inventory_transformations", "inventory_transform_idempotency_uq")) {
            execute(connection, "CREATE UNIQUE INDEX inventory_transform_idempotency_uq "
                    + "ON inventory_transformations (idempotency_key)");
        }
        if (!indexExists(connection.getMetaData(), "inventory_transformations", "inventory_transform_reverses_uq")) {
            execute(connection, "CREATE UNIQUE INDEX inventory_transform_reverses_uq "
                    + "ON inventory_transformations (reverses_id)");
        }
        if (!indexExists(connection.getMetaData(), "inventory_transformations", "inventory_transform_location_date_inx")) {
            execute(connection, "CREATE INDEX inventory_transform_location_date_inx "
                    + "ON inventory_transformations (location_id, created_at)");
        }
        if (!indexExists(connection.getMetaData(), "inventory_transformations", "inventory_transform_recipe_inx")) {
            execute(connection, "CREATE INDEX inventory_transform_recipe_inx "
                    + "ON inventory_transformations (recipe_version_id)");
        }
        meta = connection.getMetaData();
        validateForeignKey(meta, "inventory_transformations", "location_id", "locations", "id");
        validateForeignKey(meta, "inventory_transformations", "recipe_version_id", "recipe_versions", "id");
        validateForeignKey(meta, "inventory_transformations", "reverses_id", "inventory_transformations", "id");

        if (!tableExists(meta, "inventory_transformation_lines")) {
            if (isPostgreSql(meta)) {
                execute(connection, "CREATE TABLE inventory_transformation_lines ("
                        + "transformation_id VARCHAR NOT NULL, "
                        + "line_no INTEGER NOT NULL, "
                        + "direction VARCHAR(8) NOT NULL, "
                        + "product_id VARCHAR NOT NULL, "
                        + "attributesetinstance_id VARCHAR NULL, "
                        + "quantity NUMERIC(19,6) NOT NULL, "
                        + "uom_id VARCHAR NOT NULL, "
                        + "base_quantity NUMERIC(19,6) NOT NULL, "
                        + "unit_cost NUMERIC(19,6) NULL, "
                        + "total_cost NUMERIC(19,6) NULL, "
                        + "stockdiary_id VARCHAR NOT NULL, "
                        + "PRIMARY KEY (transformation_id, line_no), "
                        + "CONSTRAINT inventory_transform_line_diary_uq UNIQUE (stockdiary_id), "
                        + "CONSTRAINT inventory_transform_line_header_fk FOREIGN KEY (transformation_id) "
                        + "REFERENCES inventory_transformations(id), "
                        + "CONSTRAINT inventory_transform_line_product_fk FOREIGN KEY (product_id) REFERENCES products(id), "
                        + "CONSTRAINT inventory_transform_line_uom_fk FOREIGN KEY (uom_id) REFERENCES uom(id), "
                        + "CONSTRAINT inventory_transform_line_diary_fk FOREIGN KEY (stockdiary_id) REFERENCES stockdiary(id))");
            } else {
                execute(connection, "CREATE TABLE inventory_transformation_lines ("
                        + "transformation_id VARCHAR(255) NOT NULL, "
                        + "line_no INT NOT NULL, "
                        + "direction VARCHAR(8) NOT NULL, "
                        + "product_id VARCHAR(255) NOT NULL, "
                        + "attributesetinstance_id VARCHAR(255) NULL, "
                        + "quantity DECIMAL(19,6) NOT NULL, "
                        + "uom_id VARCHAR(255) NOT NULL, "
                        + "base_quantity DECIMAL(19,6) NOT NULL, "
                        + "unit_cost DECIMAL(19,6) NULL, "
                        + "total_cost DECIMAL(19,6) NULL, "
                        + "stockdiary_id VARCHAR(255) NOT NULL, "
                        + "PRIMARY KEY (transformation_id, line_no), "
                        + "UNIQUE KEY inventory_transform_line_diary_uq (stockdiary_id), "
                        + "KEY inventory_transform_line_product_inx (product_id), "
                        + "CONSTRAINT inventory_transform_line_header_fk FOREIGN KEY (transformation_id) "
                        + "REFERENCES inventory_transformations(id), "
                        + "CONSTRAINT inventory_transform_line_product_fk FOREIGN KEY (product_id) REFERENCES products(id), "
                        + "CONSTRAINT inventory_transform_line_uom_fk FOREIGN KEY (uom_id) REFERENCES uom(id), "
                        + "CONSTRAINT inventory_transform_line_diary_fk FOREIGN KEY (stockdiary_id) REFERENCES stockdiary(id)) "
                        + "ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=Dynamic");
            }
            meta = connection.getMetaData();
        }
        validateColumns(meta, "inventory_transformation_lines", "transformation_id", "line_no",
                "direction", "product_id", "attributesetinstance_id", "quantity", "uom_id",
                "base_quantity", "unit_cost", "total_cost", "stockdiary_id");
        validatePrimaryKey(meta, "inventory_transformation_lines", "transformation_id", "line_no");
        if (!indexExists(meta, "inventory_transformation_lines", "inventory_transform_line_diary_uq")) {
            execute(connection, "CREATE UNIQUE INDEX inventory_transform_line_diary_uq "
                    + "ON inventory_transformation_lines (stockdiary_id)");
        }
        if (!indexExists(connection.getMetaData(), "inventory_transformation_lines", "inventory_transform_line_product_inx")) {
            execute(connection, "CREATE INDEX inventory_transform_line_product_inx "
                    + "ON inventory_transformation_lines (product_id)");
        }
        meta = connection.getMetaData();
        validateForeignKey(meta, "inventory_transformation_lines", "transformation_id", "inventory_transformations", "id");
        validateForeignKey(meta, "inventory_transformation_lines", "product_id", "products", "id");
        validateForeignKey(meta, "inventory_transformation_lines", "uom_id", "uom", "id");
        validateForeignKey(meta, "inventory_transformation_lines", "stockdiary_id", "stockdiary", "id");

        validateRecipeUniqueIndexes(meta);
        validateRecipeIntegrity(connection);
    }

    private static void validateRecipeUniqueIndexes(DatabaseMetaData meta) throws SQLException {
        validateIndexDefinition(meta, "recipe_versions", "recipe_versions_product_version_uq",
                true, "product_id", "version_no");
        validateIndexDefinition(meta, "inventory_transformations", "inventory_transform_idempotency_uq",
                true, "idempotency_key");
        validateIndexDefinition(meta, "inventory_transformations", "inventory_transform_reverses_uq",
                true, "reverses_id");
        validateIndexDefinition(meta, "inventory_transformation_lines", "inventory_transform_line_diary_uq",
                true, "stockdiary_id");
    }

    private static void validateRecipeIntegrity(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT product_id, COUNT(*) FROM recipe_versions "
                        + "WHERE UPPER(status) = 'ACTIVE' GROUP BY product_id HAVING COUNT(*) > 1")) {
            if (rows.next()) {
                throw new SQLException("Recipe migration blocked: product has more than one ACTIVE recipe: "
                        + rows.getString(1));
            }
        }
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT id FROM recipe_versions WHERE yield_quantity <= 0")) {
            if (rows.next()) {
                throw new SQLException("Recipe migration blocked: non-positive yield in recipe " + rows.getString(1));
            }
        }
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT recipe_version_id, line_no FROM recipe_components WHERE quantity <= 0")) {
            if (rows.next()) {
                throw new SQLException("Recipe migration blocked: non-positive component quantity.");
            }
        }
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT product_id, from_uom_id, to_uom_id FROM product_uom_conversions "
                        + "WHERE factor <= 0 OR from_uom_id = to_uom_id")) {
            if (rows.next()) {
                throw new SQLException("UOM conversion migration blocked: invalid conversion for product "
                        + rows.getString(1));
            }
        }
    }

    private static void validateColumns(DatabaseMetaData meta, String table, String... columns)
            throws SQLException {
        for (String column : columns) {
            if (!columnExists(meta, table, column)) {
                throw new SQLException("Table " + table + " is incomplete: missing " + column);
            }
        }
    }

    private static void validateForeignKey(DatabaseMetaData meta, String table,
            String foreignColumn, String referencedTable, String referencedColumn)
            throws SQLException {
        String actualTable = resolveTable(meta, table);
        if (actualTable == null) {
            throw new SQLException("Required table not found in current schema: " + table);
        }
        try (ResultSet rows = meta.getImportedKeys(currentCatalog(meta), currentSchema(meta), actualTable)) {
            while (rows.next()) {
                if (foreignColumn.equalsIgnoreCase(rows.getString("FKCOLUMN_NAME"))
                        && referencedTable.equalsIgnoreCase(rows.getString("PKTABLE_NAME"))
                        && referencedColumn.equalsIgnoreCase(rows.getString("PKCOLUMN_NAME"))) {
                    return;
                }
            }
        }
        throw new SQLException("Missing foreign key " + table + "." + foreignColumn
                + " -> " + referencedTable + "." + referencedColumn);
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

    private static void requireColumns(DatabaseMetaData meta, String table, String... columns)
            throws SQLException {
        if (!tableExists(meta, table)) {
            throw new SQLException("Required table not found in current schema: " + table);
        }
        for (String column : columns) {
            if (!columnExists(meta, table, column)) {
                throw new SQLException("Table " + table + " is incomplete: missing " + column);
            }
        }
    }

    private static void ensureForeignKey(Connection connection, DatabaseMetaData meta,
            String table, String foreignColumn, String referencedTable, String referencedColumn,
            String constraintName, String onDelete) throws SQLException {
        String actualTable = resolveTable(meta, table);
        if (actualTable == null) {
            throw new SQLException("Required table not found in current schema: " + table);
        }

        boolean anyOnColumn = false;
        try (ResultSet rows = meta.getImportedKeys(currentCatalog(meta), currentSchema(meta), actualTable)) {
            while (rows.next()) {
                if (!foreignColumn.equalsIgnoreCase(rows.getString("FKCOLUMN_NAME"))) {
                    continue;
                }
                anyOnColumn = true;
                String pkTable = rows.getString("PKTABLE_NAME");
                String pkColumn = rows.getString("PKCOLUMN_NAME");
                if (!referencedTable.equalsIgnoreCase(pkTable)
                        || !referencedColumn.equalsIgnoreCase(pkColumn)) {
                    throw new SQLException("Foreign key " + table + "." + foreignColumn
                            + " points to an unexpected target.");
                }
                short expectedRule = (short) ("SET NULL".equalsIgnoreCase(onDelete)
                        ? DatabaseMetaData.importedKeySetNull
                        : DatabaseMetaData.importedKeyRestrict);
                short actualRule = rows.getShort("DELETE_RULE");
                if (actualRule != expectedRule
                        && !(expectedRule == DatabaseMetaData.importedKeyRestrict
                        && actualRule == DatabaseMetaData.importedKeyNoAction)) {
                    throw new SQLException("Foreign key " + table + "." + foreignColumn
                            + " has an unexpected delete rule.");
                }
                return;
            }
        }

        if (anyOnColumn) {
            throw new SQLException("Foreign key " + table + "." + foreignColumn
                    + " is incompatible with expected schema.");
        }

        String clause = "SET NULL".equalsIgnoreCase(onDelete)
                ? " ON DELETE SET NULL"
                : " ON DELETE RESTRICT";
        execute(connection, "ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName
                + " FOREIGN KEY (" + foreignColumn + ") REFERENCES " + referencedTable
                + " (" + referencedColumn + ")" + clause);

        ensureForeignKey(connection, connection.getMetaData(), table, foreignColumn,
                referencedTable, referencedColumn, constraintName, onDelete);
    }

    private static void validateCompositePrimaryKey(DatabaseMetaData meta, String table,
            String firstColumn, String secondColumn) throws SQLException {
        validatePrimaryKey(meta, table, firstColumn, secondColumn);
    }

    private static void validatePrimaryKey(DatabaseMetaData meta, String table,
            String... expectedColumns) throws SQLException {
        String actualTable = resolveTable(meta, table);
        if (actualTable == null) {
            throw new SQLException("Required table not found in current schema: " + table);
        }
        java.util.TreeMap<Short, String> columns = new java.util.TreeMap<>();
        try (ResultSet rows = meta.getPrimaryKeys(currentCatalog(meta), currentSchema(meta), actualTable)) {
            while (rows.next()) {
                String column = rows.getString("COLUMN_NAME");
                short sequence = rows.getShort("KEY_SEQ");
                if (column != null) {
                    columns.put(sequence, column);
                }
            }
        }
        if (columns.size() != expectedColumns.length) {
            throw new SQLException("Table " + table + " has an invalid primary key.");
        }
        int index = 0;
        for (String column : columns.values()) {
            if (!expectedColumns[index++].equalsIgnoreCase(column)) {
                throw new SQLException("Table " + table + " has an invalid primary key.");
            }
        }
    }

    private static void ensureCascadeForeignKey(Connection connection, String table,
            String foreignColumn, String referencedTable, String referencedColumn,
            String constraintName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String actualTable = resolveTable(meta, table);
        if (actualTable == null) {
            throw new SQLException("Required table not found in current schema: " + table);
        }

        boolean anyForeignKeyOnColumn = false;
        try (ResultSet rows = meta.getImportedKeys(currentCatalog(meta), currentSchema(meta), actualTable)) {
            while (rows.next()) {
                String fkColumn = rows.getString("FKCOLUMN_NAME");
                if (!foreignColumn.equalsIgnoreCase(fkColumn)) {
                    continue;
                }
                anyForeignKeyOnColumn = true;
                String pkTable = rows.getString("PKTABLE_NAME");
                String pkColumn = rows.getString("PKCOLUMN_NAME");
                if (referencedTable.equalsIgnoreCase(pkTable)
                        && referencedColumn.equalsIgnoreCase(pkColumn)) {
                    if (rows.getShort("DELETE_RULE") != DatabaseMetaData.importedKeyCascade) {
                        throw new SQLException("Foreign key " + table + "." + foreignColumn
                                + " exists but must use ON DELETE CASCADE.");
                    }
                    return;
                }
                throw new SQLException("Foreign key " + table + "." + foreignColumn
                        + " points to an unexpected target.");
            }
        }

        if (anyForeignKeyOnColumn) {
            throw new SQLException("Foreign key " + table + "." + foreignColumn
                    + " is incompatible with expected schema.");
        }

        String orphanSql = "SELECT c." + foreignColumn + " FROM " + table + " c "
                + "LEFT JOIN " + referencedTable + " p ON p." + referencedColumn
                + " = c." + foreignColumn + " WHERE c." + foreignColumn
                + " IS NOT NULL AND p." + referencedColumn + " IS NULL";
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(orphanSql)) {
            if (rows.next()) {
                throw new SQLException("Cannot add foreign key " + table + "." + foreignColumn
                        + ": orphan reference " + rows.getString(1));
            }
        }

        execute(connection, "ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName
                + " FOREIGN KEY (" + foreignColumn + ") REFERENCES " + referencedTable
                + " (" + referencedColumn + ") ON DELETE CASCADE");
        validateCascadeForeignKey(connection.getMetaData(), table, foreignColumn,
                referencedTable, referencedColumn);
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

    private static void ensureRecipeUiAccess(Connection connection) throws SQLException {
        patchMenuRootForRecipes(connection);
        patchStandardRoleForRecipes(connection, "0", "Administrator role");
        patchStandardRoleForRecipes(connection, "1", "Manager role");
    }

    private static void patchMenuRootForRecipes(Connection connection) throws SQLException {
        byte[] content;
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT CONTENT FROM resources WHERE NAME = ? FOR UPDATE")) {
            select.setString(1, "Menu.Root");
            try (ResultSet row = select.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("Menu.Root resource is missing; recipe entries cannot be installed safely.");
                }
                content = row.getBytes(1);
            }
        }
        if (content == null) {
            throw new SQLException("Menu.Root content is null; recipe entries cannot be installed safely.");
        }
        String text = new String(content, StandardCharsets.UTF_8);
        String patched = patchRecipeMenuText(text);
        if (patched.equals(text)) {
            return;
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE resources SET CONTENT = ? WHERE NAME = ?")) {
            update.setBytes(1, patched.getBytes(StandardCharsets.UTF_8));
            update.setString(2, "Menu.Root");
            if (update.executeUpdate() != 1) {
                throw new SQLException("Menu.Root recipe entries were not updated exactly once.");
            }
        }
    }

    /**
     * Insert recipe panels into a customized inventory submenu without relying
     * on the stock template's exact icon path, namespace or whitespace.
     */
    static String patchRecipeMenuText(String text) throws SQLException {
        if (text == null) {
            throw new SQLException("Menu.Root has null content.");
        }
        final String[] classes = {
            "com.mx.kylgis.pos.inventory.transform.RecipeManagementPanel",
            "com.mx.kylgis.pos.inventory.transform.RecipeProductionPanel",
            "com.mx.kylgis.pos.inventory.transform.TransformationHistoryPanel",
            "com.mx.kylgis.pos.inventory.transform.ProductUomConversionPanel"
        };
        final String[] keys = {
            "Menu.Recipes", "Menu.RecipeProduction", "Menu.TransformationHistory",
            "Menu.ProductUomConversions"
        };
        final String[] icons = {"bundle.png", "stockmaint.png", "reports.png", "products.png"};
        int missing = 0;
        for (int i = 0; i < classes.length; i++) {
            if (!text.contains(classes[i])) {
                if (text.contains("\"" + keys[i] + "\"")) {
                    throw new SQLException("Menu.Root has a conflicting menu key: " + keys[i]);
                }
                missing++;
            }
        }
        if (missing == 0) {
            return text;
        }

        String[] lines = text.split("\n", -1);
        int inventoryStart = -1;
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (!trimmed.startsWith("//") && !trimmed.startsWith("#")
                    && trimmed.contains(".addSubmenu(")
                    && trimmed.contains("\"Menu.StockManagement\"")) {
                if (inventoryStart >= 0) {
                    throw new SQLException("Menu.Root contains multiple inventory submenus.");
                }
                inventoryStart = i;
            }
        }
        int start = inventoryStart < 0 ? 0 : inventoryStart + 1;
        int end = lines.length;
        if (inventoryStart >= 0) {
            for (int i = start; i < lines.length; i++) {
                String compact = lines[i].trim().replace(" ", "").replace("\t", "");
                if (compact.startsWith("//")) {
                    continue;
                }
                if (compact.contains("=group.addSubmenu(")
                        || compact.contains("=menu.addGroup(")) {
                    end = i;
                    break;
                }
            }
        }

        int anchor = -1;
        int best = Integer.MAX_VALUE;
        for (int i = start; i < end; i++) {
            String line = lines[i].trim();
            if (line.startsWith("//") || !line.contains("submenu.addPanel(")) {
                continue;
            }
            int priority = Integer.MAX_VALUE;
            if (line.contains("\"Menu.Bundle\"")) {
                priority = 0;
            } else if (line.contains("\"Menu.Products\"")
                    && line.contains("inventory.ProductsPanel")) {
                priority = 1;
            } else if (inventoryStart >= 0
                    && line.contains("\"Menu.StockMovement\"")) {
                priority = 2;
            }
            if (priority < best) {
                best = priority;
                anchor = i;
            }
        }
        if (anchor < 0 && inventoryStart >= 0
                && lines[inventoryStart].replace(" ", "").replace("\t", "")
                        .contains("submenu=group.addSubmenu(")) {
            anchor = inventoryStart;
        }
        if (anchor < 0) {
            throw new SQLException("Menu.Root inventory submenu cannot be identified safely; "
                    + "no recipe entries were written.");
        }

        int panelAt = lines[anchor].indexOf("submenu.addPanel(");
        String indent = panelAt < 0 ? "        " : lines[anchor].substring(0, panelAt);
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        StringBuilder insert = new StringBuilder();
        for (int i = 0; i < classes.length; i++) {
            if (!text.contains(classes[i])) {
                insert.append(indent).append("submenu.addPanel(\"/com/mx/kylgis/pos/images/")
                        .append(icons[i]).append("\", \"").append(keys[i])
                        .append("\", \"").append(classes[i]).append("\");")
                        .append(newline);
            }
        }
        int offset = 0;
        for (int i = 0; i <= anchor; i++) {
            offset += lines[i].length();
            if (i < lines.length - 1) {
                offset++; // Preserve the original LF, including any preceding CR.
            }
        }
        if (offset == text.length() && !text.endsWith("\n")) {
            insert.insert(0, newline);
        }
        String patched = text.substring(0, offset) + insert + text.substring(offset);
        for (String className : classes) {
            if (!patched.contains(className)) {
                throw new SQLException("Menu.Root recipe patch incomplete: " + className);
            }
        }
        return patched;
    }

    private static void patchStandardRoleForRecipes(Connection connection, String roleId,
            String expectedRoleName) throws SQLException {
        final String bundlePermission = "<class name=\"com.mx.kylgis.pos.inventory.BundlePanel\"/>";
        final String recipePermission = "<class name=\"com.mx.kylgis.pos.inventory.transform.RecipeManagementPanel\"/>";
        final String productionPermission = "<class name=\"com.mx.kylgis.pos.inventory.transform.RecipeProductionPanel\"/>";
        final String historyPermission = "<class name=\"com.mx.kylgis.pos.inventory.transform.TransformationHistoryPanel\"/>";
        final String uomConversionPermission = "<class name=\"com.mx.kylgis.pos.inventory.transform.ProductUomConversionPanel\"/>";
        String roleName;
        byte[] permissions;
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT NAME, PERMISSIONS FROM roles WHERE ID = ? FOR UPDATE")) {
            select.setString(1, roleId);
            try (ResultSet row = select.executeQuery()) {
                if (!row.next()) {
                    return;
                }
                roleName = row.getString(1);
                permissions = row.getBytes(2);
            }
        }
        if (!expectedRoleName.equals(roleName) || permissions == null) {
            return;
        }
        String text = new String(permissions, StandardCharsets.UTF_8);
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        String patched = text;
        if (!patched.contains(recipePermission) && patched.contains(bundlePermission)) {
            patched = patched.replace(bundlePermission,
                    bundlePermission + newline + "    " + recipePermission);
        }
        if (!patched.contains(productionPermission) && patched.contains(recipePermission)) {
            patched = patched.replace(recipePermission,
                    recipePermission + newline + "    " + productionPermission);
        }
        if (!patched.contains(historyPermission) && patched.contains(productionPermission)) {
            patched = patched.replace(productionPermission,
                    productionPermission + newline + "    " + historyPermission);
        }
        if (!patched.contains(uomConversionPermission) && patched.contains(historyPermission)) {
            patched = patched.replace(historyPermission,
                    historyPermission + newline + "    " + uomConversionPermission);
        }
        if (patched.equals(text)) {
            return;
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE roles SET PERMISSIONS = ? WHERE ID = ? AND NAME = ?")) {
            update.setBytes(1, patched.getBytes(StandardCharsets.UTF_8));
            update.setString(2, roleId);
            update.setString(3, expectedRoleName);
            if (update.executeUpdate() != 1) {
                throw new SQLException("Standard role recipe permissions were not updated exactly once: "
                        + expectedRoleName);
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

    private static void validateIndexDefinition(DatabaseMetaData meta, String table,
            String index, boolean unique, String... expectedColumns) throws SQLException {
        String actualTable = resolveTable(meta, table);
        if (actualTable == null) {
            throw new SQLException("Required table not found in current schema: " + table);
        }
        java.util.TreeMap<Short, String> columns = new java.util.TreeMap<>();
        Boolean nonUnique = null;
        try (ResultSet rows = meta.getIndexInfo(currentCatalog(meta), currentSchema(meta),
                actualTable, false, false)) {
            while (rows.next()) {
                String name = rows.getString("INDEX_NAME");
                if (name == null || !index.equalsIgnoreCase(name)) {
                    continue;
                }
                boolean rowNonUnique = rows.getBoolean("NON_UNIQUE");
                if (nonUnique != null && nonUnique.booleanValue() != rowNonUnique) {
                    throw new SQLException("Index " + index + " has inconsistent uniqueness metadata.");
                }
                nonUnique = rowNonUnique;
                String column = rows.getString("COLUMN_NAME");
                if (column != null) {
                    columns.put(rows.getShort("ORDINAL_POSITION"), column);
                }
            }
        }
        if (nonUnique == null || (unique && nonUnique.booleanValue())
                || columns.size() != expectedColumns.length) {
            throw new SQLException("Index " + index + " has an invalid definition on table " + table + ".");
        }
        int position = 0;
        for (String column : columns.values()) {
            if (!expectedColumns[position++].equalsIgnoreCase(column)) {
                throw new SQLException("Index " + index + " has an invalid definition on table " + table + ".");
            }
        }
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
