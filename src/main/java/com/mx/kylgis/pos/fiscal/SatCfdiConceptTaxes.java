package com.mx.kylgis.pos.fiscal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Produces ONLY the tax fragment of a CFDI 4.0 concept. Not a complete signed
 * invoice, PAC client or catalog substitute. Caller supplies SAT/PAC validation,
 * precision and a document already owned by its CFDI assembly pipeline.
 */
public final class SatCfdiConceptTaxes {
    private SatCfdiConceptTaxes() {}
    private static final String NS = "http://www.sat.gob.mx/cfd/4";

    public interface DecimalPolicy {
        String base(BigDecimal amount);
        String taxAmount(BigDecimal amount);
        String rateOrQuota(BigDecimal rateOrQuota);
    }

    public static Element write(Document xml, SatFiscalEngine.Concept concept,
            SatConceptMetadata product,
            SatFiscalEngine.Result amounts, LocalDate date,
            SatFiscalPreflight.CatalogPolicy catalogs, DecimalPolicy decimals) {
        Objects.requireNonNull(xml, "documento XML");
        Objects.requireNonNull(concept, "concepto");
        Objects.requireNonNull(product, "clasificacion fiscal producto");
        Objects.requireNonNull(amounts, "calculo SAT");
        Objects.requireNonNull(decimals, "politica de precision SAT");
        List<SatFiscalEngine.Rule> rules = new ArrayList<>();
        for (SatFiscalEngine.Line line : amounts.lines()) rules.add(line.rule());
        SatFiscalPreflight.validate(concept.objetoImp(), product.getClaveProdServ(),
                product.getClaveUnidad(), rules, date, catalogs);

        // Negative POS returns need a separate Egreso mapping: never serialize
        // invalid negative tax amounts into a standard CFDI concept.
        if (concept.base().signum() < 0)
            throw new IllegalArgumentException("Devolucion: requiere adaptador CFDI Egreso");
        for (SatFiscalEngine.Line line : amounts.lines()) {
            if (line.base().signum() < 0 || line.importe().signum() < 0)
                throw new IllegalArgumentException("Importe fiscal negativo: requiere Egreso");
        }

        if (amounts.lines().isEmpty()) return null;
        Element taxes = xml.createElementNS(NS, "cfdi:Impuestos");
        Element transfers = xml.createElementNS(NS, "cfdi:Traslados");
        Element retentions = xml.createElementNS(NS, "cfdi:Retenciones");
        for (SatFiscalEngine.Line line : amounts.lines()) {
            boolean withholding = line.rule().nature() == SatFiscalEngine.Nature.Retencion;
            Element tax = xml.createElementNS(NS, withholding
                    ? "cfdi:Retencion" : "cfdi:Traslado");
            tax.setAttribute("Base", decimals.base(line.base()));
            tax.setAttribute("Impuesto", line.rule().impuesto());
            tax.setAttribute("TipoFactor", line.rule().factor().name());
            if (!line.exento()) {
                tax.setAttribute("TasaOCuota", decimals.rateOrQuota(line.rule().tasaOCuota()));
                tax.setAttribute("Importe", decimals.taxAmount(line.importe()));
            }
            if (withholding) retentions.appendChild(tax);
            else transfers.appendChild(tax);
        }
        if (transfers.hasChildNodes()) taxes.appendChild(transfers);
        if (retentions.hasChildNodes()) taxes.appendChild(retentions);
        return taxes;
    }
}