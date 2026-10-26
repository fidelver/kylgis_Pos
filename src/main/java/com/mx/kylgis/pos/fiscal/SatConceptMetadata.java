package com.mx.kylgis.pos.fiscal;

import java.math.BigDecimal;

/** SAT semantic mapping of a product/concept; never infer ObjetoImp from RATE. */
public final class SatConceptMetadata {
    private final String productId;
    private final String objetoImp;
    private final String claveProdServ;
    private final String claveUnidad;
    private final BigDecimal cuotaCantidadPorUnidad;
    private final String cuotaUnidad;

    public SatConceptMetadata(String productId, String objetoImp,
            String claveProdServ, String claveUnidad) {
        this(productId, objetoImp, claveProdServ, claveUnidad, null, null);
    }

    public SatConceptMetadata(String productId, String objetoImp,
            String claveProdServ, String claveUnidad, BigDecimal cuotaCantidadPorUnidad,
            String cuotaUnidad) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Producto obligatorio");
        }
        // The SAT catalog is dated and must be validated by the issuing layer.
        if (objetoImp != null && !objetoImp.matches("[0-9]{2}")) {
            throw new IllegalArgumentException("ObjetoImp: clave de dos dígitos requerida");
        }
        if (claveProdServ != null && !claveProdServ.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("ClaveProdServ requiere ocho dígitos");
        }
        if (claveUnidad != null && !claveUnidad.matches("[A-Za-z0-9]{2,3}")) {
            throw new IllegalArgumentException("ClaveUnidad SAT no válida");
        }
        this.productId = productId;
        this.objetoImp = objetoImp;
        this.claveProdServ = claveProdServ;
        if (cuotaCantidadPorUnidad != null &&
                (cuotaCantidadPorUnidad.signum() <= 0 || cuotaCantidadPorUnidad.scale() > 6)) {
            throw new IllegalArgumentException("Conversión de Cuota debe ser positiva y de hasta 6 decimales");
        }
        if ((cuotaUnidad == null || cuotaUnidad.isBlank()) != (cuotaCantidadPorUnidad == null)) {
            throw new IllegalArgumentException("Unidad y factor de conversión deben configurarse juntos");
        }
        this.claveUnidad = claveUnidad;
        this.cuotaCantidadPorUnidad = cuotaCantidadPorUnidad;
        this.cuotaUnidad = cuotaUnidad == null || cuotaUnidad.isBlank() ? null : cuotaUnidad.trim();
    }
    public String getProductId() { return productId; }
    public String getObjetoImp() { return objetoImp; }
    public String getClaveProdServ() { return claveProdServ; }
    public String getClaveUnidad() { return claveUnidad; }
    public BigDecimal getCuotaCantidadPorUnidad() { return cuotaCantidadPorUnidad; }
    public String getCuotaUnidad() { return cuotaUnidad; }

    public BigDecimal cantidadFiscal(BigDecimal unidadesVendidas, String unidadRequerida) {
        if (unidadesVendidas == null || unidadesVendidas.signum() < 0 || cuotaCantidadPorUnidad == null
                || cuotaUnidad == null || !cuotaUnidad.equalsIgnoreCase(unidadRequerida)) {
            throw new IllegalArgumentException("Conversión a la unidad legal de Cuota no configurada");
        }
        return unidadesVendidas.multiply(cuotaCantidadPorUnidad);
    }
}