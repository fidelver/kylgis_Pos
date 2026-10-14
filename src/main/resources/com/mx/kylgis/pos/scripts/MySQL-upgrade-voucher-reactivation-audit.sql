-- KylGis POS - authorized voucher edit audit (MySQL / MariaDB)
-- NEXT RELEASE ONLY. Do not run on production without backup, LAB rehearsal and explicit authorization.

CREATE TABLE voucher_reactivation_audit (
    id VARCHAR(100) NOT NULL,
    voucher_number VARCHAR(100) NOT NULL,
    original_receipt VARCHAR(255) NOT NULL,
    new_receipt VARCHAR(255) NOT NULL,
    operator_id VARCHAR(255) NOT NULL,
    authorized_by VARCHAR(255) NOT NULL,
    authorized_at DATETIME NOT NULL,
    reason VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX voucher_reactivation_original_uq (original_receipt, voucher_number),
    INDEX voucher_reactivation_new_inx (new_receipt),
    INDEX voucher_reactivation_authorized_inx (authorized_by, authorized_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=Dynamic;

-- Rollback:
-- DROP TABLE voucher_reactivation_audit;
