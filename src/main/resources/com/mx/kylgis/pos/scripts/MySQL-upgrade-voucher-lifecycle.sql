-- KylGis POS - voucher lifecycle migration (MySQL / MariaDB)
-- NEXT RELEASE ONLY. Do not run on production without backup, LAB rehearsal and explicit authorization.
--
-- Mandatory preflight. Both queries must return zero rows before applying the DDL:
-- SELECT UPPER(TRIM(voucher_number)) AS normalized_voucher_number, COUNT(*) AS copies
-- FROM vouchers
-- GROUP BY UPPER(TRIM(voucher_number))
-- HAVING normalized_voucher_number IS NULL OR normalized_voucher_number = '' OR COUNT(*) > 1;
--
-- SELECT id, voucher_number
-- FROM vouchers
-- WHERE voucher_number IS NULL OR TRIM(voucher_number) = '';
--
-- Historical vouchers remain non-expiring because EXPIRES_AT defaults to NULL.

ALTER TABLE vouchers
    ADD COLUMN expires_at DATETIME NULL AFTER status;

ALTER TABLE vouchers
    MODIFY voucher_number VARCHAR(100) NOT NULL;

CREATE UNIQUE INDEX vouchers_number_uq
    ON vouchers (voucher_number);

-- Rollback (execute only as a controlled rollback, in reverse feature order):
-- DROP INDEX vouchers_number_uq ON vouchers;
-- ALTER TABLE vouchers MODIFY voucher_number VARCHAR(100) NULL;
-- ALTER TABLE vouchers DROP COLUMN expires_at;
