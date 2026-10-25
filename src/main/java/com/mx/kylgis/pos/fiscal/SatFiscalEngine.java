package com.mx.kylgis.pos.fiscal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Independent CFDI tax calculation model. Does NOT mutate legacy POS taxes/tickets.
 * Mapping and serialization must be checked against the SAT catalogs for the invoice date.
 */
public final class SatFiscalEngine {
    private SatFiscalEngine() {}

    public enum Factor { Tasa, Cuota, Exento }
    public enum Nature { Traslado, Retencion }

    public record Rule(String taxId, String impuesto, Factor factor, Nature nature,
                       BigDecimal tasaOCuota, LocalDate desde, LocalDate hasta,
                       boolean enabled, boolean cascade) {
        public Rule {
            if (taxId == null || taxId.isBlank()) throw new IllegalArgumentException("taxId obligatorio");
            if (desde != null && hasta != null && desde.isAfter(hasta))
                throw new IllegalArgumentException("vigencia invertida");
            if (enabled) {
                if (!List.of("001", "002", "003").contains(impuesto))
                    throw new IllegalArgumentException("impuesto SAT invalido");
                Objects.requireNonNull(factor, "factor");
                Objects.requireNonNull(nature, "nature");
                if (factor == Factor.Exento) {
                    if (nature != Nature.Traslado || tasaOCuota != null) {
                        throw new IllegalArgumentException("Exento solo aplica como Traslado y no lleva TasaOCuota");
                    }
                } else {
                    if (tasaOCuota == null || tasaOCuota.signum() < 0)
                        throw new IllegalArgumentException("TasaOCuota requerida y no negativa");
                }
            }
        }

        public void ensureUsable(LocalDate date) {
            Objects.requireNonNull(date, "fecha");
            if (!enabled) throw new IllegalArgumentException("Regla SAT sin habilitar");
            if (desde != null && date.isBefore(desde) || hasta != null && date.isAfter(hasta))
                throw new IllegalArgumentException("Regla SAT fuera de vigencia");
        }
    }

    /** Base already net of discounts; unitsFiscal must use the legally applicable quota unit. */
    public record Concept(String objetoImp, BigDecimal base, BigDecimal unitsFiscal) {
        public Concept {
            Objects.requireNonNull(objetoImp, "ObjetoImp");
            Objects.requireNonNull(base, "base");
            Objects.requireNonNull(unitsFiscal, "unitsFiscal");
            if (objetoImp.isBlank()) throw new IllegalArgumentException("ObjetoImp vacio");
        }
    }

    public record Line(Rule rule, BigDecimal base, BigDecimal importe) {
        public Line {
            Objects.requireNonNull(rule);
            Objects.requireNonNull(base);
            Objects.requireNonNull(importe);
        }
        public boolean exento() { return rule.factor() == Factor.Exento; }
    }

    public record Result(List<Line> lines, BigDecimal traslados, BigDecimal retenciones,
                         BigDecimal total) {
        public Result {
            lines = List.copyOf(lines);
        }
    }

    /**
     * Rules must be ordered intentionally. Does not round intermediate values: CFDI
     * validation/rounding is the responsibility of the certified serializer.
     */
    public static Result calculate(Concept concept, List<Rule> rules, LocalDate date) {
        Objects.requireNonNull(concept);
        Objects.requireNonNull(rules);
        Objects.requireNonNull(date);
        if (!"02".equals(concept.objetoImp()) && !rules.isEmpty())
            throw new IllegalArgumentException("ObjetoImp distinto de 02: clasificar antes de generar impuestos");
        List<Line> lines = new ArrayList<>();
        BigDecimal traslados = BigDecimal.ZERO;
        BigDecimal retenciones = BigDecimal.ZERO;
        for (Rule rule : rules) {
            rule.ensureUsable(date);
            // Cascade explicitly modifies the taxable base only with PRIOR transfers.
            BigDecimal base = rule.cascade() ? concept.base().add(traslados) : concept.base();
            BigDecimal importe = switch (rule.factor()) {
                case Exento -> BigDecimal.ZERO;
                case Tasa -> base.multiply(rule.tasaOCuota(), MathContext.UNLIMITED);
                case Cuota -> concept.unitsFiscal().multiply(rule.tasaOCuota(), MathContext.UNLIMITED);
            };
            lines.add(new Line(rule, base, importe));
            if (rule.nature() == Nature.Traslado) traslados = traslados.add(importe);
            else retenciones = retenciones.add(importe);
        }
        return new Result(lines, traslados, retenciones,
                concept.base().add(traslados).subtract(retenciones));
    }

