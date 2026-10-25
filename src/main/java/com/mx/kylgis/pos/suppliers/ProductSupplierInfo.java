//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.suppliers;

import java.sql.Timestamp;

/**
 * Association metadata between one product and one supplier.
 *
 * The supplier relationship is intentionally kept outside the legacy product
 * row so existing product serializers retain their historical field indexes.
 */
public final class ProductSupplierInfo {

    private final String productId;
    private final String supplierId;
    private final String supplierName;
    private final String supplierSearchKey;
    private final boolean preferred;
    private final boolean active;
    private final String supplierSku;
    private final String purchaseUom;
    private final Double lastCost;
    private final Integer priority;
    private final Timestamp lastPurchase;

    public ProductSupplierInfo(String productId, String supplierId,
            String supplierName, String supplierSearchKey,
            boolean preferred, boolean active, String supplierSku,
            String purchaseUom, Double lastCost, Integer priority,
            Timestamp lastPurchase) {
        this.productId = productId;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.supplierSearchKey = supplierSearchKey;
        this.preferred = preferred;
        this.active = active;
        this.supplierSku = supplierSku;
        this.purchaseUom = purchaseUom;
        this.lastCost = lastCost;
        this.priority = priority;
        this.lastPurchase = lastPurchase;
    }

    public ProductSupplierInfo(String supplierId, boolean preferred) {
        this(null, supplierId, null, null, preferred, true,
                null, null, null, null, null);
    }

    public String getProductId() {
        return productId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getSupplierSearchKey() {
        return supplierSearchKey;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public boolean isActive() {
        return active;
    }

    public String getSupplierSku() {
        return supplierSku;
    }

    public String getPurchaseUom() {
        return purchaseUom;
    }

    public Double getLastCost() {
        return lastCost;
    }

    public Integer getPriority() {
        return priority;
    }

    public Timestamp getLastPurchase() {
        return lastPurchase;
    }
}
