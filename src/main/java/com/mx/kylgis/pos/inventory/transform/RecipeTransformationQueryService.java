//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS and is distributed under the GPLv3+.
package com.mx.kylgis.pos.inventory.transform;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Session;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only traceability API for recipe versions and inventory transformations. */
public final class RecipeTransformationQueryService {

    private static final int MAX_HISTORY_ROWS = 500;

    private final Session session;

    public RecipeTransformationQueryService(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        this.session = session;
    }

    public List<TransformationSummary> listRecentTransformations(String locationId, int limit)
            throws BasicException {
        int boundedLimit = requireLimit(limit);
        boolean filterLocation = !isBlank(locationId);
        String sql = "SELECT id, type, status, location_id, recipe_version_id, reverses_id, "
                + "expected_output, actual_output, created_at, created_by, notes "
                + "FROM inventory_transformations "
                + (filterLocation ? "WHERE location_id = ? " : "")
                + "ORDER BY created_at DESC, id DESC LIMIT ?";
        try {
            Connection connection = session.getConnection();
            List<TransformationSummary> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int parameter = 1;
                if (filterLocation) {
                    statement.setString(parameter++, locationId);
                }
                statement.setInt(parameter, boundedLimit);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        result.add(readTransformationSummary(rows));
                    }
                }
            }
            return Collections.unmodifiableList(result);
        } catch (SQLException ex) {
            throw new BasicException("Unable to list inventory transformations.", ex);
        }
    }

    public TransformationDetail getTransformation(String transformationId) throws BasicException {
        requireId(transformationId, "Transformation id is required.");
        try {
            Connection connection = session.getConnection();
            TransformationSummary summary;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, type, status, location_id, recipe_version_id, reverses_id, "
                    + "expected_output, actual_output, created_at, created_by, notes "
                    + "FROM inventory_transformations WHERE id = ?")) {
                statement.setString(1, transformationId);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) {
                        throw new BasicException("Unknown inventory transformation.");
                    }
                    summary = readTransformationSummary(row);
                }
            }

            List<TransformationLine> lines = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT line_no, direction, product_id, attributesetinstance_id, quantity, "
                    + "uom_id, base_quantity, unit_cost, total_cost, stockdiary_id "
                    + "FROM inventory_transformation_lines WHERE transformation_id = ? "
                    + "ORDER BY line_no")) {
                statement.setString(1, transformationId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        lines.add(new TransformationLine(rows.getInt(1), rows.getString(2),
                                rows.getString(3), rows.getString(4), rows.getBigDecimal(5),
                                rows.getString(6), rows.getBigDecimal(7), rows.getBigDecimal(8),
                                rows.getBigDecimal(9), rows.getString(10)));
                    }
                }
            }
            if (lines.isEmpty()) {
                throw new BasicException("Inventory transformation has no traceability lines.");
            }
            return new TransformationDetail(summary, lines);
        } catch (SQLException ex) {
            throw new BasicException("Unable to read inventory transformation detail.", ex);
        }
    }

    public List<RecipeVersionSummary> listRecipeVersions(String productId) throws BasicException {
        requireId(productId, "Product id is required.");
        try {
            Connection connection = session.getConnection();
            List<RecipeVersionSummary> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, product_id, version_no, status, yield_quantity, yield_uom_id, "
                    + "created_at, created_by, notes FROM recipe_versions "
                    + "WHERE product_id = ? ORDER BY version_no DESC, created_at DESC")) {
                statement.setString(1, productId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        result.add(readRecipeSummary(rows));
                    }
                }
            }
            return Collections.unmodifiableList(result);
        } catch (SQLException ex) {
            throw new BasicException("Unable to list recipe versions.", ex);
        }
    }

    public RecipeVersionDetail getRecipeVersion(String recipeVersionId) throws BasicException {
        requireId(recipeVersionId, "Recipe version id is required.");
        try {
            Connection connection = session.getConnection();
            RecipeVersionSummary summary;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, product_id, version_no, status, yield_quantity, yield_uom_id, "
                    + "created_at, created_by, notes FROM recipe_versions WHERE id = ?")) {
                statement.setString(1, recipeVersionId);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) {
                        throw new BasicException("Unknown recipe version.");
                    }
                    summary = readRecipeSummary(row);
                }
            }

            List<RecipeComponent> components = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT line_no, component_product_id, quantity, uom_id, waste_allowance "
                    + "FROM recipe_components WHERE recipe_version_id = ? ORDER BY line_no")) {
                statement.setString(1, recipeVersionId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        components.add(new RecipeComponent(rows.getInt(1), rows.getString(2),
                                rows.getBigDecimal(3), rows.getString(4), rows.getBigDecimal(5)));
                    }
                }
            }
            return new RecipeVersionDetail(summary, components);
        } catch (SQLException ex) {
            throw new BasicException("Unable to read recipe version detail.", ex);
        }
    }

    static int requireLimit(int limit) throws BasicException {
        if (limit < 1 || limit > MAX_HISTORY_ROWS) {
            throw new BasicException("History limit must be between 1 and " + MAX_HISTORY_ROWS + ".");
        }
        return limit;
    }

    private static void requireId(String value, String message) throws BasicException {
        if (isBlank(value)) {
            throw new BasicException(message);
        }
    }

    private static TransformationSummary readTransformationSummary(ResultSet row) throws SQLException {
        return new TransformationSummary(row.getString(1), row.getString(2), row.getString(3),
                row.getString(4), row.getString(5), row.getString(6), row.getBigDecimal(7),
                row.getBigDecimal(8), row.getTimestamp(9), row.getString(10), row.getString(11));
    }

    private static RecipeVersionSummary readRecipeSummary(ResultSet row) throws SQLException {
        return new RecipeVersionSummary(row.getString(1), row.getString(2), row.getInt(3),
                row.getString(4), row.getBigDecimal(5), row.getString(6), row.getTimestamp(7),
                row.getString(8), row.getString(9));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class TransformationSummary {
        public final String id;
        public final String type;
        public final String status;
        public final String locationId;
        public final String recipeVersionId;
        public final String reversesId;
        public final BigDecimal expectedOutput;
        public final BigDecimal actualOutput;
        public final Timestamp createdAt;
        public final String createdBy;
        public final String notes;

        TransformationSummary(String id, String type, String status, String locationId,
                String recipeVersionId, String reversesId, BigDecimal expectedOutput,
                BigDecimal actualOutput, Timestamp createdAt, String createdBy, String notes) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.locationId = locationId;
            this.recipeVersionId = recipeVersionId;
            this.reversesId = reversesId;
            this.expectedOutput = expectedOutput;
            this.actualOutput = actualOutput;
            this.createdAt = createdAt;
            this.createdBy = createdBy;
            this.notes = notes;
        }
    }

    public static final class TransformationLine {
        public final int lineNo;
        public final String direction;
        public final String productId;
        public final String attributeSetInstanceId;
        public final BigDecimal quantity;
        public final String uomId;
        public final BigDecimal baseQuantity;
        public final BigDecimal unitCost;
        public final BigDecimal totalCost;
        public final String stockDiaryId;

        TransformationLine(int lineNo, String direction, String productId,
                String attributeSetInstanceId, BigDecimal quantity, String uomId,
                BigDecimal baseQuantity, BigDecimal unitCost, BigDecimal totalCost,
                String stockDiaryId) {
            this.lineNo = lineNo;
            this.direction = direction;
            this.productId = productId;
            this.attributeSetInstanceId = attributeSetInstanceId;
            this.quantity = quantity;
            this.uomId = uomId;
            this.baseQuantity = baseQuantity;
            this.unitCost = unitCost;
            this.totalCost = totalCost;
            this.stockDiaryId = stockDiaryId;
        }
    }

    public static final class TransformationDetail {
        public final TransformationSummary summary;
        public final List<TransformationLine> lines;

        TransformationDetail(TransformationSummary summary, List<TransformationLine> lines) {
            this.summary = summary;
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        }
    }

    public static final class RecipeVersionSummary {
        public final String id;
        public final String productId;
        public final int versionNo;
        public final String status;
        public final BigDecimal yieldQuantity;
        public final String yieldUomId;
        public final Timestamp createdAt;
        public final String createdBy;
        public final String notes;

        RecipeVersionSummary(String id, String productId, int versionNo, String status,
                BigDecimal yieldQuantity, String yieldUomId, Timestamp createdAt,
                String createdBy, String notes) {
            this.id = id;
            this.productId = productId;
            this.versionNo = versionNo;
            this.status = status;
            this.yieldQuantity = yieldQuantity;
            this.yieldUomId = yieldUomId;
            this.createdAt = createdAt;
            this.createdBy = createdBy;
            this.notes = notes;
        }
    }

    public static final class RecipeComponent {
        public final int lineNo;
        public final String productId;
        public final BigDecimal quantity;
        public final String uomId;
        public final BigDecimal wasteAllowance;

        RecipeComponent(int lineNo, String productId, BigDecimal quantity,
                String uomId, BigDecimal wasteAllowance) {
            this.lineNo = lineNo;
            this.productId = productId;
            this.quantity = quantity;
            this.uomId = uomId;
            this.wasteAllowance = wasteAllowance;
        }
    }

    public static final class RecipeVersionDetail {
        public final RecipeVersionSummary summary;
        public final List<RecipeComponent> components;

        RecipeVersionDetail(RecipeVersionSummary summary, List<RecipeComponent> components) {
            this.summary = summary;
            this.components = Collections.unmodifiableList(new ArrayList<>(components));
        }
    }
}
