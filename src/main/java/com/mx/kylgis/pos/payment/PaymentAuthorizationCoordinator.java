package com.mx.kylgis.pos.payment;

import com.mx.kylgis.pos.basic.BasicException;

/**
 * QA-only hook that makes the durable payment intent a barrier before gateway AUTH.
 * Production remains a no-op because the coordinator is only installed when the
 * isolated durable-ledger flag is enabled.
 */
public interface PaymentAuthorizationCoordinator {
    void beforeCardAuthorization(PaymentInfoMagcard payment) throws BasicException;
    void afterCardAuthorization(PaymentInfoMagcard payment) throws BasicException;
    void markDurableCoordinationFailure(String message);
}
