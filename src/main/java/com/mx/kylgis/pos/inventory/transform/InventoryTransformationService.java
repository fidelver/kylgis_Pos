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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import java.util.UUID;

/**
 * Transactional inventory production/transform core.
 *
 * This service intentionally does not call DataLogicSales.adjustStock(): recipe
 * components are physical SKUs. A semi-finished component therefore consumes
 * its own previously-produced stock and never re-expands raw ingredients.
 */
public final class InventoryTransformationService {

    private static final String TYPE_PRODUCTION = "PRODUCTION";
    private static final String TYPE_CONVERSION = "CONVERSION";
    private static final String TYPE_WASTE = "WASTE";
    private static final String TYPE_REVERSAL = "REVERSAL";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_POSTED = "POSTED";
    private static final String DIR_INPUT = "INPUT";
    private static final String DIR_OUTPUT = "OUTPUT";
    private static final String DIR_WASTE = "WASTE";
    private static final int REASON_IN_PRODUCTION = 6;
    private static final int REASON_IN_CONVERSION = 7;
    private static final int REASON_IN_REVERSAL = 8;
    private static final int REASON_OUT_PRODUCTION = -10;
    private static final int REASON_OUT_CONVERSION = -11;
    private static final int REASON_OUT_WASTE = -12;
    private static final int REASON_OUT_REVERSAL = -13;
    private static final int QUANTITY_SCALE = 6;

    private final Session session;

    public InventoryTransformationService(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        this.session = session;
    }

