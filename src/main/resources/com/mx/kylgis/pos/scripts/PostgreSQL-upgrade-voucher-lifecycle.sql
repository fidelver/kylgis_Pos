-- KylGis POS - voucher lifecycle migration (PostgreSQL)
-- NEXT RELEASE ONLY. Do not run on production without backup, LAB rehearsal and explicit authorization.
--
-- Mandatory preflight. Both queries must return zero rows before applying the DDL:
-- SELECT UPPER(TRIM(voucher_number)) AS normalized_voucher_number, COUNT(*) AS copies
-- FROM vouchers
-- GROUP BY UPPER(TRIM(voucher_number))
-- HAVING UPPER(TRIM(voucher_number)) IS NULL OR UPPER(TRIM(voucher_number)) = '' OR COUNT(*) > 1;
--
-- SELECT id, voucher_number
-- FROM vouchers
-- WHERE voucher_number IS NULL OR BTRIM(voucher_number) = '';
--
-- Historical vouchers remain non-expiring because EXPIRES_AT defaults to NULL.

ALTER TABLE vouchers
    ADD COLUMN expires_at TIMESTAMP NULL;

ALTER TABLE vouchers
    ALTER COLUMN voucher_number SET NOT NULL;

ALTER TABLE vouchers
    ADD CONSTRAINT vouchers_number_uq UNIQUE (voucher_number);

-- Rollback (execute only as a controlled rollback, in reverse feature order):
-- ALTER TABLE vouchers DROP CONSTRAINT vouchers_number_uq;
-- ALTER TABLE vouchers ALTER COLUMN voucher_number DROP NOT NULL;
-- ALTER TABLE vouchers DROP COLUMN expires_at;
