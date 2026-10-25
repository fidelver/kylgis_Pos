//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.voucher;

import com.mx.kylgis.pos.payment.PaymentInfo;

/**
 * Transient carrier used by the payment dialog for a voucher discount.
 * It intentionally implements PaymentInfo only to preserve the existing
 * JPaymentInterface contract; JPaymentSelect keeps it out of real payments.
 */
public final class VoucherDiscountInfo extends PaymentInfo {

    public static final String NAME = "voucherdiscount";

    private final String voucherId;
    private final String voucherNumber;
    private final String customerId;
    private final double faceValue;
    private final double appliedAmount;

    public VoucherDiscountInfo(String voucherId, String voucherNumber,
            String customerId, double faceValue, double appliedAmount) {
        this.voucherId = voucherId;
        this.voucherNumber = voucherNumber;
        this.customerId = customerId;
        this.faceValue = faceValue;
        this.appliedAmount = appliedAmount;
    }

    public String getVoucherId() {
        return voucherId;
    }

    @Override
    public String getVoucher() {
        return voucherNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public double getFaceValue() {
        return faceValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public double getTotal() {
        return appliedAmount;
    }

    @Override
    public PaymentInfo copyPayment() {
        return new VoucherDiscountInfo(voucherId, voucherNumber, customerId, faceValue, appliedAmount);
    }

    @Override
    public String getTransactionID() {
        return null;
    }

    @Override
    public double getPaid() {
        return appliedAmount;
    }

    @Override
    public double getChange() {
        return 0.0;
    }

    @Override
    public double getTendered() {
        return appliedAmount;
    }

    @Override
    public String getCardName() {
        return null;
    }
}
