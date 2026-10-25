package com.mx.kylgis.pos.fiscal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exact, sign-preserving fiscal computation. Unrounded values are returned;
 * a CFDI serializer/PAC must validate SAT catalog, currency precision,
 * aggregation and applicable rounding rules at document creation.
 */
public final class SatTaxCalculator {
    private SatTaxCalculator() { }

    public static final class Line {
        private final SatTaxProfile profile;
        private final BigDecimal base;
        private final BigDecimal importe;
        private final BigDecimal cantidadFiscal;

        private Line(SatTaxProfile p, BigDecimal b, BigDecimal i, BigDecimal cantidadFiscal) {
            profile = p; base = b; importe = i; this.cantidadFiscal = cantidadFiscal;
        }
        public SatTaxProfile profile() { return profile; }
        public BigDecimal base() { return base; }
        public BigDecimal importe() { return importe; }
        public BigDecimal cantidadFiscal() { return cantidadFiscal; }
        public boolean isExento() { return profile.getFactor() == SatTaxProfile.Factor.Exento; }
        public BigDecimal signedEffect() {
            return profile.getNature() == SatTaxProfile.Nature.Retencion
                    ? importe.negate() : importe;
        }
    }

    /**
     * "cantidadFiscal" is mandatory for Cuota and MUST already be expressed
     * in the exact legal unit (liters/kg/pieces etc); it is not a money base.
     * Negative base or quantity for returns is rejected: CFDI egreso must
     * represent nonnegative tax amounts with its separate document type.
     */
    public static Line calculate(SatTaxProfile profile, BigDecimal base,
            BigDecimal cantidadFiscal, LocalDate date) {
        if (profile == null || base == null) {
            throw new IllegalArgumentException("Perfil y base obligatorios");
        }
        profile.requireActive(date);
        if (base.signum() < 0) {
            throw new IllegalArgumentException("CFDI requiere base no negativa; usar egreso para devoluciones");
        }
        BigDecimal amount;
        switch (profile.getFactor()) {
            case Exento:
                amount = BigDecimal.ZERO;
                break;
            case Tasa:
                amount = base.multiply(profile.getTasaCuota());
                break;
            case Cuota:
                if (cantidadFiscal == null || cantidadFiscal.signum() < 0) {
                    throw new IllegalArgumentException("Cuota requiere cantidad fiscal no negativa");
                }
                amount = cantidadFiscal.multiply(profile.getTasaCuota());
                break;
            default:
                throw new IllegalStateException("Factor sin soporte");
        }
        return new Line(profile, base, amount,
                profile.getFactor() == SatTaxProfile.Factor.Cuota ? cantidadFiscal : null);
    }

    public static final class Totals {
        private final BigDecimal traslados;
        private final BigDecimal retenciones;
        private final List<Line> lines;
        private Totals(BigDecimal t, BigDecimal r, List<Line> lines) {
            this.traslados = t;
            this.retenciones = r;
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        }
        public BigDecimal traslados() { return traslados; }
        public BigDecimal retenciones() { return retenciones; }
        public BigDecimal netTax() { return traslados.subtract(retenciones); }
        public List<Line> lines() { return lines; }
    }

    public static Totals aggregate(List<Line> lines) {
        BigDecimal traslados = BigDecimal.ZERO;
        BigDecimal retenciones = BigDecimal.ZERO;
        for (Line line : lines) {
            if (line.profile().getNature() == SatTaxProfile.Nature.Traslado) {
                traslados = traslados.add(line.importe());
            } else {
                retenciones = retenciones.add(line.importe());
            }
        }
        return new Totals(traslados, retenciones, lines);
    }

    /** Presentation-only rounding; NOT a replacement for Anexo 20 validation. */
    public static BigDecimal rounded(BigDecimal amount, int decimals) {
        if (amount == null || decimals < 0 || decimals > 6) {
            throw new IllegalArgumentException("Precisión no válida");
        }
        return amount.setScale(decimals, RoundingMode.HALF_UP);
    }
}