    /**
     * Explicit hierarchy adapter, preserving legacy root-as-group semantics without
     * using negative rates as withholding signals. Only leaves produce tax lines.
     */
    public record Node(Rule rule, String parentId, int order) {}

    public static Result calculateTree(Concept concept, List<Node> nodes,
                                       String rootId, LocalDate date) {
        Objects.requireNonNull(nodes);
        java.util.Map<String, Node> byId = new java.util.HashMap<>();
        for (Node node : nodes) {
            if (byId.putIfAbsent(node.rule().taxId(), node) != null)
                throw new IllegalArgumentException("Impuesto repetido: " + node.rule().taxId());
        }
        if (!byId.containsKey(rootId)) throw new IllegalArgumentException("Impuesto raiz inexistente");
        for (Node node : nodes) {
            if (node.parentId() != null && !byId.containsKey(node.parentId()))
                throw new IllegalArgumentException("Padre fiscal inexistente");
        }
        // Validate ALL paths, not only the selected root, to detect inherited corruption.
        for (Node node : nodes) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            String current = node.rule().taxId();
            while (current != null) {
                if (!seen.add(current)) throw new IllegalArgumentException("Ciclo de impuestos SAT");
                current = byId.get(current).parentId();
            }
        }
        if (!"02".equals(concept.objetoImp()))
            throw new IllegalArgumentException("ObjetoImp no admite este desglose fiscal");
        List<Line> lines = new ArrayList<>();
        calculateNode(byId, rootId, concept.base(), concept.unitsFiscal(), date, lines);
        BigDecimal transfers = BigDecimal.ZERO;
        BigDecimal withheld = BigDecimal.ZERO;
        for (Line item : lines) {
            if (item.rule().nature() == Nature.Traslado) transfers = transfers.add(item.importe());
            else withheld = withheld.add(item.importe());
        }
        return new Result(lines, transfers, withheld,
                concept.base().add(transfers).subtract(withheld));
    }

    private static BigDecimal calculateNode(java.util.Map<String, Node> byId, String id,
                                            BigDecimal base, BigDecimal fiscalUnits,
                                            LocalDate date, List<Line> lines) {
        Node node = byId.get(id);
        List<Node> children = new ArrayList<>();
        for (Node n : byId.values()) {
            if (id.equals(n.parentId())) children.add(n);
        }
        children.sort(java.util.Comparator.comparingInt(Node::order)
                .thenComparing(n -> n.rule().taxId()));
        if (children.isEmpty()) {
            Rule rule = node.rule();
            rule.ensureUsable(date);
            BigDecimal amount = switch (rule.factor()) {
                case Exento -> BigDecimal.ZERO;
                case Tasa -> base.multiply(rule.tasaOCuota());
                case Cuota -> fiscalUnits.multiply(rule.tasaOCuota());
            };
            lines.add(new Line(rule, base, amount));
            return rule.nature() == Nature.Traslado ? amount : BigDecimal.ZERO;
        }
        BigDecimal accumulatedTransfers = BigDecimal.ZERO;
        for (Node child : children) {
            BigDecimal childBase = child.rule().cascade()
                    ? base.add(accumulatedTransfers) : base;
            accumulatedTransfers = accumulatedTransfers.add(
                    calculateNode(byId, child.rule().taxId(), childBase,
                            fiscalUnits, date, lines));
        }
        return accumulatedTransfers;
    }

    /** Guardrail: legacy zero-rate/negative-rate rules MUST NOT be auto-classified. */
    public static boolean canAutoMapLegacyRate(BigDecimal legacyRate) {
        return false;
    }
}