    public ProductionResult postProduction(final ProductionRequest request) throws BasicException {
        validateRequest(request);
        return new Transaction<ProductionResult>(session) {
            @Override
            protected ProductionResult transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    String requestHash = hashRequest(request);
                    ProductionResult existing = findExisting(connection, request.idempotencyKey, requestHash);
                    if (existing != null) {
                        return existing;
                    }

                    Recipe recipe = loadActiveRecipeForUpdate(connection, request.recipeVersionId);
                    existing = findExisting(connection, request.idempotencyKey, requestHash);
                    if (existing != null) {
                        return existing;
                    }
                    List<RecipeComponent> components = loadRecipeComponents(connection, recipe.id);
                    if (components.isEmpty()) {
                        throw new BasicException("Active recipe has no components.");
                    }
                    assertActiveRecipeGraphAcyclic(connection);

                    Set<String> productIds = new HashSet<>();
                    productIds.add(recipe.productId);
                    for (RecipeComponent component : components) {
                        productIds.add(component.productId);
                    }
                    Map<String, ProductState> products = lockProducts(connection, productIds);

                    ProductState outputProduct = products.get(recipe.productId);
                    requireNoAttributeSet(outputProduct);
                    BigDecimal expectedOutput = quantity(recipe.yieldQuantity.multiply(request.batches));
                    BigDecimal actualOutput = quantity(request.actualOutput);
                    BigDecimal outputBase = convertToInventoryUom(connection, recipe.productId,
                            actualOutput, recipe.yieldUomId, outputProduct.inventoryUomId);
                    if (outputBase.signum() <= 0) {
                        throw new BasicException("Production output rounds to zero inventory units.");
                    }

                    List<PreparedLine> inputLines = new ArrayList<>();
                    BigDecimal totalInputCost = BigDecimal.ZERO;
                    int lineNo = 1;
                    for (RecipeComponent component : components) {
                        ProductState product = products.get(component.productId);
                        requireNoAttributeSet(product);
                        BigDecimal recipeQuantity = quantity(component.quantity.multiply(request.batches));
                        BigDecimal baseQuantity = convertToInventoryUom(connection, component.productId,
                                recipeQuantity, component.uomId, product.inventoryUomId);
                        if (baseQuantity.signum() <= 0) {
                            throw new BasicException("Recipe component rounds to zero inventory units: "
                                    + component.productId);
                        }
                        BigDecimal available = lockStock(connection, request.locationId, component.productId);
                        if (available.compareTo(baseQuantity) < 0) {
                            throw new BasicException("Insufficient stock for recipe component "
                                    + component.productId + ": required=" + baseQuantity
                                    + " available=" + available);
                        }
                        BigDecimal lineCost = money(product.unitCost.multiply(baseQuantity));
                        totalInputCost = money(totalInputCost.add(lineCost));
                        inputLines.add(new PreparedLine(lineNo++, DIR_INPUT, component.productId,
                                recipeQuantity, component.uomId, baseQuantity,
                                money(product.unitCost), lineCost));
                    }

                    // Product-row locking serializes output creation with another transformation.
                    BigDecimal existingOutput = lockStock(connection, request.locationId, recipe.productId);
                    BigDecimal outputUnitCost = money(totalInputCost.divide(outputBase,
                            QUANTITY_SCALE, RoundingMode.HALF_UP));
                    BigDecimal weightedStockCost = weightedCostAfterAdd(existingOutput,
                            outputProduct.unitCost, outputBase, totalInputCost);
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    String transformationId = UUID.randomUUID().toString();
                    insertHeader(connection, transformationId, request, requestHash,
                            expectedOutput, actualOutput, timestamp);

                    for (PreparedLine line : inputLines) {
                        applyStockDelta(connection, request.locationId, line.productId,
                                line.baseQuantity.negate());
                        String diaryId = insertStockDiary(connection, timestamp,
                                REASON_OUT_PRODUCTION, request.locationId, line.productId,
                                line.baseQuantity.negate(), line.unitCost, request.createdBy);
                        insertTransformationLine(connection, transformationId, line,
                                diaryId);
                    }

                    applyStockDelta(connection, request.locationId, recipe.productId, outputBase);
                    updateStockCost(connection, recipe.productId, weightedStockCost);
                    String outputDiaryId = insertStockDiary(connection, timestamp,
                            REASON_IN_PRODUCTION, request.locationId, recipe.productId,
                            outputBase, outputUnitCost, request.createdBy);
                    PreparedLine outputLine = new PreparedLine(lineNo, DIR_OUTPUT,
                            recipe.productId, actualOutput, recipe.yieldUomId, outputBase,
                            outputUnitCost, totalInputCost);
                    insertTransformationLine(connection, transformationId, outputLine, outputDiaryId);

                    try (PreparedStatement posted = connection.prepareStatement(
                            "UPDATE inventory_transformations SET status = ? WHERE id = ? AND status = ?")) {
                        posted.setString(1, STATUS_POSTED);
                        posted.setString(2, transformationId);
                        posted.setString(3, STATUS_PENDING);
                        if (posted.executeUpdate() != 1) {
                            throw new SQLException("Transformation did not transition PENDING -> POSTED.");
                        }
                    }
                    return new ProductionResult(transformationId, recipe.productId,
                            expectedOutput, actualOutput, outputBase, outputUnitCost, false);
                } catch (SQLException ex) {
                    throw new BasicException("Unable to post inventory production.", ex);
                }
            }
        }.execute();
    }

    /**
     * Posts a non-sale SKU conversion. The first prototype intentionally
     * supports one output SKU so all consumed cost has an unambiguous target.
     * Multi-output transformations require an explicit allocation policy and
     * are rejected instead of guessing.
     */
    public ConversionResult postConversion(final ConversionRequest request) throws BasicException {
        validateConversionRequest(request);
        return new Transaction<ConversionResult>(session) {
            @Override
            protected ConversionResult transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    String requestHash = hashConversionRequest(request);
                    ConversionResult existing = findExistingConversion(connection,
                            request.idempotencyKey, requestHash);
                    if (existing != null) {
                        return existing;
                    }

                    Set<String> productIds = new HashSet<>();
                    for (ConversionInput input : request.inputs) {
                        productIds.add(input.productId);
                    }
                    productIds.add(request.output.productId);
                    Map<String, ProductState> products = lockProducts(connection, productIds);
                    existing = findExistingConversion(connection, request.idempotencyKey, requestHash);
                    if (existing != null) {
                        return existing;
                    }

                    List<PreparedLine> inputLines = new ArrayList<>();
                    BigDecimal totalInputCost = BigDecimal.ZERO;
                    int lineNo = 1;
                    for (ConversionInput input : request.inputs) {
                        ProductState product = products.get(input.productId);
                        requireNoAttributeSet(product);
                        BigDecimal requested = quantity(input.quantity);
                        BigDecimal base = convertToInventoryUom(connection, input.productId,
                                requested, input.uomId, product.inventoryUomId);
                        if (base.signum() <= 0) {
                            throw new BasicException("Conversion input rounds to zero inventory units: "
                                    + input.productId);
                        }
                        BigDecimal available = lockStock(connection, request.locationId, input.productId);
                        if (available.compareTo(base) < 0) {
                            throw new BasicException("Insufficient stock for conversion input "
                                    + input.productId + ": required=" + base + " available=" + available);
                        }
                        BigDecimal lineCost = money(product.unitCost.multiply(base));
                        totalInputCost = money(totalInputCost.add(lineCost));
                        inputLines.add(new PreparedLine(lineNo++, DIR_INPUT, input.productId,
                                requested, input.uomId, base, money(product.unitCost), lineCost));
                    }

                    ProductState outputProduct = products.get(request.output.productId);
                    requireNoAttributeSet(outputProduct);
                    BigDecimal requestedOutput = quantity(request.output.quantity);
                    BigDecimal outputBase = convertToInventoryUom(connection, request.output.productId,
                            requestedOutput, request.output.uomId, outputProduct.inventoryUomId);
                    if (outputBase.signum() <= 0) {
                        throw new BasicException("Conversion output rounds to zero inventory units.");
                    }
                    BigDecimal existingOutput = lockStock(connection, request.locationId,
                            request.output.productId);
                    BigDecimal outputUnitCost = money(totalInputCost.divide(outputBase,
                            QUANTITY_SCALE, RoundingMode.HALF_UP));
                    BigDecimal weightedStockCost = weightedCostAfterAdd(existingOutput,
                            outputProduct.unitCost, outputBase, totalInputCost);
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    String transformationId = UUID.randomUUID().toString();
                    insertTransformationHeader(connection, transformationId,
                            request.idempotencyKey, requestHash, TYPE_CONVERSION,
                            request.locationId, null, null, null, null,
                            timestamp, request.createdBy, request.notes);

                    for (PreparedLine line : inputLines) {
                        applyStockDelta(connection, request.locationId, line.productId,
                                line.baseQuantity.negate());
                        String diaryId = insertStockDiary(connection, timestamp,
                                REASON_OUT_CONVERSION, request.locationId, line.productId,
                                line.baseQuantity.negate(), line.unitCost, request.createdBy);
                        insertTransformationLine(connection, transformationId, line, diaryId);
                    }

                    applyStockDelta(connection, request.locationId, request.output.productId, outputBase);
                    updateStockCost(connection, request.output.productId, weightedStockCost);
                    String outputDiaryId = insertStockDiary(connection, timestamp,
                            REASON_IN_CONVERSION, request.locationId, request.output.productId,
                            outputBase, outputUnitCost, request.createdBy);
                    PreparedLine outputLine = new PreparedLine(lineNo, DIR_OUTPUT,
                            request.output.productId, requestedOutput, request.output.uomId,
                            outputBase, outputUnitCost, totalInputCost);
                    insertTransformationLine(connection, transformationId, outputLine, outputDiaryId);
                    markPosted(connection, transformationId);

                    return new ConversionResult(transformationId, request.output.productId,
                            outputBase, outputUnitCost, false);
                } catch (SQLException ex) {
                    throw new BasicException("Unable to post inventory conversion.", ex);
                }
            }
        }.execute();
    }

    /**
     * Posts explicitly measured physical waste. Waste has dedicated WASTE
     * lines: no artificial output SKU is created and the operation is never
     * confused with a sale or generic stock adjustment.
     */
    public WasteResult postWaste(final WasteRequest request) throws BasicException {
        validateWasteRequest(request);
        return new Transaction<WasteResult>(session) {
            @Override
            protected WasteResult transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    String requestHash = hashWasteRequest(request);
                    WasteResult existing = findExistingWaste(connection,
                            request.idempotencyKey, requestHash);
                    if (existing != null) {
                        return existing;
                    }

                    Set<String> productIds = new HashSet<>();
                    for (WasteInput input : request.inputs) {
                        productIds.add(input.productId);
                    }
                    Map<String, ProductState> products = lockProducts(connection, productIds);
                    existing = findExistingWaste(connection, request.idempotencyKey, requestHash);
                    if (existing != null) {
                        return existing;
                    }

                    List<PreparedLine> lines = new ArrayList<>();
                    BigDecimal totalWasteCost = BigDecimal.ZERO;
                    int lineNo = 1;
                    for (WasteInput input : request.inputs) {
                        ProductState product = products.get(input.productId);
                        requireNoAttributeSet(product);
                        BigDecimal requested = quantity(input.quantity);
                        BigDecimal base = convertToInventoryUom(connection, input.productId,
                                requested, input.uomId, product.inventoryUomId);
                        if (base.signum() <= 0) {
                            throw new BasicException("Waste input rounds to zero inventory units: "
                                    + input.productId);
                        }
                        BigDecimal available = lockStock(connection, request.locationId,
                                input.productId);
                        if (available.compareTo(base) < 0) {
                            throw new BasicException("Insufficient stock for measured waste "
                                    + input.productId + ": required=" + base
                                    + " available=" + available);
                        }
                        BigDecimal lineCost = money(product.unitCost.multiply(base));
                        totalWasteCost = money(totalWasteCost.add(lineCost));
                        lines.add(new PreparedLine(lineNo++, DIR_WASTE, input.productId,
                                requested, input.uomId, base, money(product.unitCost), lineCost));
                    }

                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    String transformationId = UUID.randomUUID().toString();
                    insertTransformationHeader(connection, transformationId,
                            request.idempotencyKey, requestHash, TYPE_WASTE,
                            request.locationId, null, null, null, null,
                            timestamp, request.createdBy, request.notes);

                    for (PreparedLine line : lines) {
                        applyStockDelta(connection, request.locationId, line.productId,
                                line.baseQuantity.negate());
                        String diaryId = insertStockDiary(connection, timestamp,
                                REASON_OUT_WASTE, request.locationId, line.productId,
                                line.baseQuantity.negate(), line.unitCost, request.createdBy);
                        insertTransformationLine(connection, transformationId, line, diaryId);
                    }
                    markPosted(connection, transformationId);
                    return new WasteResult(transformationId, totalWasteCost, false);
                } catch (SQLException ex) {
                    throw new BasicException("Unable to post measured inventory waste.", ex);
                }
            }
        }.execute();
    }

    /**
     * Reverses one previously-posted production/conversion by posting opposite
     * stock lines. Historical rows are never deleted or rewritten.
     */
    public ReversalResult reverseTransformation(final ReversalRequest request) throws BasicException {
        validateReversalRequest(request);
        return new Transaction<ReversalResult>(session) {
            @Override
            protected ReversalResult transact() throws BasicException {
                try {
                    Connection connection = session.getConnection();
                    OriginalTransformation original = loadOriginalForUpdate(connection,
                            request.originalTransformationId);
                    if (!STATUS_POSTED.equalsIgnoreCase(original.status)
                            || (!TYPE_PRODUCTION.equalsIgnoreCase(original.type)
                            && !TYPE_CONVERSION.equalsIgnoreCase(original.type))) {
                        throw new BasicException("Only POSTED production/conversion operations can be reversed.");
                    }
                    List<StoredLine> originalLines = loadStoredLines(connection, original.id);
                    if (originalLines.isEmpty()) {
                        throw new BasicException("Original transformation has no stock lines.");
                    }

                    Set<String> productIds = new HashSet<>();
                    for (StoredLine line : originalLines) {
                        if (!isBlank(line.attributeSetInstanceId)) {
                            throw new BasicException("Prototype reversal does not support attribute-set lines.");
                        }
                        if (!DIR_INPUT.equals(line.direction) && !DIR_OUTPUT.equals(line.direction)) {
                            throw new BasicException("Unsupported original line direction: " + line.direction);
                        }
                        if (!productIds.add(line.productId)) {
                            throw new BasicException("Prototype reversal requires one stock line per product: "
                                    + line.productId);
                        }
                    }
                    Map<String, ProductState> products = lockProducts(connection, productIds);
                    for (ProductState product : products.values()) {
                        requireNoAttributeSet(product);
                    }

                    String requestHash = hashReversalRequest(request);
                    ReversalResult replay = findExistingReversal(connection,
                            request.idempotencyKey, requestHash, original.id);
                    if (replay != null) {
                        return replay;
                    }
                    ensureNotAlreadyReversed(connection, original.id);

                    // Validate every stock removal and capture current quantities before the first mutation.
                    Map<String, BigDecimal> stockBefore = new HashMap<>();
                    for (StoredLine line : originalLines) {
                        BigDecimal available = lockStock(connection, original.locationId, line.productId);
                        stockBefore.put(line.productId, available);
                        if (DIR_OUTPUT.equals(line.direction)
                                && available.compareTo(line.baseQuantity) < 0) {
                            throw new BasicException("Cannot reverse transformation; output stock already consumed for product "
                                    + line.productId + ": required=" + line.baseQuantity
                                    + " available=" + available);
                        }
                    }

                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    String reversalId = UUID.randomUUID().toString();
                    insertTransformationHeader(connection, reversalId, request.idempotencyKey,
                            requestHash, TYPE_REVERSAL, original.locationId,
                            original.recipeVersionId, original.id, null, null,
                            timestamp, request.createdBy, request.notes);

                    int lineNo = 1;
                    for (StoredLine originalLine : originalLines) {
                        boolean removeOriginalOutput = DIR_OUTPUT.equals(originalLine.direction);
                        BigDecimal delta = removeOriginalOutput
                                ? originalLine.baseQuantity.negate() : originalLine.baseQuantity;
                        String reversalDirection = removeOriginalOutput ? DIR_INPUT : DIR_OUTPUT;
                        int reason = removeOriginalOutput ? REASON_OUT_REVERSAL : REASON_IN_REVERSAL;

                        ProductState currentProduct = products.get(originalLine.productId);
                        BigDecimal currentQuantity = stockBefore.get(originalLine.productId);
                        BigDecimal adjustedStockCost = removeOriginalOutput
                                ? weightedCostAfterRemove(currentQuantity, currentProduct.unitCost,
                                        originalLine.baseQuantity, originalLine.totalCost)
                                : weightedCostAfterAdd(currentQuantity, currentProduct.unitCost,
                                        originalLine.baseQuantity, originalLine.totalCost);

                        applyStockDelta(connection, original.locationId, originalLine.productId, delta);
                        if (adjustedStockCost != null) {
                            updateStockCost(connection, originalLine.productId, adjustedStockCost);
                        }
                        String diaryId = insertStockDiary(connection, timestamp, reason,
                                original.locationId, originalLine.productId, delta,
                                originalLine.unitCost, request.createdBy);
                        PreparedLine reversalLine = new PreparedLine(lineNo++, reversalDirection,
                                originalLine.productId, originalLine.quantity,
                                originalLine.uomId, originalLine.baseQuantity,
                                originalLine.unitCost, originalLine.totalCost);
                        insertTransformationLine(connection, reversalId, reversalLine, diaryId);
                    }
                    markPosted(connection, reversalId);
                    return new ReversalResult(reversalId, original.id, false);
                } catch (SQLException ex) {
                    throw new BasicException("Unable to reverse inventory transformation.", ex);
                }
            }
        }.execute();
    }

    private static Recipe loadActiveRecipeForUpdate(Connection connection, String recipeId)
            throws SQLException, BasicException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, product_id, yield_quantity, yield_uom_id "
                + "FROM recipe_versions WHERE id = ? AND UPPER(status) = ? FOR UPDATE")) {
            statement.setString(1, recipeId);
            statement.setString(2, STATUS_ACTIVE);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new BasicException("Unknown or inactive recipe version.");
                }
                return new Recipe(row.getString(1), row.getString(2),
                        row.getBigDecimal(3), row.getString(4));
            }
        }
    }

    private static List<RecipeComponent> loadRecipeComponents(Connection connection, String recipeId)
            throws SQLException {
        List<RecipeComponent> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT component_product_id, quantity, uom_id FROM recipe_components "
                + "WHERE recipe_version_id = ? ORDER BY line_no")) {
            statement.setString(1, recipeId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new RecipeComponent(rows.getString(1), rows.getBigDecimal(2), rows.getString(3)));
                }
            }
        }
        return result;
    }

    private static void assertActiveRecipeGraphAcyclic(Connection connection)
            throws SQLException, BasicException {
        Map<String, Set<String>> graph = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT rv.product_id, rc.component_product_id "
                + "FROM recipe_versions rv JOIN recipe_components rc ON rc.recipe_version_id = rv.id "
                + "WHERE UPPER(rv.status) = ?")) {
            statement.setString(1, STATUS_ACTIVE);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    graph.computeIfAbsent(rows.getString(1), key -> new HashSet<>()).add(rows.getString(2));
                }
            }
        }
        RecipeDefinitionService.assertAcyclic(graph);
    }

    private static Map<String, ProductState> lockProducts(Connection connection, Set<String> productIds)
            throws SQLException, BasicException {
        List<String> ordered = new ArrayList<>(productIds);
        Collections.sort(ordered);
        Map<String, ProductState> result = new HashMap<>();
        String sql = "SELECT uom, attributeset_id, stockcost, pricebuy FROM products WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String productId : ordered) {
                statement.setString(1, productId);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) {
                        throw new BasicException("Unknown product in transformation: " + productId);
                    }
                    String uom = row.getString(1);
                    if (isBlank(uom)) {
                        throw new BasicException("Product has no inventory UOM: " + productId);
                    }
                    BigDecimal stockCost = row.getBigDecimal(3);
                    BigDecimal priceBuy = row.getBigDecimal(4);
                    BigDecimal cost = stockCost != null && stockCost.signum() > 0
                            ? stockCost : (priceBuy == null ? BigDecimal.ZERO : priceBuy);
                    if (cost.signum() < 0) {
                        throw new BasicException("Product has invalid negative cost: " + productId);
                    }
                    result.put(productId, new ProductState(productId, uom,
                            row.getString(2), money(cost)));
                }
            }
        }
        return result;
    }

    private static void requireNoAttributeSet(ProductState product) throws BasicException {
        if (!isBlank(product.attributeSetId)) {
            throw new BasicException("Prototype transformations do not yet support attribute-set products: "
                    + product.productId);
        }
    }

    private static BigDecimal convertToInventoryUom(Connection connection, String productId,
            BigDecimal quantity, String fromUom, String inventoryUom)
            throws SQLException, BasicException {
        if (fromUom.equals(inventoryUom)) {
            return quantity(quantity);
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT factor FROM product_uom_conversions "
                + "WHERE product_id = ? AND from_uom_id = ? AND to_uom_id = ? AND active = ?")) {
            statement.setString(1, productId);
            statement.setString(2, fromUom);
            statement.setString(3, inventoryUom);
            statement.setBoolean(4, true);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new BasicException("Missing direct UOM conversion for product " + productId);
                }
                BigDecimal factor = row.getBigDecimal(1);
                if (factor == null || factor.signum() <= 0) {
                    throw new BasicException("Invalid UOM conversion factor for product " + productId);
                }
                return quantity(quantity.multiply(factor));
            }
        }
    }

    private static BigDecimal lockStock(Connection connection, String locationId, String productId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT units FROM stockcurrent WHERE location = ? AND product = ? "
                + "AND attributesetinstance_id IS NULL FOR UPDATE")) {
            statement.setString(1, locationId);
            statement.setString(2, productId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return BigDecimal.ZERO.setScale(QUANTITY_SCALE);
                }
                BigDecimal units = quantity(row.getBigDecimal(1));
                if (row.next()) {
                    throw new SQLException("Duplicate stockcurrent rows for product " + productId
                            + " at location " + locationId + " with NULL attribute instance.");
                }
                return units;
            }
        }
    }

    private static void lockStockIfPresent(Connection connection, String locationId, String productId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT units FROM stockcurrent WHERE location = ? AND product = ? "
                + "AND attributesetinstance_id IS NULL FOR UPDATE")) {
            statement.setString(1, locationId);
            statement.setString(2, productId);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next() && rows.next()) {
                    throw new SQLException("Duplicate stockcurrent rows for product " + productId
                            + " at location " + locationId + " with NULL attribute instance.");
                }
                // Lock only; the product row is also locked to serialize missing-row inserts.
            }
        }
    }

    private static void applyStockDelta(Connection connection, String locationId,
            String productId, BigDecimal delta) throws SQLException {
        int changed;
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE stockcurrent SET units = units + ? WHERE location = ? AND product = ? "
                + "AND attributesetinstance_id IS NULL")) {
            update.setBigDecimal(1, delta);
            update.setString(2, locationId);
            update.setString(3, productId);
            changed = update.executeUpdate();
        }
        if (changed == 0) {
            if (delta.signum() < 0) {
                throw new SQLException("Input stock row disappeared during locked transformation.");
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO stockcurrent (location, product, attributesetinstance_id, units) "
                    + "VALUES (?, ?, NULL, ?)")) {
                insert.setString(1, locationId);
                insert.setString(2, productId);
                insert.setBigDecimal(3, delta);
                if (insert.executeUpdate() != 1) {
                    throw new SQLException("Output stock row was not inserted exactly once.");
                }
            }
        } else if (changed != 1) {
            throw new SQLException("Unexpected stock row cardinality for product " + productId);
        }
    }

    private static String insertStockDiary(Connection connection, Timestamp timestamp, int reason,
            String locationId, String productId, BigDecimal units, BigDecimal unitCost,
            String user) throws SQLException {
        String id = UUID.randomUUID().toString();
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO stockdiary (id, datenew, reason, location, product, "
                + "attributesetinstance_id, units, price, appuser, supplier, supplierdoc) "
                + "VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, NULL, NULL)")) {
            insert.setString(1, id);
            insert.setTimestamp(2, timestamp);
            insert.setInt(3, reason);
            insert.setString(4, locationId);
            insert.setString(5, productId);
            insert.setBigDecimal(6, units);
            insert.setBigDecimal(7, unitCost);
            insert.setString(8, user);
            if (insert.executeUpdate() != 1) {
                throw new SQLException("Stock diary line was not inserted exactly once.");
            }
        }
        return id;
    }

    private static void insertHeader(Connection connection, String id, ProductionRequest request,
            String requestHash, BigDecimal expectedOutput, BigDecimal actualOutput,
            Timestamp timestamp) throws SQLException {
        insertTransformationHeader(connection, id, request.idempotencyKey, requestHash,
                TYPE_PRODUCTION, request.locationId, request.recipeVersionId, null,
                expectedOutput, actualOutput, timestamp, request.createdBy, request.notes);
    }

    private static void insertTransformationHeader(Connection connection, String id,
            String idempotencyKey, String requestHash, String type, String locationId,
            String recipeVersionId, String reversesId, BigDecimal expectedOutput,
            BigDecimal actualOutput, Timestamp timestamp, String createdBy, String notes)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO inventory_transformations "
                + "(id, idempotency_key, request_hash, type, status, location_id, recipe_version_id, "
                + "reverses_id, expected_output, actual_output, created_at, created_by, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, id);
            insert.setString(2, idempotencyKey);
            insert.setString(3, requestHash);
            insert.setString(4, type);
            insert.setString(5, STATUS_PENDING);
            insert.setString(6, locationId);
            if (recipeVersionId == null) {
                insert.setNull(7, java.sql.Types.VARCHAR);
            } else {
                insert.setString(7, recipeVersionId);
            }
            if (reversesId == null) {
                insert.setNull(8, java.sql.Types.VARCHAR);
            } else {
                insert.setString(8, reversesId);
            }
            if (expectedOutput == null) {
                insert.setNull(9, java.sql.Types.DECIMAL);
            } else {
                insert.setBigDecimal(9, expectedOutput);
            }
            if (actualOutput == null) {
                insert.setNull(10, java.sql.Types.DECIMAL);
            } else {
                insert.setBigDecimal(10, actualOutput);
            }
            insert.setTimestamp(11, timestamp);
            insert.setString(12, createdBy);
            insert.setString(13, notes);
            if (insert.executeUpdate() != 1) {
                throw new SQLException("Transformation header was not inserted exactly once.");
            }
        }
    }

    private static void insertTransformationLine(Connection connection, String transformationId,
            PreparedLine line, String stockDiaryId) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO inventory_transformation_lines "
                + "(transformation_id, line_no, direction, product_id, attributesetinstance_id, "
                + "quantity, uom_id, base_quantity, unit_cost, total_cost, stockdiary_id) "
                + "VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, transformationId);
            insert.setInt(2, line.lineNo);
            insert.setString(3, line.direction);
            insert.setString(4, line.productId);
            insert.setBigDecimal(5, line.quantity);
            insert.setString(6, line.uomId);
            insert.setBigDecimal(7, line.baseQuantity);
            insert.setBigDecimal(8, line.unitCost);
            insert.setBigDecimal(9, line.totalCost);
            insert.setString(10, stockDiaryId);
            if (insert.executeUpdate() != 1) {
                throw new SQLException("Transformation line was not inserted exactly once.");
            }
        }
    }

    private static void updateStockCost(Connection connection, String productId,
            BigDecimal outputUnitCost) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE products SET stockcost = ? WHERE id = ?")) {
            update.setBigDecimal(1, outputUnitCost);
            update.setString(2, productId);
            if (update.executeUpdate() != 1) {
                throw new SQLException("Output product stock cost was not updated exactly once.");
            }
        }
    }

    private static ProductionResult findExisting(Connection connection, String idempotencyKey,
            String requestHash) throws SQLException, BasicException {
        String id;
        String hash;
        String status;
        String type;
        String recipeId;
        BigDecimal expected;
        BigDecimal actual;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, request_hash, status, type, recipe_version_id, expected_output, actual_output "
                + "FROM inventory_transformations WHERE idempotency_key = ? FOR UPDATE")) {
            statement.setString(1, idempotencyKey);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return null;
                }
                id = row.getString(1);
                hash = row.getString(2);
                status = row.getString(3);
                type = row.getString(4);
                recipeId = row.getString(5);
                expected = row.getBigDecimal(6);
                actual = row.getBigDecimal(7);
            }
        }
        if (!requestHash.equals(hash)) {
            throw new BasicException("Idempotency key is already bound to a different production request.");
        }
        if (!TYPE_PRODUCTION.equalsIgnoreCase(type) || !STATUS_POSTED.equalsIgnoreCase(status)) {
            throw new BasicException("Existing idempotent request is not a POSTED production.");
        }
        String outputProduct = null;
        BigDecimal outputBase = null;
        BigDecimal unitCost = null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT product_id, base_quantity, unit_cost FROM inventory_transformation_lines "
                + "WHERE transformation_id = ? AND direction = ? ORDER BY line_no")) {
            statement.setString(1, id);
            statement.setString(2, DIR_OUTPUT);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) {
                    outputProduct = row.getString(1);
                    outputBase = row.getBigDecimal(2);
                    unitCost = row.getBigDecimal(3);
                }
            }
        }
        if (outputProduct == null || outputBase == null || unitCost == null || isBlank(recipeId)) {
            throw new BasicException("Stored idempotent production is incomplete.");
        }
        return new ProductionResult(id, outputProduct, expected, actual,
                outputBase, unitCost, true);
    }

    private static OriginalTransformation loadOriginalForUpdate(Connection connection, String id)
            throws SQLException, BasicException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, type, status, location_id, recipe_version_id "
                + "FROM inventory_transformations WHERE id = ? FOR UPDATE")) {
            statement.setString(1, id);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new BasicException("Unknown transformation to reverse.");
                }
                return new OriginalTransformation(row.getString(1), row.getString(2),
                        row.getString(3), row.getString(4), row.getString(5));
            }
        }
    }

    private static List<StoredLine> loadStoredLines(Connection connection, String transformationId)
            throws SQLException {
        List<StoredLine> lines = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT direction, product_id, attributesetinstance_id, quantity, uom_id, "
                + "base_quantity, unit_cost, total_cost FROM inventory_transformation_lines "
                + "WHERE transformation_id = ? ORDER BY line_no FOR UPDATE")) {
            statement.setString(1, transformationId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    lines.add(new StoredLine(rows.getString(1), rows.getString(2),
                            rows.getString(3), rows.getBigDecimal(4), rows.getString(5),
                            rows.getBigDecimal(6), rows.getBigDecimal(7), rows.getBigDecimal(8)));
                }
            }
        }
        return lines;
    }

    private static ReversalResult findExistingReversal(Connection connection,
            String idempotencyKey, String requestHash, String originalId)
            throws SQLException, BasicException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, request_hash, type, status, reverses_id FROM inventory_transformations "
                + "WHERE idempotency_key = ? FOR UPDATE")) {
            statement.setString(1, idempotencyKey);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return null;
                }
                String id = row.getString(1);
                if (!requestHash.equals(row.getString(2))
                        || !TYPE_REVERSAL.equalsIgnoreCase(row.getString(3))
                        || !STATUS_POSTED.equalsIgnoreCase(row.getString(4))
                        || !originalId.equals(row.getString(5))) {
                    throw new BasicException("Idempotency key is already bound to another transformation.");
                }
                return new ReversalResult(id, originalId, true);
            }
        }
    }

    private static void ensureNotAlreadyReversed(Connection connection, String originalId)
            throws SQLException, BasicException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM inventory_transformations WHERE reverses_id = ? FOR UPDATE")) {
            statement.setString(1, originalId);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    throw new BasicException("Transformation has already been reversed by " + rows.getString(1));
                }
            }
        }
    }

    private static String hashReversalRequest(ReversalRequest request) throws BasicException {
        return sha256(request.originalTransformationId + "\n"
                + request.createdBy + "\n"
                + (request.notes == null ? "" : request.notes));
    }

    private static void validateReversalRequest(ReversalRequest request) throws BasicException {
        if (request == null || isBlank(request.idempotencyKey) || request.idempotencyKey.length() > 255
                || isBlank(request.originalTransformationId) || isBlank(request.createdBy)
                || (request.notes != null && request.notes.length() > 1000)) {
            throw new BasicException("Invalid reversal request.");
        }
    }

    private static WasteResult findExistingWaste(Connection connection,
            String idempotencyKey, String requestHash) throws SQLException, BasicException {
        String id;
        String hash;
        String status;
        String type;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, request_hash, status, type FROM inventory_transformations "
                + "WHERE idempotency_key = ? FOR UPDATE")) {
            statement.setString(1, idempotencyKey);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return null;
                }
                id = row.getString(1);
                hash = row.getString(2);
                status = row.getString(3);
                type = row.getString(4);
            }
        }
        if (!requestHash.equals(hash)) {
            throw new BasicException("Idempotency key is already bound to a different waste request.");
        }
        if (!TYPE_WASTE.equalsIgnoreCase(type) || !STATUS_POSTED.equalsIgnoreCase(status)) {
            throw new BasicException("Existing idempotent waste request is not POSTED waste.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*), COALESCE(SUM(total_cost), 0) "
                + "FROM inventory_transformation_lines WHERE transformation_id = ? AND direction = ?")) {
            statement.setString(1, id);
            statement.setString(2, DIR_WASTE);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || row.getInt(1) <= 0) {
                    throw new BasicException("Stored idempotent waste operation has no input lines.");
                }
                return new WasteResult(id, money(row.getBigDecimal(2)), true);
            }
        }
    }

    private static ConversionResult findExistingConversion(Connection connection,
            String idempotencyKey, String requestHash) throws SQLException, BasicException {
        String id;
        String hash;
        String status;
        String type;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, request_hash, status, type FROM inventory_transformations "
                + "WHERE idempotency_key = ? FOR UPDATE")) {
            statement.setString(1, idempotencyKey);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return null;
                }
                id = row.getString(1);
                hash = row.getString(2);
                status = row.getString(3);
                type = row.getString(4);
            }
        }
        if (!requestHash.equals(hash)) {
            throw new BasicException("Idempotency key is already bound to a different conversion request.");
        }
        if (!TYPE_CONVERSION.equalsIgnoreCase(type) || !STATUS_POSTED.equalsIgnoreCase(status)) {
            throw new BasicException("Existing idempotent conversion is not a POSTED conversion.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT product_id, base_quantity, unit_cost FROM inventory_transformation_lines "
                + "WHERE transformation_id = ? AND direction = ? ORDER BY line_no")) {
            statement.setString(1, id);
            statement.setString(2, DIR_OUTPUT);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new BasicException("Stored idempotent conversion has no output line.");
                }
                return new ConversionResult(id, row.getString(1), row.getBigDecimal(2),
                        row.getBigDecimal(3), true);
            }
        }
    }

    private static void markPosted(Connection connection, String transformationId)
            throws SQLException {
        try (PreparedStatement posted = connection.prepareStatement(
                "UPDATE inventory_transformations SET status = ? WHERE id = ? AND status = ?")) {
            posted.setString(1, STATUS_POSTED);
            posted.setString(2, transformationId);
            posted.setString(3, STATUS_PENDING);
            if (posted.executeUpdate() != 1) {
                throw new SQLException("Transformation did not transition PENDING -> POSTED.");
            }
        }
    }

    static String hashWasteRequest(WasteRequest request) throws BasicException {
        List<WasteInput> inputs = new ArrayList<>(request.inputs);
        inputs.sort((left, right) -> {
            int product = left.productId.compareTo(right.productId);
            if (product != 0) {
                return product;
            }
            int uom = left.uomId.compareTo(right.uomId);
            if (uom != 0) {
                return uom;
            }
            return decimal(left.quantity).compareTo(decimal(right.quantity));
        });
        StringBuilder normalized = new StringBuilder();
        normalized.append(request.locationId).append('\n')
                .append(request.createdBy).append('\n')
                .append(request.notes == null ? "" : request.notes).append('\n');
        for (WasteInput input : inputs) {
            normalized.append("W|").append(input.productId).append('|')
                    .append(input.uomId).append('|').append(decimal(input.quantity)).append('\n');
        }
        return sha256(normalized.toString());
    }

    static void validateWasteRequest(WasteRequest request) throws BasicException {
        if (request == null || isBlank(request.idempotencyKey) || request.idempotencyKey.length() > 255
                || isBlank(request.locationId) || isBlank(request.createdBy)
                || request.inputs == null || request.inputs.isEmpty()
                || (request.notes != null && request.notes.length() > 1000)) {
            throw new BasicException("Invalid waste request.");
        }
        Set<String> products = new HashSet<>();
        for (WasteInput input : request.inputs) {
            if (input == null || isBlank(input.productId) || isBlank(input.uomId)
                    || input.quantity == null || input.quantity.signum() <= 0) {
                throw new BasicException("Invalid measured waste input.");
            }
            if (!products.add(input.productId)) {
                throw new BasicException("Duplicate measured waste product: " + input.productId);
            }
        }
    }

    private static String hashConversionRequest(ConversionRequest request) throws BasicException {
        List<ConversionInput> inputs = new ArrayList<>(request.inputs);
        inputs.sort((left, right) -> {
            int product = left.productId.compareTo(right.productId);
            if (product != 0) {
                return product;
            }
            int uom = left.uomId.compareTo(right.uomId);
            if (uom != 0) {
                return uom;
            }
            return decimal(left.quantity).compareTo(decimal(right.quantity));
        });
        StringBuilder normalized = new StringBuilder();
        normalized.append(request.locationId).append('\n')
                .append(request.createdBy).append('\n')
                .append(request.notes == null ? "" : request.notes).append('\n');
        for (ConversionInput input : inputs) {
            normalized.append("I|").append(input.productId).append('|')
                    .append(input.uomId).append('|').append(decimal(input.quantity)).append('\n');
        }
        normalized.append("O|").append(request.output.productId).append('|')
                .append(request.output.uomId).append('|').append(decimal(request.output.quantity));
        return sha256(normalized.toString());
    }

    private static void validateConversionRequest(ConversionRequest request) throws BasicException {
        if (request == null || isBlank(request.idempotencyKey) || request.idempotencyKey.length() > 255
                || isBlank(request.locationId) || isBlank(request.createdBy)
                || request.inputs == null || request.inputs.isEmpty() || request.output == null
                || (request.notes != null && request.notes.length() > 1000)) {
            throw new BasicException("Invalid conversion request.");
        }
        Set<String> inputProducts = new HashSet<>();
        for (ConversionInput input : request.inputs) {
            if (input == null || isBlank(input.productId) || isBlank(input.uomId)
                    || input.quantity == null || input.quantity.signum() <= 0) {
                throw new BasicException("Invalid conversion input.");
            }
            if (!inputProducts.add(input.productId)) {
                throw new BasicException("Duplicate conversion input product: " + input.productId);
            }
        }
        if (isBlank(request.output.productId) || isBlank(request.output.uomId)
                || request.output.quantity == null || request.output.quantity.signum() <= 0) {
            throw new BasicException("Invalid conversion output.");
        }
        if (inputProducts.contains(request.output.productId)) {
            throw new BasicException("SKU conversion output must differ from every input SKU.");
        }
    }

    private static String sha256(String normalized) throws BasicException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                out.append(String.format("%02x", value & 0xff));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BasicException("SHA-256 is unavailable.", ex);
        }
    }

    private static String hashRequest(ProductionRequest request) throws BasicException {
        String normalized = request.recipeVersionId + "\n"
                + request.locationId + "\n"
                + decimal(request.batches) + "\n"
                + decimal(request.actualOutput) + "\n"
                + request.createdBy + "\n"
                + (request.notes == null ? "" : request.notes);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                out.append(String.format("%02x", value & 0xff));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BasicException("SHA-256 is unavailable.", ex);
        }
    }

    private static void validateRequest(ProductionRequest request) throws BasicException {
        if (request == null || isBlank(request.idempotencyKey) || request.idempotencyKey.length() > 255
                || isBlank(request.recipeVersionId) || isBlank(request.locationId)
                || isBlank(request.createdBy) || request.batches == null
                || request.batches.signum() <= 0 || request.actualOutput == null
                || request.actualOutput.signum() <= 0
                || (request.notes != null && request.notes.length() > 1000)) {
            throw new BasicException("Invalid production request.");
        }
    }

    static BigDecimal weightedCostAfterAdd(BigDecimal existingQuantity,
            BigDecimal existingUnitCost, BigDecimal addedQuantity,
            BigDecimal addedTotalCost) throws BasicException {
        if (existingQuantity == null || existingUnitCost == null || addedQuantity == null
                || addedTotalCost == null || existingQuantity.signum() < 0
                || existingUnitCost.signum() < 0 || addedQuantity.signum() <= 0
                || addedTotalCost.signum() < 0) {
            throw new BasicException("Invalid values for weighted inventory cost addition.");
        }
        BigDecimal totalQuantity = quantity(existingQuantity.add(addedQuantity));
        if (totalQuantity.signum() <= 0) {
            throw new BasicException("Weighted inventory quantity must remain positive.");
        }
        BigDecimal existingValue = money(existingQuantity.multiply(existingUnitCost));
        BigDecimal totalValue = money(existingValue.add(addedTotalCost));
        return money(totalValue.divide(totalQuantity, QUANTITY_SCALE, RoundingMode.HALF_UP));
    }

    static BigDecimal weightedCostAfterRemove(BigDecimal existingQuantity,
            BigDecimal existingUnitCost, BigDecimal removedQuantity,
            BigDecimal removedTotalCost) throws BasicException {
        if (existingQuantity == null || existingUnitCost == null || removedQuantity == null
                || removedTotalCost == null || existingQuantity.signum() < 0
                || existingUnitCost.signum() < 0 || removedQuantity.signum() <= 0
                || removedTotalCost.signum() < 0
                || existingQuantity.compareTo(removedQuantity) < 0) {
            throw new BasicException("Invalid values for weighted inventory cost removal.");
        }
        BigDecimal remainingQuantity = quantity(existingQuantity.subtract(removedQuantity));
        if (remainingQuantity.signum() == 0) {
            // With no stock remaining there is no meaningful average to restore.
            // Keep the current reference cost; the next addition ignores it because
            // existing quantity is zero.
            return null;
        }
        BigDecimal existingValue = money(existingQuantity.multiply(existingUnitCost));
        BigDecimal remainingValue = money(existingValue.subtract(removedTotalCost));
        if (remainingValue.signum() < 0) {
            throw new BasicException("Inventory valuation would become negative after reversal.");
        }
        return money(remainingValue.divide(remainingQuantity,
                QUANTITY_SCALE, RoundingMode.HALF_UP));
    }

    private static BigDecimal quantity(BigDecimal value) {
        return value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class WasteRequest {
        public final String idempotencyKey;
        public final String locationId;
        public final List<WasteInput> inputs;
        public final String createdBy;
        public final String notes;

        public WasteRequest(String idempotencyKey, String locationId,
                List<WasteInput> inputs, String createdBy, String notes) {
            this.idempotencyKey = idempotencyKey;
            this.locationId = locationId;
            this.inputs = inputs == null ? null : new ArrayList<>(inputs);
            this.createdBy = createdBy;
            this.notes = notes;
        }
    }

    public static final class WasteInput {
        public final String productId;
        public final BigDecimal quantity;
        public final String uomId;

        public WasteInput(String productId, BigDecimal quantity, String uomId) {
            this.productId = productId;
            this.quantity = quantity;
            this.uomId = uomId;
        }
    }

    public static final class WasteResult {
        public final String transformationId;
        public final BigDecimal totalCost;
        public final boolean idempotentReplay;

        WasteResult(String transformationId, BigDecimal totalCost,
                boolean idempotentReplay) {
            this.transformationId = transformationId;
            this.totalCost = totalCost;
            this.idempotentReplay = idempotentReplay;
        }
    }

    public static final class ReversalRequest {
        public final String idempotencyKey;
        public final String originalTransformationId;
        public final String createdBy;
        public final String notes;

        public ReversalRequest(String idempotencyKey, String originalTransformationId,
                String createdBy, String notes) {
            this.idempotencyKey = idempotencyKey;
            this.originalTransformationId = originalTransformationId;
            this.createdBy = createdBy;
            this.notes = notes;
        }
    }

    public static final class ReversalResult {
        public final String reversalTransformationId;
        public final String originalTransformationId;
        public final boolean idempotentReplay;

        ReversalResult(String reversalTransformationId, String originalTransformationId,
                boolean idempotentReplay) {
            this.reversalTransformationId = reversalTransformationId;
            this.originalTransformationId = originalTransformationId;
            this.idempotentReplay = idempotentReplay;
        }
    }

    public static final class ConversionRequest {
        public final String idempotencyKey;
        public final String locationId;
        public final List<ConversionInput> inputs;
        public final ConversionOutput output;
        public final String createdBy;
        public final String notes;

        public ConversionRequest(String idempotencyKey, String locationId,
                List<ConversionInput> inputs, ConversionOutput output,
                String createdBy, String notes) {
            this.idempotencyKey = idempotencyKey;
            this.locationId = locationId;
            this.inputs = inputs == null ? null : new ArrayList<>(inputs);
            this.output = output;
            this.createdBy = createdBy;
            this.notes = notes;
        }
    }

    public static final class ConversionInput {
        public final String productId;
        public final BigDecimal quantity;
        public final String uomId;

        public ConversionInput(String productId, BigDecimal quantity, String uomId) {
            this.productId = productId;
            this.quantity = quantity;
            this.uomId = uomId;
        }
    }

    public static final class ConversionOutput {
        public final String productId;
        public final BigDecimal quantity;
        public final String uomId;

        public ConversionOutput(String productId, BigDecimal quantity, String uomId) {
            this.productId = productId;
            this.quantity = quantity;
            this.uomId = uomId;
        }
    }

    public static final class ConversionResult {
        public final String transformationId;
        public final String outputProductId;
        public final BigDecimal outputBaseQuantity;
        public final BigDecimal outputUnitCost;
        public final boolean idempotentReplay;

        ConversionResult(String transformationId, String outputProductId,
                BigDecimal outputBaseQuantity, BigDecimal outputUnitCost,
                boolean idempotentReplay) {
            this.transformationId = transformationId;
            this.outputProductId = outputProductId;
            this.outputBaseQuantity = outputBaseQuantity;
            this.outputUnitCost = outputUnitCost;
            this.idempotentReplay = idempotentReplay;
        }
    }

    public static final class ProductionRequest {
        public final String idempotencyKey;
        public final String recipeVersionId;
        public final String locationId;
        public final BigDecimal batches;
        public final BigDecimal actualOutput;
        public final String createdBy;
        public final String notes;

        public ProductionRequest(String idempotencyKey, String recipeVersionId,
                String locationId, BigDecimal batches, BigDecimal actualOutput,
                String createdBy, String notes) {
            this.idempotencyKey = idempotencyKey;
            this.recipeVersionId = recipeVersionId;
            this.locationId = locationId;
            this.batches = batches;
            this.actualOutput = actualOutput;
            this.createdBy = createdBy;
            this.notes = notes;
        }
    }

    public static final class ProductionResult {
        public final String transformationId;
        public final String outputProductId;
        public final BigDecimal expectedOutput;
        public final BigDecimal actualOutput;
        public final BigDecimal outputBaseQuantity;
        public final BigDecimal outputUnitCost;
        public final boolean idempotentReplay;

        ProductionResult(String transformationId, String outputProductId,
                BigDecimal expectedOutput, BigDecimal actualOutput,
                BigDecimal outputBaseQuantity, BigDecimal outputUnitCost,
                boolean idempotentReplay) {
            this.transformationId = transformationId;
            this.outputProductId = outputProductId;
            this.expectedOutput = expectedOutput;
            this.actualOutput = actualOutput;
            this.outputBaseQuantity = outputBaseQuantity;
            this.outputUnitCost = outputUnitCost;
            this.idempotentReplay = idempotentReplay;
        }
    }

    private static final class OriginalTransformation {
        final String id;
        final String type;
        final String status;
        final String locationId;
        final String recipeVersionId;

        OriginalTransformation(String id, String type, String status,
                String locationId, String recipeVersionId) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.locationId = locationId;
            this.recipeVersionId = recipeVersionId;
        }
    }

    private static final class StoredLine {
        final String direction;
        final String productId;
        final String attributeSetInstanceId;
        final BigDecimal quantity;
        final String uomId;
        final BigDecimal baseQuantity;
        final BigDecimal unitCost;
        final BigDecimal totalCost;

        StoredLine(String direction, String productId, String attributeSetInstanceId,
                BigDecimal quantity, String uomId, BigDecimal baseQuantity,
                BigDecimal unitCost, BigDecimal totalCost) {
            this.direction = direction;
            this.productId = productId;
            this.attributeSetInstanceId = attributeSetInstanceId;
            this.quantity = quantity;
            this.uomId = uomId;
            this.baseQuantity = baseQuantity;
            this.unitCost = unitCost;
            this.totalCost = totalCost;
        }
    }

    private static final class Recipe {
        final String id;
        final String productId;
        final BigDecimal yieldQuantity;
        final String yieldUomId;

        Recipe(String id, String productId, BigDecimal yieldQuantity, String yieldUomId) {
            this.id = id;
            this.productId = productId;
            this.yieldQuantity = yieldQuantity;
            this.yieldUomId = yieldUomId;
        }
    }

    private static final class RecipeComponent {
        final String productId;
        final BigDecimal quantity;
        final String uomId;

        RecipeComponent(String productId, BigDecimal quantity, String uomId) {
            this.productId = productId;
            this.quantity = quantity;
            this.uomId = uomId;
        }
    }

    private static final class ProductState {
        final String productId;
        final String inventoryUomId;
        final String attributeSetId;
        final BigDecimal unitCost;

        ProductState(String productId, String inventoryUomId,
                String attributeSetId, BigDecimal unitCost) {
            this.productId = productId;
            this.inventoryUomId = inventoryUomId;
            this.attributeSetId = attributeSetId;
            this.unitCost = unitCost;
        }
    }

    private static final class PreparedLine {
        final int lineNo;
        final String direction;
        final String productId;
        final BigDecimal quantity;
        final String uomId;
        final BigDecimal baseQuantity;
        final BigDecimal unitCost;
        final BigDecimal totalCost;

        PreparedLine(int lineNo, String direction, String productId,
                BigDecimal quantity, String uomId, BigDecimal baseQuantity,
                BigDecimal unitCost, BigDecimal totalCost) {
            this.lineNo = lineNo;
            this.direction = direction;
            this.productId = productId;
            this.quantity = quantity;
            this.uomId = uomId;
            this.baseQuantity = baseQuantity;
            this.unitCost = unitCost;
            this.totalCost = totalCost;
        }
    }
}
