package com.mx.kylgis.pos.fiscal;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Fail-closed fiscal preflight. A PAC/SAT catalog adapter MUST supply an
 * authoritative date-sensitive policy. No built-in rate list is assumed current.
 */
public final class SatFiscalPreflight {
    private SatFiscalPreflight() {}

    public interface CatalogPolicy {
        /** Confirm the concept's ObjetoImp, ClaveProdServ, ClaveUnidad and legal applicability. */
        boolean conceptAllowed(String objetoImp, String claveProdServ, String claveUnidad, LocalDate issued);
        /** Confirm impuesto/factor/nature/rate eligibility for this concept and issue date. */
        boolean taxAllowed(SatFiscalEngine.Rule rule, String claveProdServ, LocalDate issued);
    }

    public static void validate(String objetoImp, String claveProdServ, String claveUnidad,
                                List<SatFiscalEngine.Rule> taxes, LocalDate issued, CatalogPolicy policy) {
        Objects.requireNonNull(issued, "fecha");
        Objects.requireNonNull(policy, "catalogo SAT/PAC requerido");
        Objects.requireNonNull(taxes, "impuestos");
        if (objetoImp == null || objetoImp.isBlank()
                || claveProdServ == null || claveProdServ.isBlank()
                || claveUnidad == null || claveUnidad.isBlank()
                || !policy.conceptAllowed(objetoImp, claveProdServ, claveUnidad, issued)) {
            throw new IllegalArgumentException("Concepto no validado por catalogo SAT");
        }
        if (!"02".equals(objetoImp) && !taxes.isEmpty()) {
            throw new IllegalArgumentException("Reglas fiscales incompatibles con ObjetoImp");
        }
        for (SatFiscalEngine.Rule rule : taxes) {
            rule.ensureUsable(issued);
            if (!policy.taxAllowed(rule, claveProdServ, issued))
                throw new IllegalArgumentException("Impuesto no validado por catalogo SAT");
        }
    }
}