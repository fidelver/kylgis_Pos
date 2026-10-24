package com.mx.kylgis.pos.inventory.transform;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.forms.DatabaseUpgradeService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dependency-free regression tests executed by tools/test_recipes_transformations.sh. */
public final class RecipeTransformationSelfTest {

    private static int tests;

    private RecipeTransformationSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        testAcyclicChain();
        testAcyclicDiamond();
        testDirectCycleRejected();
        testIndirectCycleRejected();
        testDepthGuardRejected();
        testUpgradeTransitions();
        testWeightedCostAddition();
        testWeightedCostAdditionAfterNegativeStock();
        testWeightedCostRemoval();
        testWeightedCostRemovalToZero();
        testInvalidWeightedCostRemovalRejected();
        testUomFactorNormalization();
        testZeroUomFactorRejected();
        testRoundedZeroUomFactorRejected();
        testWasteHashOrderIndependent();
        testDuplicateWasteProductRejected();
        testZeroWasteQuantityRejected();
        testRecipeComponentLabels();
        testTransformationLabels();
        testHistoryLimitBounds();
        System.out.println("KYLGIS_RECIPE_SELFTEST_OK tests=" + tests);
    }

    private static void testAcyclicChain() throws Exception {
        Map<String, Set<String>> graph = new HashMap<>();
        edge(graph, "A", "B");
        edge(graph, "B", "C");
        RecipeDefinitionService.assertAcyclic(graph);
        pass();
    }

    private static void testAcyclicDiamond() throws Exception {
        Map<String, Set<String>> graph = new HashMap<>();
        edge(graph, "A", "B");
        edge(graph, "A", "C");
        edge(graph, "B", "D");
        edge(graph, "C", "D");
        RecipeDefinitionService.assertAcyclic(graph);
        pass();
    }

    private static void testDirectCycleRejected() throws Exception {
        Map<String, Set<String>> graph = new HashMap<>();
        edge(graph, "A", "A");
        expectBasicException(() -> RecipeDefinitionService.assertAcyclic(graph), "direct cycle");
        pass();
    }

    private static void testIndirectCycleRejected() throws Exception {
        Map<String, Set<String>> graph = new HashMap<>();
        edge(graph, "A", "B");
        edge(graph, "B", "C");
        edge(graph, "C", "A");
        expectBasicException(() -> RecipeDefinitionService.assertAcyclic(graph), "indirect cycle");
        pass();
    }

    private static void testDepthGuardRejected() throws Exception {
        Map<String, Set<String>> graph = new HashMap<>();
        for (int i = 0; i < 130; i++) {
            edge(graph, "N" + i, "N" + (i + 1));
        }
        expectBasicException(() -> RecipeDefinitionService.assertAcyclic(graph), "depth guard");
        pass();
    }

    private static void testUpgradeTransitions() {
        require(DatabaseUpgradeService.supports("1.2", "1.3"), "1.2 -> 1.3 must be supported");
        require(DatabaseUpgradeService.supports("1.2.1", "1.3"), "1.2.1 -> 1.3 must be supported");
        require(DatabaseUpgradeService.supports("1.1", "1.3"), "1.1 -> 1.3 must be supported");
        require(DatabaseUpgradeService.supports("1.0", "1.3"), "1.0 -> 1.3 must be supported");
        require(!DatabaseUpgradeService.supports("1.3", "1.2"), "downgrade must be rejected");
        require(!DatabaseUpgradeService.supports("1.3", "1.3"), "no-op upgrade must be rejected");
        pass();
    }

    private static void testWeightedCostAddition() throws Exception {
        BigDecimal result = InventoryTransformationService.weightedCostAfterAdd(
                new BigDecimal("10"), new BigDecimal("2"),
                new BigDecimal("10"), new BigDecimal("40"));
        require(result.compareTo(new BigDecimal("3.000000")) == 0,
                "weighted add cost must be 3.000000");
        pass();
    }

    private static void testWeightedCostAdditionAfterNegativeStock() throws Exception {
        BigDecimal result = InventoryTransformationService.weightedCostAfterAdd(
                new BigDecimal("-2"), new BigDecimal("4"),
                new BigDecimal("3"), new BigDecimal("9"));
        require(result.compareTo(new BigDecimal("3.000000")) == 0,
                "negative stock replenishment must use incoming unit cost");
        pass();
    }

    private static void testWeightedCostRemoval() throws Exception {
        BigDecimal result = InventoryTransformationService.weightedCostAfterRemove(
                new BigDecimal("10"), new BigDecimal("3"),
                new BigDecimal("2"), new BigDecimal("6"));
        require(result != null && result.compareTo(new BigDecimal("3.000000")) == 0,
                "weighted removal cost must remain 3.000000");
        pass();
    }

    private static void testWeightedCostRemovalToZero() throws Exception {
        BigDecimal result = InventoryTransformationService.weightedCostAfterRemove(
                new BigDecimal("2"), new BigDecimal("3"),
                new BigDecimal("2"), new BigDecimal("6"));
        require(result == null, "removing all stock must leave reference cost unchanged");
        pass();
    }

    private static void testInvalidWeightedCostRemovalRejected() throws Exception {
        expectBasicException(() -> InventoryTransformationService.weightedCostAfterRemove(
                new BigDecimal("1"), new BigDecimal("3"),
                new BigDecimal("2"), new BigDecimal("6")), "weighted over-removal");
        pass();
    }

    private static void testUomFactorNormalization() throws Exception {
        BigDecimal result = ProductUomConversionService.normalizeFactor(new BigDecimal("2"));
        require(result.compareTo(new BigDecimal("2.000000000")) == 0,
                "UOM factor must normalize to 9 decimals");
        pass();
    }

    private static void testZeroUomFactorRejected() throws Exception {
        expectBasicException(() -> ProductUomConversionService.normalizeFactor(BigDecimal.ZERO),
                "zero UOM factor");
        pass();
    }

    private static void testRoundedZeroUomFactorRejected() throws Exception {
        expectBasicException(() -> ProductUomConversionService.normalizeFactor(
                new BigDecimal("0.0000000004")), "rounded-zero UOM factor");
        pass();
    }

    private static void testWasteHashOrderIndependent() throws Exception {
        InventoryTransformationService.WasteInput a =
                new InventoryTransformationService.WasteInput("A", new BigDecimal("1.5"), "KG");
        InventoryTransformationService.WasteInput b =
                new InventoryTransformationService.WasteInput("B", new BigDecimal("2"), "PZ");
        InventoryTransformationService.WasteRequest first =
                new InventoryTransformationService.WasteRequest("KEY", "LOC",
                        List.of(a, b), "USER", "MERMA");
        InventoryTransformationService.WasteRequest second =
                new InventoryTransformationService.WasteRequest("KEY", "LOC",
                        List.of(b, a), "USER", "MERMA");
        InventoryTransformationService.validateWasteRequest(first);
        InventoryTransformationService.validateWasteRequest(second);
        require(InventoryTransformationService.hashWasteRequest(first)
                .equals(InventoryTransformationService.hashWasteRequest(second)),
                "waste hash must not depend on input order");
        pass();
    }

    private static void testDuplicateWasteProductRejected() throws Exception {
        InventoryTransformationService.WasteInput a =
                new InventoryTransformationService.WasteInput("A", BigDecimal.ONE, "KG");
        InventoryTransformationService.WasteInput duplicate =
                new InventoryTransformationService.WasteInput("A", new BigDecimal("2"), "KG");
        InventoryTransformationService.WasteRequest request =
                new InventoryTransformationService.WasteRequest("KEY", "LOC",
                        List.of(a, duplicate), "USER", null);
        expectBasicException(() -> InventoryTransformationService.validateWasteRequest(request),
                "duplicate waste product");
        pass();
    }

    private static void testZeroWasteQuantityRejected() throws Exception {
        InventoryTransformationService.WasteInput zero =
                new InventoryTransformationService.WasteInput("A", BigDecimal.ZERO, "KG");
        InventoryTransformationService.WasteRequest request =
                new InventoryTransformationService.WasteRequest("KEY", "LOC",
                        List.of(zero), "USER", null);
        expectBasicException(() -> InventoryTransformationService.validateWasteRequest(request),
                "zero waste quantity");
        pass();
    }

    private static void testRecipeComponentLabels() {
        RecipeTransformationQueryService.RecipeComponent named =
                new RecipeTransformationQueryService.RecipeComponent(
                        1, "product-id", "ARZ-001", "Arroz", new BigDecimal("2"),
                        "uom-id", "Kilogramo", BigDecimal.ZERO);
        require("ARZ-001 - Arroz".equals(named.productLabel()),
                "recipe component must display product reference and name");
        require("Kilogramo".equals(named.uomLabel()),
                "recipe component must display UOM name");

        RecipeTransformationQueryService.RecipeComponent fallback =
                new RecipeTransformationQueryService.RecipeComponent(
                        2, "product-fallback", null, null, BigDecimal.ONE,
                        "uom-fallback", null, BigDecimal.ZERO);
        require("Producto sin nombre".equals(fallback.productLabel()),
                "missing product catalog label must not expose product id");
        require("Unidad sin nombre".equals(fallback.uomLabel()),
                "missing UOM catalog label must not expose UOM id");
        pass();
    }

    private static void testTransformationLabels() {
        RecipeTransformationQueryService.TransformationLine line =
                new RecipeTransformationQueryService.TransformationLine(
                        1, "INPUT", "product-id", "REF-001", "Producto",
                        null, new BigDecimal("2"), "uom-id", "Kilogramo",
                        new BigDecimal("2"), new BigDecimal("1"), new BigDecimal("2"), "diary");
        require("REF-001 - Producto".equals(line.productLabel()),
                "history line must display product reference and name");
        require("Kilogramo".equals(line.uomLabel()),
                "history line must display UOM name");

        RecipeTransformationQueryService.RecipeVersionSummary version =
                new RecipeTransformationQueryService.RecipeVersionSummary(
                        "version", "product", 1, "ACTIVE", BigDecimal.ONE,
                        "uom-id", "Kilogramo", null, "user", null);
        require("Kilogramo".equals(version.yieldUomLabel()),
                "recipe version must display yield UOM name");
        pass();
    }

    private static void testHistoryLimitBounds() throws Exception {
        require(RecipeTransformationQueryService.requireLimit(1) == 1,
                "history lower limit must be accepted");
        require(RecipeTransformationQueryService.requireLimit(500) == 500,
                "history upper limit must be accepted");
        expectBasicException(() -> RecipeTransformationQueryService.requireLimit(0),
                "history limit below range");
        expectBasicException(() -> RecipeTransformationQueryService.requireLimit(501),
                "history limit above range");
        pass();
    }

    private static void edge(Map<String, Set<String>> graph, String from, String to) {
        graph.computeIfAbsent(from, key -> new HashSet<>()).add(to);
    }

    private static void expectBasicException(CheckedAction action, String label) throws Exception {
        try {
            action.run();
        } catch (BasicException expected) {
            return;
        }
        throw new AssertionError("Expected BasicException for " + label);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void pass() {
        tests++;
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}