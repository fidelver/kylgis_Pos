-- KylGis POS - authorized voucher edit audit (PostgreSQL)
-- NEXT RELEASE ONLY. Do not run on production without backup, LAB rehearsal and explicit authorization.

CREATE TABLE voucher_reactivation_audit (
    id VARCHAR NOT NULL,
    voucher_number VARCHAR NOT NULL,
    original_receipt VARCHAR NOT NULL,
    new_receipt VARCHAR NOT NULL,
    operator_id VARCHAR NOT NULL,
    authorized_by VARCHAR NOT NULL,
    authorized_at TIMESTAMP NOT NULL,
    reason VARCHAR NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT voucher_reactivation_original_uq UNIQUE (original_receipt, voucher_number)
);
CREATE INDEX voucher_reactivation_new_inx ON voucher_reactivation_audit(new_receipt);
CREATE INDEX voucher_reactivation_authorized_inx ON voucher_reactivation_audit(authorized_by, authorized_at);

-- Rollback:
-- DROP TABLE voucher_reactivation_audit;
