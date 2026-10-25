package com.mx.kylgis.pos.fiscal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Explicit CFDI tax mapping. Disabled legacy rules are never inferred from
 * historical RATE (in particular 0% != Exento, negative rate != Retencion).
 * This model is not itself a certification against the SAT's dated catalogs.
 */
public final class SatTaxProfile {
    public enum Factor { Tasa, Cuota, Exento }
    public enum Nature { Traslado, Retencion }

    private final String taxId;
    private final String impuesto;
    private final Factor factor;
    private final Nature nature;
    private final BigDecimal tasaCuota;
    private final String cuotaUnidad;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final boolean enabled;

    public SatTaxProfile(String taxId, String impuesto, Factor factor, Nature nature,
            BigDecimal tasaCuota, String cuotaUnidad, LocalDate desde, LocalDate hasta,
            boolean enabled) {
        if (taxId == null || taxId.isBlank()) {
            throw new IllegalArgumentException("TAX_ID requerido");
        }
        this.taxId = taxId;
        this.impuesto = impuesto == null ? null : impuesto.trim();
        this.factor = factor;
        this.nature = nature;
        this.tasaCuota = tasaCuota;
        this.cuotaUnidad = cuotaUnidad == null || cuotaUnidad.isBlank() ? null : cuotaUnidad.trim();
        this.desde = desde;
        this.hasta = hasta;
        this.enabled = enabled;
        validate();
    }

    private void validate() {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La vigencia final no puede anteceder a la inicial");
        }
        if (tasaCuota != null && (tasaCuota.signum() < 0 || tasaCuota.scale() > 6)) {
            throw new IllegalArgumentException("TasaOCuota debe tener entre 0 y 6 decimales y no ser negativa");
        }
        if (!enabled) {
            return;
        }
        if (!Objects.equals(impuesto, "001") && !Objects.equals(impuesto, "002")
                && !Objects.equals(impuesto, "003")) {
            throw new IllegalArgumentException("Clave SAT de impuesto desconocida (001/002/003)");
        }
        if (factor == null || nature == null) {
            throw new IllegalArgumentException("TipoFactor y naturaleza son obligatorios");
        }
        if (factor == Factor.Exento) {
            if (tasaCuota != null || cuotaUnidad != null || nature == Nature.Retencion) {
                throw new IllegalArgumentException("Exento no lleva TasaOCuota, unidad ni naturaleza Retencion");
            }
        } else {
            if (tasaCuota == null) {
                throw new IllegalArgumentException("TasaOCuota es obligatoria para Tasa/Cuota");
            }
            if (factor == Factor.Cuota && cuotaUnidad == null) {
                throw new IllegalArgumentException("Cuota requiere unidad fiscal explícita");
            }
            if (factor == Factor.Tasa && cuotaUnidad != null) {
                throw new IllegalArgumentException("Tasa porcentual no acepta cuotaUnidad");
            }
        }
    }

    public void requireActive(LocalDate date) {
        if (!enabled) {
            throw new IllegalStateException("Regla SAT incompleta/no habilitada");
        }
        if (date == null || (desde != null && date.isBefore(desde))
                || (hasta != null && date.isAfter(hasta))) {
            throw new IllegalStateException("Impuesto fuera de su vigencia");
        }
    }

    public String getTaxId() { return taxId; }
    public String getImpuesto() { return impuesto; }
    public Factor getFactor() { return factor; }
    public Nature getNature() { return nature; }
    public BigDecimal getTasaCuota() { return tasaCuota; }
    public String getCuotaUnidad() { return cuotaUnidad; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public boolean isEnabled() { return enabled; }
}