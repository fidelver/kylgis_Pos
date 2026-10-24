package com.mx.kylgis.pos.fiscal;

import com.mx.kylgis.pos.ticket.TicketLineInfo;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fail-closed SAT evaluation adapter for a new POS ticket line.
 *
 * Separate from the live legacy TaxesLogic (which remains the commercial
 * calculation until the protected sales integration is reconciled). Do NOT
 * use this class to generate or stamp a full CFDI without catalog validation
 * and fiscal testing with the chosen PAC.
 */
public final class SatSaleFiscalEvaluator {
    private SatSaleFiscalEvaluator() {}

    public static final class Concept {
        private final SatConceptMetadata metadata;
        private final BigDecimal base;
        private final List<SatTaxCalculator.Line> taxes;
        private Concept(SatConceptMetadata m,BigDecimal b,List<SatTaxCalculator.Line> t) {
            this.metadata=m;this.base=b;
            this.taxes=java.util.Collections.unmodifiableList(new ArrayList<>(t));
        }
        public SatConceptMetadata metadata() { return metadata; }
        public BigDecimal base() { return base; }
        public List<SatTaxCalculator.Line> taxes() { return taxes; }
        public SatTaxCalculator.Totals totals() { return SatTaxCalculator.aggregate(taxes); }
    }

    public static Concept evaluate(TicketLineInfo line,
            SatTaxRepository repository, LocalDate date) throws SQLException {
        if (line==null || repository==null || line.getProductID()==null) {
            throw new IllegalArgumentException("Se necesita una línea vinculada a un producto");
        }
        SatConceptMetadata metadata=repository.findProduct(line.getProductID());
        return evaluate(line,metadata,repository,date);
    }

    public static Concept evaluate(TicketLineInfo line,SatConceptMetadata metadata,
            SatTaxRepository repository,LocalDate date) throws SQLException {
        if (line==null || metadata==null || repository==null || date==null) {
            throw new IllegalArgumentException("ObjetoImp y datos de concepto requeridos");
        }
        if (metadata.getObjetoImp()==null || metadata.getClaveProdServ()==null
                || metadata.getClaveUnidad()==null) {
            throw new IllegalStateException("Claves SAT del producto/concepto incompletas");
        }
        BigDecimal units=BigDecimal.valueOf(line.getMultiply());
        BigDecimal base=BigDecimal.valueOf(line.getPrice()).multiply(units);
        if (units.signum()<0 || base.signum()<0) {
            throw new IllegalArgumentException("Devoluciones deben declararse como comprobantes de egreso");
        }
        if ("01".equals(metadata.getObjetoImp())) {
            return new Concept(metadata,base,java.util.Collections.emptyList());
        }
        // Other c_ObjetoImp keys need context-specific legal logic. Don't
        // treat them silently as key 02, even if their RATE is nonzero.
        if (!"02".equals(metadata.getObjetoImp())) {
            throw new IllegalStateException("ObjetoImp requiere validación fiscal especializada: "
                    + metadata.getObjetoImp());
        }
        if (line.getTaxInfo()==null || line.getTaxInfo().getId()==null) {
            throw new IllegalStateException("La línea no tiene una regla fiscal de impuesto");
        }
        List<SatTaxCalculator.Line> out=evaluateTree(line.getTaxInfo().getId(),
                base,units,metadata,repository,date,new HashSet<>());
        if (out.isEmpty()) throw new IllegalStateException("La regla no produjo impuestos SAT");
        return new Concept(metadata,base,out);
    }

    private static List<SatTaxCalculator.Line> evaluateTree(String id,BigDecimal base,
            BigDecimal units,SatConceptMetadata product,SatTaxRepository repository,
            LocalDate date,Set<String> ancestors) throws SQLException {
        if (!ancestors.add(id)) {
            throw new IllegalStateException("Ciclo detectado en el árbol de impuestos: " + id);
        }
        try {
            List<SatTaxRepository.ChildTax> children=repository.listChildren(id);
            if (children.isEmpty()) {
                SatTaxProfile tax=repository.findTax(id);
                if (tax==null) throw new IllegalStateException("Falta mapeo SAT: "+id);
                BigDecimal amountUnits=tax.getFactor()==SatTaxProfile.Factor.Cuota
                        ? product.cantidadFiscal(units,tax.getCuotaUnidad()) : null;
                return java.util.Collections.singletonList(
                        SatTaxCalculator.calculate(tax,base,amountUnits,date));
            }
            List<SatTaxCalculator.Line> out=new ArrayList<>();
            BigDecimal accumulatedBase=base;
            for(SatTaxRepository.ChildTax child:children) {
                BigDecimal childBase=child.cascade?accumulatedBase:base;
                List<SatTaxCalculator.Line> applied=evaluateTree(child.taxId,
                        childBase,units,product,repository,date,ancestors);
                out.addAll(applied);
                // Compatible with legacy RATECASCADE calculation sequence,
                // but legal applicability of tax-on-tax MUST be checked separately.
                accumulatedBase=accumulatedBase.add(SatTaxCalculator.aggregate(applied).netTax());
            }
            return out;
        } finally {
            ancestors.remove(id);
        }
    }
}