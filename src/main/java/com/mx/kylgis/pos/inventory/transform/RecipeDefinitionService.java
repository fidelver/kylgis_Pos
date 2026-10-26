//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS and is distributed under the GPLv3+.
package com.mx.kylgis.pos.inventory.transform;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.data.loader.Transaction;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Isolated domain service for immutable recipe versions.
 *
 * Recipe ingredients are deliberately independent from products_bundle. A
 * produced semi-finished product is stock on its own and must be consumed as
 * such by later recipes/bundles instead of recursively consuming its raw
 * ingredients again.
 */
public final class RecipeDefinitionService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_RETIRED = "RETIRED";
    public static final String MODE_PRODUCTION = "PRODUCTION";
    public static final String MODE_SALE_CONSUMPTION = "SALE_CONSUMPTION";
    private static final int MAX_GRAPH_DEPTH = 128;

    private final Session session;

    public RecipeDefinitionService(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        this.session = session;
    }

    public String createDraft(final RecipeDraft draft) throws BasicException {
        return createDraftInternal(draft, false);
    }

    public String createNextDraft(String id, String productId, BigDecimal yieldQuantity,
            String yieldUomId, String createdBy, String notes,
            List<ComponentDraft> components) throws BasicException {
        return createNextDraft(id, productId, yieldQuantity, yieldUomId, createdBy, notes,
                components, MODE_PRODUCTION);
    }

    public String createNextDraft(String id, String productId, BigDecimal yieldQuantity,
            String yieldUomId, String createdBy, String notes,
            List<ComponentDraft> components, String recipeMode) throws BasicException {
        return createDraftInternal(new RecipeDraft(id, productId, 1, yieldQuantity,
                yieldUomId, createdBy, notes, components, recipeMode), true);
    }

    /**
     * Loads an immutable version into an editable template. Nothing is written
     * until the caller saves it through createNextDraft, which guarantees a new
     * version number and leaves the source version intact.
     */
    public RecipeDraft loadRevisionTemplate(final String recipeVersionId, final String createdBy)
            throws BasicException {
        if (isBlank(recipeVersionId) || isBlank(createdBy)) {
            throw new BasicException("Recipe version and user are required.");
        }
        return new Transaction<RecipeDraft>(session) {
            @Override
            protected RecipeDraft transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    String productId;
                    int versionNo;
                    String status;
                    BigDecimal yieldQuantity;
                    String yieldUomId;
                    String notes;
                    String recipeMode;
                    try (PreparedStatement header = connection.prepareStatement(
                            "SELECT product_id, version_no, status, yield_quantity, yield_uom_id, "
                            + "notes, recipe_mode FROM recipe_versions WHERE id = ?")) {
                        header.setString(1, recipeVersionId);
                        try (ResultSet row = header.executeQuery()) {
                            if (!row.next()) {
                                throw new BasicException("Unknown recipe version.");
                            }
                            productId = row.getString(1);
                            versionNo = row.getInt(2);
                            status = row.getString(3);
                            yieldQuantity = row.getBigDecimal(4);
                            yieldUomId = row.getString(5);
                            notes = row.getString(6);
                            recipeMode = row.getString(7);
                        }
                    }
                    if (!STATUS_ACTIVE.equalsIgnoreCase(status)
                            && !STATUS_DRAFT.equalsIgnoreCase(status)) {
                        throw new BasicException("Only ACTIVE or DRAFT recipes can be edited as a new version.");
                    }

                    List<ComponentDraft> components = new ArrayList<>();
                    try (PreparedStatement lines = connection.prepareStatement(
                            "SELECT component_product_id, quantity, uom_id, waste_allowance "
                            + "FROM recipe_components WHERE recipe_version_id = ? ORDER BY line_no")) {
                        lines.setString(1, recipeVersionId);
                        try (ResultSet rows = lines.executeQuery()) {
                            while (rows.next()) {
                                components.add(new ComponentDraft(rows.getString(1),
                                        rows.getBigDecimal(2), rows.getString(3), rows.getBigDecimal(4)));
                            }
                        }
                    }
                    if (components.isEmpty()) {
                        throw new BasicException("Recipe cannot be edited without components.");
                    }
                    return new RecipeDraft(recipeVersionId, productId, versionNo,
                            yieldQuantity, yieldUomId, createdBy, notes, components, recipeMode);
                } catch (SQLException ex) {
                    throw new BasicException("Unable to load recipe for editing.", ex);
                }
            }
        }.execute();
    }

    private String createDraftInternal(final RecipeDraft draft, final boolean autoVersion)
            throws BasicException {
        validateDraft(draft);
        return new Transaction<String>(session) {
            @Override
            protected String transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    Set<String> productIds = new HashSet<>();
                    productIds.add(draft.productId);
                    for (ComponentDraft component : draft.components) {
                        productIds.add(component.productId);
                    }
                    List<String> orderedProductIds = new ArrayList<>(productIds);
                    Collections.sort(orderedProductIds);
                    Map<String, ProductUnit> productUnits = new HashMap<>();
                    for (String productId : orderedProductIds) {
                        productUnits.put(productId, lockProductUnit(connection, productId));
                    }

                    int resolvedVersionNo = draft.versionNo;
                    if (autoVersion) {
                        try (PreparedStatement nextVersion = connection.prepareStatement(
                                "SELECT COALESCE(MAX(version_no), 0) + 1 FROM recipe_versions WHERE product_id = ?")) {
                            nextVersion.setString(1, draft.productId);
                            try (ResultSet row = nextVersion.executeQuery()) {
                                if (!row.next()) {
                                    throw new SQLException("Unable to calculate next recipe version.");
                                }
                                resolvedVersionNo = row.getInt(1);
                            }
                        }
                        if (resolvedVersionNo <= 0) {
                            throw new SQLException("Calculated recipe version is invalid.");
                        }
                    }

                    ProductUnit resultUnit = productUnits.get(draft.productId);
                    requireConvertible(connection, draft.productId, draft.yieldUomId, resultUnit.uomId);
                    for (ComponentDraft component : draft.components) {
                        ProductUnit componentUnit = productUnits.get(component.productId);
                        requireConvertible(connection, component.productId, component.uomId, componentUnit.uomId);
                    }

                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO recipe_versions "
                            + "(id, product_id, version_no, status, recipe_mode, yield_quantity, yield_uom_id, "
                            + "created_at, created_by, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        insert.setString(1, draft.id);
                        insert.setString(2, draft.productId);
                        insert.setInt(3, resolvedVersionNo);
                        insert.setString(4, STATUS_DRAFT);
                        insert.setString(5, draft.recipeMode);
                        insert.setBigDecimal(6, draft.yieldQuantity);
                        insert.setString(7, draft.yieldUomId);
                        insert.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
                        insert.setString(9, draft.createdBy);
                        insert.setString(10, draft.notes);
                        if (insert.executeUpdate() != 1) {
                            throw new SQLException("Recipe header was not inserted exactly once.");
                        }
                    }

                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO recipe_components "
                            + "(recipe_version_id, line_no, component_product_id, quantity, uom_id, waste_allowance) "
                            + "VALUES (?, ?, ?, ?, ?, ?)")) {
                        int line = 1;
                        for (ComponentDraft component : draft.components) {
                            insert.setString(1, draft.id);
                            insert.setInt(2, line++);
                            insert.setString(3, component.productId);
                            insert.setBigDecimal(4, component.quantity);
                            insert.setString(5, component.uomId);
                            if (component.wasteAllowance == null) {
                                insert.setNull(6, java.sql.Types.DECIMAL);
                            } else {
                                insert.setBigDecimal(6, component.wasteAllowance);
                            }
                            if (insert.executeUpdate() != 1) {
                                throw new SQLException("Recipe component was not inserted exactly once.");
                            }
                        }
                    }
                    return draft.id;
                } catch (SQLException ex) {
                    throw new BasicException("Unable to create recipe draft.", ex);
                }
            }
        }.execute();
    }

    /**
     * Activates an immutable recipe version after replacing any previous ACTIVE
     * version for the same product. The product row is the serialization lock.
     */
    public void activate(final String recipeVersionId) throws BasicException {
        if (isBlank(recipeVersionId)) {
            throw new BasicException("Recipe version id is required.");
        }
        new Transaction<Void>(session) {
            @Override
            protected Void transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    RecipeHeader recipe = loadRecipeForUpdate(connection, recipeVersionId);
                    if (!STATUS_DRAFT.equals(recipe.status)) {
                        throw new BasicException("Only DRAFT recipe versions can be activated.");
                    }
                    ensureRecipeHasComponents(connection, recipe.id);
                    lockRecipeGraphProducts(connection, recipe);
                    validateRecipeUnits(connection, recipe);
                    assertActivationGraphAcyclic(connection, recipe);

                    try (PreparedStatement retire = connection.prepareStatement(
                            "UPDATE recipe_versions SET status = ? WHERE product_id = ? AND UPPER(status) = ? "
                            + "AND UPPER(recipe_mode) = ?")) {
                        retire.setString(1, STATUS_RETIRED);
                        retire.setString(2, recipe.productId);
                        retire.setString(3, STATUS_ACTIVE);
                        retire.setString(4, recipe.recipeMode);
                        retire.executeUpdate();
                    }
                    try (PreparedStatement activate = connection.prepareStatement(
                            "UPDATE recipe_versions SET status = ? WHERE id = ? AND status = ?")) {
                        activate.setString(1, STATUS_ACTIVE);
                        activate.setString(2, recipe.id);
                        activate.setString(3, STATUS_DRAFT);
                        if (activate.executeUpdate() != 1) {
                            throw new SQLException("Recipe activation lost its expected DRAFT state.");
                        }
                    }
                    if (MODE_SALE_CONSUMPTION.equals(recipe.recipeMode)) {
                        SaleRecipeConsumptionService.refreshActiveRecipeCosts(connection);
                    }
                    return null;
                } catch (SQLException ex) {
                    throw new BasicException("Unable to activate recipe version.", ex);
                }
            }
        }.execute();
    }

    /**
     * Retires a DRAFT or ACTIVE recipe without deleting its immutable
     * components/history. Repeating the operation on an already RETIRED
     * version is an idempotent no-op.
     */
    public void retire(final String recipeVersionId) throws BasicException {
        if (isBlank(recipeVersionId)) {
            throw new BasicException("Recipe version id is required.");
        }
        new Transaction<Void>(session) {
            @Override
            protected Void transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    RecipeHeader recipe = loadRecipeForUpdate(connection, recipeVersionId);
                    lockProductUnit(connection, recipe.productId);
                    if (STATUS_RETIRED.equals(recipe.status)) {
                        return null;
                    }
                    if (!STATUS_DRAFT.equals(recipe.status) && !STATUS_ACTIVE.equals(recipe.status)) {
                        throw new BasicException("Only DRAFT or ACTIVE recipe versions can be retired.");
                    }
                    try (PreparedStatement retire = connection.prepareStatement(
                            "UPDATE recipe_versions SET status = ? WHERE id = ? AND status = ?")) {
                        retire.setString(1, STATUS_RETIRED);
                        retire.setString(2, recipe.id);
                        retire.setString(3, recipe.status);
                        if (retire.executeUpdate() != 1) {
                            throw new SQLException("Recipe retirement lost its expected state.");
                        }
                    }
                    return null;
                } catch (SQLException ex) {
                    throw new BasicException("Unable to retire recipe version.", ex);
                }
            }
        }.execute();
    }

    private static void validateRecipeUnits(Connection connection, RecipeHeader recipe)
            throws SQLException, BasicException {
        try (PreparedStatement header = connection.prepareStatement(
                "SELECT rv.product_id, rv.yield_uom_id, p.uom "
                + "FROM recipe_versions rv JOIN products p ON p.id = rv.product_id "
                + "WHERE rv.id = ?")) {
            header.setString(1, recipe.id);
            try (ResultSet row = header.executeQuery()) {
                if (!row.next()) {
                    throw new BasicException("Unknown recipe version during UOM validation.");
                }
                requireConvertible(connection, row.getString(1), row.getString(2), row.getString(3));
            }
        }
        try (PreparedStatement components = connection.prepareStatement(
                "SELECT rc.component_product_id, rc.uom_id, p.uom "
                + "FROM recipe_components rc JOIN products p ON p.id = rc.component_product_id "
                + "WHERE rc.recipe_version_id = ? ORDER BY rc.line_no")) {
            components.setString(1, recipe.id);
            try (ResultSet rows = components.executeQuery()) {
                while (rows.next()) {
                    requireConvertible(connection, rows.getString(1), rows.getString(2), rows.getString(3));
                }
            }
        }
    }

    /**
     * Serializes graph publication across concurrent recipe activations. Every
     * product participating in the currently ACTIVE graph plus the candidate
     * graph is locked in one deterministic order before cycle validation.
     */
    private static void lockRecipeGraphProducts(Connection connection, RecipeHeader candidate)
            throws SQLException, BasicException {
        Set<String> productIds = new HashSet<>();
        productIds.add(candidate.productId);
        String sql = "SELECT rv.product_id, rc.component_product_id "
                + "FROM recipe_versions rv JOIN recipe_components rc ON rc.recipe_version_id = rv.id "
                + "WHERE UPPER(rv.status) = ? OR rv.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, STATUS_ACTIVE);
            statement.setString(2, candidate.id);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    productIds.add(rows.getString(1));
                    productIds.add(rows.getString(2));
                }
            }
        }
        List<String> ordered = new ArrayList<>(productIds);
        Collections.sort(ordered);
        for (String productId : ordered) {
            lockProductUnit(connection, productId);
        }
    }

    private void assertActivationGraphAcyclic(Connection connection, RecipeHeader candidate)
            throws SQLException, BasicException {
        Map<String, Set<String>> graph = new HashMap<>();
        String sql = "SELECT rv.product_id, rc.component_product_id "
                + "FROM recipe_versions rv JOIN recipe_components rc ON rc.recipe_version_id = rv.id "
                + "WHERE (UPPER(rv.status) = ? AND rv.product_id <> ?) OR rv.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, STATUS_ACTIVE);
            statement.setString(2, candidate.productId);
            statement.setString(3, candidate.id);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String result = rows.getString(1);
                    String component = rows.getString(2);
                    graph.computeIfAbsent(result, key -> new HashSet<>()).add(component);
                }
            }
        }
        assertAcyclic(graph);
    }

    static void assertAcyclic(Map<String, Set<String>> graph) throws BasicException {
        Map<String, Integer> state = new HashMap<>();
        Map<String, Integer> maxDepth = new HashMap<>();
        for (String node : graph.keySet()) {
            if (visit(node, graph, state, maxDepth, 0) > MAX_GRAPH_DEPTH) {
                throw new BasicException("Recipe graph exceeds safe depth.");
            }
        }
    }

    private static int visit(String node, Map<String, Set<String>> graph,
            Map<String, Integer> state, Map<String, Integer> maxDepth,
            int recursionDepth) throws BasicException {
        if (recursionDepth > MAX_GRAPH_DEPTH) {
            throw new BasicException("Recipe graph exceeds safe depth.");
        }
        Integer cachedDepth = maxDepth.get(node);
        if (cachedDepth != null) {
            return cachedDepth;
        }
        if (Integer.valueOf(1).equals(state.get(node))) {
            throw new BasicException("La receta contiene un ciclo entre productos y no puede activarse.");
        }

        state.put(node, 1);
        int deepest = 0;
        Set<String> children = graph.get(node);
        if (children != null) {
            for (String child : children) {
                if (node.equals(child)) {
                    throw new BasicException("Recipe cannot contain its own result product.");
                }
                int childDepth = 1;
                if (graph.containsKey(child)) {
                    childDepth += visit(child, graph, state, maxDepth, recursionDepth + 1);
                }
                deepest = Math.max(deepest, childDepth);
                if (deepest > MAX_GRAPH_DEPTH) {
                    throw new BasicException("Recipe graph exceeds safe depth.");
                }
            }
        }
        state.put(node, 2);
        maxDepth.put(node, deepest);
        return deepest;
    }

    private static RecipeHeader loadRecipeForUpdate(Connection connection, String id)
            throws SQLException, BasicException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, product_id, status, recipe_mode FROM recipe_versions WHERE id = ? FOR UPDATE")) {
            statement.setString(1, id);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new BasicException("Unknown recipe version.");
                }
                return new RecipeHeader(row.getString(1), row.getString(2), row.getString(3).toUpperCase(), row.getString(4).toUpperCase());
            }
        }
    }

    private static void ensureRecipeHasComponents(Connection connection, String id)
            throws SQLException, BasicException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM recipe_components WHERE recipe_version_id = ?")) {
            statement.setString(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new BasicException("Recipe cannot be activated without components.");
                }
            }
        }
    }

    private static ProductUnit lockProductUnit(Connection connection, String productId)
            throws SQLException, BasicException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT uom, attributeset_id FROM products WHERE id = ? FOR UPDATE")) {
            statement.setString(1, productId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new BasicException("No se encontró el producto indicado en la receta.");
                }
                String uom = row.getString(1);
                if (isBlank(uom)) {
                    throw new BasicException("El producto " + productLabel(connection, productId)
                            + " no tiene una unidad de inventario configurada.");
                }
                return new ProductUnit(uom, row.getString(2));
            }
        }
    }

    private static void requireConvertible(Connection connection, String productId,
            String fromUom, String inventoryUom) throws SQLException, BasicException {
        if (fromUom.equals(inventoryUom)) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT factor FROM product_uom_conversions "
                + "WHERE product_id = ? AND from_uom_id = ? AND to_uom_id = ? AND active = ?")) {
            statement.setString(1, productId);
            statement.setString(2, fromUom);
            statement.setString(3, inventoryUom);
            statement.setBoolean(4, true);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || row.getBigDecimal(1) == null
                        || row.getBigDecimal(1).signum() <= 0) {
                    throw new BasicException("Falta configurar la conversión de unidad para "
                            + productLabel(connection, productId) + ": 1 "
                            + uomLabel(connection, fromUom) + " = ? "
                            + uomLabel(connection, inventoryUom)
                            + ". Configure el factor en Inventario > Conversiones UOM.");
                }
            }
        }
    }

    private static String productLabel(Connection connection, String productId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT reference, name FROM products WHERE id = ?")) {
            statement.setString(1, productId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return "producto no identificado";
                }
                String reference = row.getString(1);
                String name = row.getString(2);
                String ref = reference == null ? "" : reference.trim();
                String displayName = name == null ? "" : name.trim();
                if (ref.isEmpty()) {
                    return displayName.isEmpty() ? "producto sin nombre" : displayName;
                }
                return displayName.isEmpty() ? ref : ref + " - " + displayName;
            }
        }
    }

    private static String uomLabel(Connection connection, String uomId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM uom WHERE id = ?")) {
            statement.setString(1, uomId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return "unidad no identificada";
                }
                String name = row.getString(1);
                return isBlank(name) ? "unidad sin nombre" : name.trim();
            }
        }
    }

    private static void validateDraft(RecipeDraft draft) throws BasicException {
        if (draft == null || isBlank(draft.id) || isBlank(draft.productId)
                || draft.versionNo <= 0 || draft.yieldQuantity == null
                || draft.yieldQuantity.signum() <= 0 || isBlank(draft.yieldUomId)
                || isBlank(draft.createdBy) || draft.components == null
                || draft.components.isEmpty()
                || (!MODE_PRODUCTION.equals(draft.recipeMode)
                && !MODE_SALE_CONSUMPTION.equals(draft.recipeMode))) {
            throw new BasicException("Invalid recipe draft.");
        }
        Set<String> components = new HashSet<>();
        for (ComponentDraft component : draft.components) {
            if (component == null || isBlank(component.productId) || isBlank(component.uomId)
                    || component.quantity == null || component.quantity.signum() <= 0
                    || (component.wasteAllowance != null
                    && (component.wasteAllowance.signum() < 0
                    || component.wasteAllowance.compareTo(BigDecimal.ONE) >= 0))) {
                throw new BasicException("Invalid recipe component.");
            }
            if (draft.productId.equals(component.productId)) {
                throw new BasicException("Recipe cannot directly contain its own result product.");
            }
            if (!components.add(component.productId)) {
                throw new BasicException("La receta no puede repetir el mismo producto componente.");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class RecipeDraft {
        public final String id;
        public final String productId;
        public final int versionNo;
        public final BigDecimal yieldQuantity;
        public final String yieldUomId;
        public final String createdBy;
        public final String notes;
        public final String recipeMode;
        public final List<ComponentDraft> components;

        public RecipeDraft(String id, String productId, int versionNo,
                BigDecimal yieldQuantity, String yieldUomId, String createdBy,
                String notes, List<ComponentDraft> components) {
            this(id, productId, versionNo, yieldQuantity, yieldUomId, createdBy,
                    notes, components, MODE_PRODUCTION);
        }

        public RecipeDraft(String id, String productId, int versionNo,
                BigDecimal yieldQuantity, String yieldUomId, String createdBy,
                String notes, List<ComponentDraft> components, String recipeMode) {
            this.id = id;
            this.productId = productId;
            this.versionNo = versionNo;
            this.yieldQuantity = yieldQuantity;
            this.yieldUomId = yieldUomId;
            this.createdBy = createdBy;
            this.notes = notes;
            this.recipeMode = recipeMode == null ? MODE_PRODUCTION : recipeMode.trim().toUpperCase(java.util.Locale.ROOT);
            this.components = components == null ? null : new ArrayList<>(components);
        }
    }

    public static final class ComponentDraft {
        public final String productId;
        public final BigDecimal quantity;
        public final String uomId;
        public final BigDecimal wasteAllowance;

        public ComponentDraft(String productId, BigDecimal quantity,
                String uomId, BigDecimal wasteAllowance) {
            this.productId = productId;
            this.quantity = quantity;
            this.uomId = uomId;
            this.wasteAllowance = wasteAllowance;
        }
    }

    private static final class RecipeHeader {
        final String id;
        final String productId;
        final String status;
        final String recipeMode;

        RecipeHeader(String id, String productId, String status, String recipeMode) {
            this.id = id;
            this.productId = productId;
            this.status = status;
            this.recipeMode = recipeMode;
        }
    }

    private static final class ProductUnit {
        final String uomId;
        final String attributeSetId;

        ProductUnit(String uomId, String attributeSetId) {
            this.uomId = uomId;
            this.attributeSetId = attributeSetId;
        }
    }
}
