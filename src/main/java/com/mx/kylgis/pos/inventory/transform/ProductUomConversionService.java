//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS and is distributed under the GPLv3+.
package com.mx.kylgis.pos.inventory.transform;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.data.loader.Transaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Product-specific direct UOM conversions into the product inventory UOM.
 *
 * This deliberately does not model SKU transformations such as box -> pieces
 * when box and piece are different products. Those belong to
 * InventoryTransformationService.postConversion().
 */
public final class ProductUomConversionService {

    private static final int FACTOR_SCALE = 9;

    private final Session session;

    public ProductUomConversionService(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        this.session = session;
    }

    /**
     * Creates or updates a direct conversion from an alternate UOM into the
     * product's configured inventory UOM. Product-row locking serializes an
     * insert with concurrent recipe/transform configuration for the same SKU.
     */
    public ConversionInfo saveToInventory(final String productId,
            final String fromUomId, final BigDecimal factor, final boolean active)
            throws BasicException {
        if (isBlank(productId) || isBlank(fromUomId)) {
            throw new BasicException("Product and source UOM are required.");
        }
        final BigDecimal normalizedFactor = normalizeFactor(factor);

        return new Transaction<ConversionInfo>(session) {
            @Override
            protected ConversionInfo transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    String inventoryUomId = lockInventoryUom(connection, productId);
                    requireUom(connection, fromUomId);
                    if (fromUomId.equals(inventoryUomId)) {
                        throw new BasicException("Source UOM already is the product inventory UOM.");
                    }

                    boolean exists;
                    BigDecimal existingFactor = null;
                    boolean existingActive = false;
                    try (PreparedStatement select = connection.prepareStatement(
                            "SELECT factor, active FROM product_uom_conversions "
                            + "WHERE product_id = ? AND from_uom_id = ? AND to_uom_id = ? FOR UPDATE")) {
                        select.setString(1, productId);
                        select.setString(2, fromUomId);
                        select.setString(3, inventoryUomId);
                        try (ResultSet row = select.executeQuery()) {
                            exists = row.next();
                            if (exists) {
                                existingFactor = row.getBigDecimal(1);
                                existingActive = row.getBoolean(2);
                                if (row.next()) {
                                    throw new SQLException("Duplicate product UOM conversion row.");
                                }
                            }
                        }
                    }

                    if (exists) {
                        if ((existingFactor == null || existingFactor.compareTo(normalizedFactor) != 0)
                                || (existingActive && !active)) {
                            assertNoActiveRecipeReference(connection, productId, fromUomId);
                        }
                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE product_uom_conversions SET factor = ?, active = ? "
                                + "WHERE product_id = ? AND from_uom_id = ? AND to_uom_id = ?")) {
                            update.setBigDecimal(1, normalizedFactor);
                            update.setBoolean(2, active);
                            update.setString(3, productId);
                            update.setString(4, fromUomId);
                            update.setString(5, inventoryUomId);
                            if (update.executeUpdate() != 1) {
                                throw new SQLException("Product UOM conversion was not updated exactly once.");
                            }
                        }
                    } else {
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO product_uom_conversions "
                                + "(product_id, from_uom_id, to_uom_id, factor, active) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                            insert.setString(1, productId);
                            insert.setString(2, fromUomId);
                            insert.setString(3, inventoryUomId);
                            insert.setBigDecimal(4, normalizedFactor);
                            insert.setBoolean(5, active);
                            if (insert.executeUpdate() != 1) {
                                throw new SQLException("Product UOM conversion was not inserted exactly once.");
                            }
                        }
                    }
                    return new ConversionInfo(productId, fromUomId, inventoryUomId,
                            normalizedFactor, active);
                } catch (SQLException ex) {
                    throw new BasicException("Unable to save product UOM conversion.", ex);
                }
            }
        }.execute();
    }

    /** Soft-disable keeps historical recipe references auditable. */
    public void setActive(final String productId, final String fromUomId,
            final boolean active) throws BasicException {
        if (isBlank(productId) || isBlank(fromUomId)) {
            throw new BasicException("Product and source UOM are required.");
        }
        new Transaction<Void>(session) {
            @Override
            protected Void transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    String inventoryUomId = lockInventoryUom(connection, productId);
                    if (!active) {
                        assertNoActiveRecipeReference(connection, productId, fromUomId);
                    }
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE product_uom_conversions SET active = ? "
                            + "WHERE product_id = ? AND from_uom_id = ? AND to_uom_id = ?")) {
                        update.setBoolean(1, active);
                        update.setString(2, productId);
                        update.setString(3, fromUomId);
                        update.setString(4, inventoryUomId);
                        if (update.executeUpdate() != 1) {
                            throw new BasicException("Unknown product UOM conversion.");
                        }
                    }
                    return null;
                } catch (SQLException ex) {
                    throw new BasicException("Unable to change product UOM conversion state.", ex);
                }
            }
        }.execute();
    }

    public List<ConversionInfo> listForProduct(String productId) throws BasicException {
        if (isBlank(productId)) {
            throw new BasicException("Product is required.");
        }
        try {
            Connection connection = session.getConnection();
            List<ConversionInfo> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT from_uom_id, to_uom_id, factor, active "
                    + "FROM product_uom_conversions WHERE product_id = ? "
                    + "ORDER BY from_uom_id, to_uom_id")) {
                statement.setString(1, productId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        result.add(new ConversionInfo(productId, rows.getString(1),
                                rows.getString(2), rows.getBigDecimal(3), rows.getBoolean(4)));
                    }
                }
            }
            return result;
        } catch (SQLException ex) {
            throw new BasicException("Unable to list product UOM conversions.", ex);
        }
    }

    static BigDecimal normalizeFactor(BigDecimal factor) throws BasicException {
        if (factor == null || factor.signum() <= 0) {
            throw new BasicException("UOM conversion factor must be positive.");
        }
        BigDecimal normalized = factor.setScale(FACTOR_SCALE, RoundingMode.HALF_UP);
        if (normalized.signum() <= 0) {
            throw new BasicException("UOM conversion factor rounds to zero.");
        }
        return normalized;
    }

    private static void assertNoActiveRecipeReference(Connection connection,
            String productId, String fromUomId) throws SQLException, BasicException {
        try (PreparedStatement yield = connection.prepareStatement(
                "SELECT id FROM recipe_versions WHERE UPPER(status) = 'ACTIVE' "
                + "AND product_id = ? AND yield_uom_id = ?")) {
            yield.setString(1, productId);
            yield.setString(2, fromUomId);
            try (ResultSet rows = yield.executeQuery()) {
                if (rows.next()) {
                    throw new BasicException("UOM conversion is referenced by ACTIVE recipe "
                            + rows.getString(1) + "; publish a new recipe version first.");
                }
            }
        }
        try (PreparedStatement component = connection.prepareStatement(
                "SELECT rv.id FROM recipe_versions rv "
                + "JOIN recipe_components rc ON rc.recipe_version_id = rv.id "
                + "WHERE UPPER(rv.status) = 'ACTIVE' "
                + "AND rc.component_product_id = ? AND rc.uom_id = ?")) {
            component.setString(1, productId);
            component.setString(2, fromUomId);
            try (ResultSet rows = component.executeQuery()) {
                if (rows.next()) {
                    throw new BasicException("UOM conversion is referenced by ACTIVE recipe "
                            + rows.getString(1) + "; publish a new recipe version first.");
                }
            }
        }
    }

    private static String lockInventoryUom(Connection connection, String productId)
            throws SQLException, BasicException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT uom FROM products WHERE id = ? FOR UPDATE")) {
            statement.setString(1, productId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new BasicException("Unknown product for UOM conversion.");
                }
                String uom = row.getString(1);
                if (isBlank(uom)) {
                    throw new BasicException("Product has no inventory UOM.");
                }
                return uom;
            }
        }
    }

    private static void requireUom(Connection connection, String uomId)
            throws SQLException, BasicException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM uom WHERE id = ?")) {
            statement.setString(1, uomId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new BasicException("Unknown UOM: " + uomId);
                }
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class ConversionInfo {
        public final String productId;
        public final String fromUomId;
        public final String toUomId;
        public final BigDecimal factor;
        public final boolean active;

        ConversionInfo(String productId, String fromUomId, String toUomId,
                BigDecimal factor, boolean active) {
            this.productId = productId;
            this.fromUomId = fromUomId;
            this.toUomId = toUomId;
            this.factor = factor;
            this.active = active;
        }
    }
}
