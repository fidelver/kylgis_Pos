//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS and is distributed under the GPLv3+.
package com.mx.kylgis.pos.inventory.transform;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.forms.AppLocal;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
        String sql = "SELECT t.id, t.type, t.status, t.location_id, l.name, "
                + "t.recipe_version_id, t.reverses_id, t.expected_output, t.actual_output, "
                + "t.created_at, t.created_by, t.notes "
                + "FROM inventory_transformations t "
                + "LEFT JOIN locations l ON l.id = t.location_id "
                + (filterLocation ? "WHERE t.location_id = ? " : "")
                + "ORDER BY t.created_at DESC, t.id DESC LIMIT ?";
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
                    "SELECT t.id, t.type, t.status, t.location_id, l.name, "
                    + "t.recipe_version_id, t.reverses_id, t.expected_output, t.actual_output, "
                    + "t.created_at, t.created_by, t.notes "
                    + "FROM inventory_transformations t "
                    + "LEFT JOIN locations l ON l.id = t.location_id WHERE t.id = ?")) {
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
                    "SELECT l.line_no, l.direction, l.product_id, p.reference, p.name, "
                    + "l.attributesetinstance_id, l.quantity, l.uom_id, u.name, "
                    + "l.base_quantity, l.unit_cost, l.total_cost, l.stockdiary_id "
                    + "FROM inventory_transformation_lines l "
                    + "LEFT JOIN products p ON p.id = l.product_id "
                    + "LEFT JOIN uom u ON u.id = l.uom_id "
                    + "WHERE l.transformation_id = ? ORDER BY l.line_no")) {
                statement.setString(1, transformationId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        lines.add(new TransformationLine(rows.getInt(1), rows.getString(2),
                                rows.getString(3), rows.getString(4), rows.getString(5),
                                rows.getString(6), rows.getBigDecimal(7), rows.getString(8),
                                rows.getString(9), rows.getBigDecimal(10), rows.getBigDecimal(11),
                                rows.getBigDecimal(12), rows.getString(13)));
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
                    "SELECT rv.id, rv.product_id, rv.version_no, rv.status, rv.yield_quantity, "
                    + "rv.yield_uom_id, u.name, rv.created_at, rv.created_by, rv.notes "
                    + "FROM recipe_versions rv LEFT JOIN uom u ON u.id = rv.yield_uom_id "
                    + "WHERE rv.product_id = ? ORDER BY rv.version_no DESC, rv.created_at DESC")) {
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
                    "SELECT rv.id, rv.product_id, rv.version_no, rv.status, rv.yield_quantity, "
                    + "rv.yield_uom_id, u.name, rv.created_at, rv.created_by, rv.notes "
                    + "FROM recipe_versions rv LEFT JOIN uom u ON u.id = rv.yield_uom_id "
                    + "WHERE rv.id = ?")) {
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
                    "SELECT rc.line_no, rc.component_product_id, p.reference, p.name, "
                    + "rc.quantity, rc.uom_id, u.name, rc.waste_allowance "
                    + "FROM recipe_components rc "
                    + "LEFT JOIN products p ON p.id = rc.component_product_id "
                    + "LEFT JOIN uom u ON u.id = rc.uom_id "
                    + "WHERE rc.recipe_version_id = ? ORDER BY rc.line_no")) {
                statement.setString(1, recipeVersionId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        components.add(new RecipeComponent(rows.getInt(1), rows.getString(2),
                                rows.getString(3), rows.getString(4), rows.getBigDecimal(5),
                                rows.getString(6), rows.getString(7), rows.getBigDecimal(8)));
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
                row.getString(4), row.getString(5), row.getString(6), row.getString(7),
                row.getBigDecimal(8), row.getBigDecimal(9), row.getTimestamp(10),
                row.getString(11), row.getString(12));
    }

    private static RecipeVersionSummary readRecipeSummary(ResultSet row) throws SQLException {
        return new RecipeVersionSummary(row.getString(1), row.getString(2), row.getInt(3),
                row.getString(4), row.getBigDecimal(5), row.getString(6), row.getString(7),
                row.getTimestamp(8), row.getString(9), row.getString(10));
    }

    private static String localizedEnum(String prefix, String value) {
        if (isBlank(value)) {
            return "";
        }
        String key = prefix + value.trim().toLowerCase(Locale.ROOT);
        String translated = AppLocal.getIntString(key);
        return translated.startsWith("** " + key + " **") ? value : translated;
    }

    private static String localizedFallback(String key, String fallback) {
        String translated = AppLocal.getIntString(key);
        if (!translated.startsWith("** " + key + " **")) {
            return translated;
        }
        if ("recipe.catalog.unknown.product".equals(key)) {
            return "Producto sin nombre";
        }
        if ("recipe.catalog.unknown.uom".equals(key)) {
            return "Unidad sin nombre";
        }
        if ("recipe.catalog.unknown.location".equals(key)) {
            return "Ubicación sin nombre";
        }
        return "Sin descripción";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class TransformationSummary {
        public final String id;
        public final String type;
        public final String status;
        public final String locationId;
        public final String locationName;
        public final String recipeVersionId;
        public final String reversesId;
        public final BigDecimal expectedOutput;
        public final BigDecimal actualOutput;
        public final Timestamp createdAt;
        public final String createdBy;
        public final String notes;

        TransformationSummary(String id, String type, String status, String locationId,
                String locationName, String recipeVersionId, String reversesId,
                BigDecimal expectedOutput, BigDecimal actualOutput, Timestamp createdAt,
                String createdBy, String notes) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.locationId = locationId;
            this.locationName = locationName;
            this.recipeVersionId = recipeVersionId;
            this.reversesId = reversesId;
            this.expectedOutput = expectedOutput;
            this.actualOutput = actualOutput;
            this.createdAt = createdAt;
            this.createdBy = createdBy;
            this.notes = notes;
        }

        public String locationLabel() {
            String name = locationName == null ? "" : locationName.trim();
            return name.isEmpty() ? localizedFallback("recipe.catalog.unknown.location", locationId) : name;
        }

        public String typeLabel() {
            return localizedEnum("transform.type.", type);
        }

        public String statusLabel() {
            return localizedEnum("transform.status.", status);
        }
    }

    public static final class TransformationLine {
        public final int lineNo;
        public final String direction;
        public final String productId;
        public final String productReference;
        public final String productName;
        public final String attributeSetInstanceId;
        public final BigDecimal quantity;
        public final String uomId;
        public final String uomName;
        public final BigDecimal baseQuantity;
        public final BigDecimal unitCost;
        public final BigDecimal totalCost;
        public final String stockDiaryId;

        TransformationLine(int lineNo, String direction, String productId,
                String productReference, String productName, String attributeSetInstanceId,
                BigDecimal quantity, String uomId, String uomName, BigDecimal baseQuantity,
                BigDecimal unitCost, BigDecimal totalCost, String stockDiaryId) {
            this.lineNo = lineNo;
            this.direction = direction;
            this.productId = productId;
            this.productReference = productReference;
            this.productName = productName;
            this.attributeSetInstanceId = attributeSetInstanceId;
            this.quantity = quantity;
            this.uomId = uomId;
            this.uomName = uomName;
            this.baseQuantity = baseQuantity;
            this.unitCost = unitCost;
            this.totalCost = totalCost;
            this.stockDiaryId = stockDiaryId;
        }

        public String productLabel() {
            String name = productName == null ? "" : productName.trim();
            String reference = productReference == null ? "" : productReference.trim();
            if (name.isEmpty()) {
                return reference.isEmpty()
                        ? localizedFallback("recipe.catalog.unknown.product", productId)
                        : reference;
            }
            return reference.isEmpty() ? name : reference + " - " + name;
        }

        public String uomLabel() {
            String name = uomName == null ? "" : uomName.trim();
            return name.isEmpty() ? localizedFallback("recipe.catalog.unknown.uom", uomId) : name;
        }

        public String directionLabel() {
            return localizedEnum("transform.direction.", direction);
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
        public final String yieldUomName;
        public final Timestamp createdAt;
        public final String createdBy;
        public final String notes;

        RecipeVersionSummary(String id, String productId, int versionNo, String status,
                BigDecimal yieldQuantity, String yieldUomId, String yieldUomName,
                Timestamp createdAt, String createdBy, String notes) {
            this.id = id;
            this.productId = productId;
            this.versionNo = versionNo;
            this.status = status;
            this.yieldQuantity = yieldQuantity;
            this.yieldUomId = yieldUomId;
            this.yieldUomName = yieldUomName;
            this.createdAt = createdAt;
            this.createdBy = createdBy;
            this.notes = notes;
        }

        public String yieldUomLabel() {
            String name = yieldUomName == null ? "" : yieldUomName.trim();
            return name.isEmpty() ? localizedFallback("recipe.catalog.unknown.uom", yieldUomId) : name;
        }

        public String statusLabel() {
            return localizedEnum("transform.status.", status);
        }
    }

    public static final class RecipeComponent {
        public final int lineNo;
        public final String productId;
        public final String productReference;
        public final String productName;
        public final BigDecimal quantity;
        public final String uomId;
        public final String uomName;
        public final BigDecimal wasteAllowance;

        RecipeComponent(int lineNo, String productId, String productReference,
                String productName, BigDecimal quantity, String uomId, String uomName,
                BigDecimal wasteAllowance) {
            this.lineNo = lineNo;
            this.productId = productId;
            this.productReference = productReference;
            this.productName = productName;
            this.quantity = quantity;
            this.uomId = uomId;
            this.uomName = uomName;
            this.wasteAllowance = wasteAllowance;
        }

        public String productLabel() {
            String name = productName == null ? "" : productName.trim();
            String reference = productReference == null ? "" : productReference.trim();
            if (name.isEmpty()) {
                return reference.isEmpty()
                        ? localizedFallback("recipe.catalog.unknown.product", productId)
                        : reference;
            }
            return reference.isEmpty() ? name : reference + " - " + name;
        }

        public String uomLabel() {
            String name = uomName == null ? "" : uomName.trim();
            return name.isEmpty() ? localizedFallback("recipe.catalog.unknown.uom", uomId) : name;
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