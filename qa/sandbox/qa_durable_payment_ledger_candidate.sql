-- QA-ONLY migration candidate. DO NOT APPLY TO REAL POS, .17 OR .18.
-- Separate synthetic DB only. Contract is not integrated into DataLogicSales yet.
CREATE TABLE qa_sale_operation_intents (
 operation_uuid VARCHAR(36) NOT NULL,
 receipt_id VARCHAR(255) NOT NULL,
 terminal_id VARCHAR(128) NOT NULL,
 state VARCHAR(32) NOT NULL DEFAULT 'PREPARED',
 revision BIGINT UNSIGNED NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY (operation_uuid),
 UNIQUE KEY qa_unique_receipt (receipt_id),
 CONSTRAINT qa_operation_state_chk CHECK (state IN
 ('PREPARED','IN_FLIGHT','COMMIT_UNKNOWN','DB_CONFIRMED','DB_NOT_CONFIRMED','BANK_RECONCILIATION','RECONCILED')),
 CONSTRAINT qa_operation_uuid_nonempty CHECK (CHAR_LENGTH(operation_uuid)=36)
) ENGINE=InnoDB;
CREATE TABLE qa_external_payment_intents (
 payment_uuid VARCHAR(36) NOT NULL,
 operation_uuid VARCHAR(36) NOT NULL,
 provider VARCHAR(64) NOT NULL,
 provider_reference VARCHAR(128) DEFAULT NULL,
 amount DECIMAL(18,2) NOT NULL,
 currency CHAR(3) NOT NULL DEFAULT 'MXN',
 state VARCHAR(32) NOT NULL DEFAULT 'PREPARED',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(payment_uuid),
 UNIQUE KEY qa_provider_reference_once (provider,provider_reference),
 CONSTRAINT qa_intent_operation_fk FOREIGN KEY(operation_uuid)
 REFERENCES qa_sale_operation_intents(operation_uuid),
 CONSTRAINT qa_payment_amount_chk CHECK (amount>=0),
 CONSTRAINT qa_payment_state_chk CHECK (state IN
 ('PREPARED','UNKNOWN','APPROVED','DECLINED','RECONCILED'))
) ENGINE=InnoDB;
-- Deliberately do NOT add UNIQUE(payments.TRANSID): legacy mixed tenders may share it.
-- SQL inserts/updates alone cannot authenticate provider APPROVED.
-- COMMIT_UNKNOWN and bank UNKNOWN forbid repeat charging until verified reconciliation.
