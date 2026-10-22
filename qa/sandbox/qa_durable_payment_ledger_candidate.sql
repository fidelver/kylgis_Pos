-- QA ONLY. Never apply to .17/.18 or a real POS catalog.
-- Nullable binding fields can represent old unbound prototype rows; real saveTicket refuses them.
CREATE TABLE qa_sale_operation_intents (
 operation_uuid VARCHAR(36) NOT NULL,
 receipt_id VARCHAR(255) NOT NULL,
 terminal_id VARCHAR(128) NOT NULL,
 request_sha256 CHAR(64) DEFAULT NULL,
 state VARCHAR(32) NOT NULL DEFAULT 'PREPARED',
 revision BIGINT UNSIGNED NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(operation_uuid),
 UNIQUE KEY qa_unique_receipt(receipt_id),
 CONSTRAINT qa_operation_state_chk CHECK(state IN
 ('PREPARED','IN_FLIGHT','COMMIT_UNKNOWN','DB_CONFIRMED','DB_NOT_CONFIRMED','BANK_RECONCILIATION','RECONCILED')),
 CONSTRAINT qa_operation_uuid_nonempty CHECK(CHAR_LENGTH(operation_uuid)=36),
 CONSTRAINT qa_request_hash_chk CHECK(request_sha256 IS NULL OR request_sha256 REGEXP '^[a-f0-9]{64}$')
) ENGINE=InnoDB;
CREATE TABLE qa_external_payment_intents (
 payment_uuid VARCHAR(36) NOT NULL,
 operation_uuid VARCHAR(36) NOT NULL,
 provider VARCHAR(64) NOT NULL,
 provider_reference VARCHAR(128) DEFAULT NULL,
 component_index INT DEFAULT NULL,
 amount DECIMAL(18,2) NOT NULL,
 currency CHAR(3) NOT NULL DEFAULT 'MXN',
 state VARCHAR(32) NOT NULL DEFAULT 'PREPARED',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(payment_uuid),
 UNIQUE KEY qa_provider_reference_once(provider,provider_reference),
 UNIQUE KEY qa_component_once(operation_uuid,component_index),
 CONSTRAINT qa_intent_operation_fk FOREIGN KEY(operation_uuid) REFERENCES qa_sale_operation_intents(operation_uuid),
 CONSTRAINT qa_payment_amount_chk CHECK(amount>=0),
 CONSTRAINT qa_component_index_chk CHECK(component_index IS NULL OR component_index>=0),
 CONSTRAINT qa_payment_state_chk CHECK(state IN ('PREPARED','UNKNOWN','APPROVED','DECLINED','RECONCILED'))
) ENGINE=InnoDB;
DELIMITER //
CREATE TRIGGER qa_operation_immutable BEFORE UPDATE ON qa_sale_operation_intents FOR EACH ROW
BEGIN
 IF NOT (NEW.operation_uuid <=> OLD.operation_uuid) OR NOT (NEW.receipt_id <=> OLD.receipt_id)
 OR NOT (NEW.terminal_id <=> OLD.terminal_id) OR NOT (NEW.request_sha256 <=> OLD.request_sha256) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA_OPERATION_IDENTITY_IMMUTABLE';
 END IF;
END//
CREATE TRIGGER qa_operation_retained BEFORE DELETE ON qa_sale_operation_intents FOR EACH ROW
BEGIN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA_OPERATION_RETAINED_FOR_RECONCILIATION';
END//
CREATE TRIGGER qa_payment_immutable BEFORE UPDATE ON qa_external_payment_intents FOR EACH ROW
BEGIN
 IF NOT (NEW.payment_uuid <=> OLD.payment_uuid) OR NOT (NEW.operation_uuid <=> OLD.operation_uuid)
 OR NOT (NEW.provider <=> OLD.provider) OR NOT (NEW.component_index <=> OLD.component_index)
 OR NOT (NEW.amount <=> OLD.amount) OR NOT (NEW.currency <=> OLD.currency)
 OR (OLD.provider_reference IS NOT NULL AND NOT (NEW.provider_reference <=> OLD.provider_reference)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA_PAYMENT_REQUEST_IMMUTABLE';
 END IF;
 IF NEW.state <> OLD.state AND NOT (
  (OLD.state='PREPARED' AND NEW.state IN ('UNKNOWN','DECLINED')) OR
  (OLD.state='UNKNOWN' AND NEW.state IN ('APPROVED','DECLINED')) OR
  (OLD.state='APPROVED' AND NEW.state='RECONCILED')
 ) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA_PAYMENT_STATE_CANNOT_BACKTRACK';
 END IF;
END//
CREATE TRIGGER qa_payment_retained BEFORE DELETE ON qa_external_payment_intents FOR EACH ROW
BEGIN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA_PAYMENT_RETAINED_FOR_RECONCILIATION';
END//
DELIMITER ;
-- Do not add UNIQUE to legacy payments.TRANSID. No provider claim is authenticated by SQL alone.
