package com.mx.kylgis.pos.fiscal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static com.mx.kylgis.pos.fiscal.SatFiscalEngine.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Dependency-free executable regression checks; not automatically run by Surefire. */
public final class SatFiscalEngineSelfTest {
    static final LocalDate DATE = LocalDate.of(2026, 10, 19);

    private static void eq(String expected, BigDecimal actual) {
        if (new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError(expected + " != " + actual);
        }
    }

    private static Rule rule(String id, String imp, Factor factor, Nature nature,
            String rate, boolean cascade) {
        return new Rule(id, imp, factor, nature,
                rate == null ? null : new BigDecimal(rate),
                null, null, true, cascade);
    }

    private static void fails(Runnable r) {
        try {
            r.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected invalid configuration");
    }

    public static void main(String[] args) throws Exception {
        Rule iva16 = rule("IVA16", "002", Factor.Tasa, Nature.Traslado, "0.160000", false);
        Result simple = calculate(new Concept("02", new BigDecimal("100.00"), BigDecimal.ONE),
                List.of(iva16), DATE);
        eq("16", simple.traslados());
        eq("116", simple.total());

        Rule iva0 = rule("IVA0", "002", Factor.Tasa, Nature.Traslado, "0.000000", false);
        Rule exento = rule("EX", "002", Factor.Exento, Nature.Traslado, null, false);
        Result zero = calculate(new Concept("02", new BigDecimal("50"), BigDecimal.ONE),
                List.of(iva0), DATE);
        Result exempt = calculate(new Concept("02", new BigDecimal("50"), BigDecimal.ONE),
                List.of(exento), DATE);
        eq("0", zero.traslados());
        eq("0", exempt.traslados());
        if (zero.lines().get(0).exento() || !exempt.lines().get(0).exento()) {
            throw new AssertionError("Tasa 0 differs from Exento");
        }

        Rule cuota = rule("IEPSQ", "003", Factor.Cuota, Nature.Traslado, "1.234500", false);
        Result quota = calculate(new Concept("02", new BigDecimal("20"), new BigDecimal("3")),
                List.of(cuota), DATE);
        eq("3.703500", quota.traslados());

        Rule reten = rule("RET", "001", Factor.Tasa, Nature.Retencion, "0.100000", false);
        Result mixed = calculate(new Concept("02", new BigDecimal("100"), BigDecimal.ONE),
                List.of(iva16, reten), DATE);
        eq("16", mixed.traslados());
        eq("10", mixed.retenciones());
        eq("106", mixed.total());

        Rule ivaCascade = rule("IVAC", "002", Factor.Tasa, Nature.Traslado, "0.160000", true);
        Result cascade = calculate(new Concept("02", new BigDecimal("100"), BigDecimal.ONE),
                List.of(rule("IEPS", "003", Factor.Tasa, Nature.Traslado, "0.080000", false),
                        ivaCascade), DATE);
        eq("25.28", cascade.traslados());

        Result decimals = calculate(new Concept("02", new BigDecimal("0.03"), BigDecimal.ONE),
                List.of(iva16), DATE);
        eq("0.0048", decimals.traslados());

        Result refund = calculate(new Concept("02", new BigDecimal("-100"), new BigDecimal("-1")),
                List.of(iva16), DATE);
        eq("-16", refund.traslados());

        fails(() -> rule("INVALID", "002", Factor.Exento, Nature.Traslado, "0", false));
        fails(() -> rule("INVALID", "002", Factor.Tasa, Nature.Retencion, "-0.1", false));
        fails(() -> calculate(new Concept("01", BigDecimal.ONE, BigDecimal.ONE),
                List.of(iva16), DATE));
        fails(() -> new Rule("X", "002", Factor.Tasa, Nature.Traslado, BigDecimal.ONE,
                DATE.plusDays(1), DATE.minusDays(1), true, false));
        if (canAutoMapLegacyRate(BigDecimal.ZERO)) {
            throw new AssertionError("unsafe legacy mapping");
        }

        Rule group = new Rule("ROOT", null, null, null, null, null, null, false, false);
        List<Node> tree = List.of(new Node(group, null, 0),
                new Node(rule("IEPS-TREE", "003", Factor.Tasa, Nature.Traslado, "0.08", false),
                        "ROOT", 1),
                new Node(rule("IVA-TREE", "002", Factor.Tasa, Nature.Traslado, "0.16", true),
                        "ROOT", 2));
        Result treeResult = calculateTree(new Concept("02", new BigDecimal("100"),
                BigDecimal.ONE), tree, "ROOT", DATE);
        eq("25.28", treeResult.traslados());

        fails(() -> calculateTree(new Concept("02", BigDecimal.ONE, BigDecimal.ONE),
                List.of(new Node(group, "IVA-TREE", 0),
                        new Node(iva16, "ROOT", 1),
                        new Node(rule("IVA-TREE", "002", Factor.Tasa, Nature.Traslado,
                                "0.16", false), "IVA16", 2)), "ROOT", DATE));
        fails(() -> calculateTree(new Concept("02", BigDecimal.ONE, BigDecimal.ONE),
                List.of(new Node(iva16, "UNKNOWN", 1)), "IVA16", DATE));

        Document xml = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        SatFiscalPreflight.CatalogPolicy catalogStub = new SatFiscalPreflight.CatalogPolicy() {
            @Override
            public boolean conceptAllowed(String objeto, String claveProd, String claveUnidad,
                    LocalDate date) {
                return true;
            }

            @Override
            public boolean taxAllowed(Rule r, String claveProd, LocalDate date) {
                return true;
            }
        };
        SatCfdiConceptTaxes.DecimalPolicy decimalsPolicy =
                new SatCfdiConceptTaxes.DecimalPolicy() {
            @Override
            public String base(BigDecimal value) { return value.toPlainString(); }

            @Override
            public String taxAmount(BigDecimal value) { return value.toPlainString(); }

            @Override
            public String rateOrQuota(BigDecimal value) { return value.toPlainString(); }
        };
        SatConceptMetadata classification = new SatConceptMetadata(
                "product", "02", "90101800", "H87");
        Element xmlIva = SatCfdiConceptTaxes.write(xml,
                new Concept("02", new BigDecimal("100"), BigDecimal.ONE),
                classification, mixed, DATE, catalogStub, decimalsPolicy);
        if (!"Impuestos".equals(xmlIva.getLocalName())
                || xmlIva.getElementsByTagNameNS(
                        "http://www.sat.gob.mx/cfd/4", "Traslado").getLength() != 1
                || xmlIva.getElementsByTagNameNS(
                        "http://www.sat.gob.mx/cfd/4", "Retencion").getLength() != 1) {
            throw new AssertionError("CFDI tax nodes");
        }

        Element xmlExento = SatCfdiConceptTaxes.write(xml,
                new Concept("02", new BigDecimal("50"), BigDecimal.ONE),
                classification, exempt, DATE, catalogStub, decimalsPolicy);
        Element exemptNode = (Element) xmlExento.getElementsByTagNameNS(
                "http://www.sat.gob.mx/cfd/4", "Traslado").item(0);
        if (exemptNode.hasAttribute("TasaOCuota") || exemptNode.hasAttribute("Importe")) {
            throw new AssertionError("Exento must omit TasaOCuota and Importe");
        }

        fails(() -> SatCfdiConceptTaxes.write(xml,
                new Concept("02", new BigDecimal("-100"), BigDecimal.ONE),
                classification, refund, DATE, catalogStub, decimalsPolicy));

        System.out.println("SAT_FISCAL_ENGINE_SELFTEST_OK");
    }
}
