package com.mx.kylgis.pos.data.loader;

import com.mx.kylgis.pos.basic.BasicException;

/**
 * A transaction was in the finalization phase but the client could not
 * establish whether the server durably accepted or rejected it.
 *
 * NEVER interpret this exception as proof of rollback; the caller must
 * reconcile using an independent authoritative connection and durable IDs.
 */
public final class TransactionOutcomeUnknownException extends BasicException {
    public enum Phase { COMMIT, ROLLBACK }
    private final Phase phase;

    public TransactionOutcomeUnknownException(Phase phase, Throwable cause) {
        super("Transaction " + phase + " outcome unknown; manual reconciliation required", cause);
        this.phase = phase;
    }
    public Phase getPhase() { return phase; }
}
