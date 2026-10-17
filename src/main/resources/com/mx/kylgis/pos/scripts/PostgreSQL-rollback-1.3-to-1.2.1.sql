-- KylGis POS 1.3 -> 1.2.1 guarded schema rollback (PostgreSQL)
-- ADMINISTRATIVE USE ONLY. Do not execute during normal startup.
-- This rollback is intentionally allowed only before any 1.3 data exists.

BEGIN;

DO $kylgis_rollback$
DECLARE
    v_count BIGINT;
    v_version_rows BIGINT;
BEGIN
    IF to_regclass('recipe_versions') IS NULL
       OR to_regclass('recipe_components') IS NULL
       OR to_regclass('product_uom_conversions') IS NULL
       OR to_regclass('inventory_transformations') IS NULL
       OR to_regclass('inventory_transformation_lines') IS NULL THEN
        RAISE EXCEPTION 'KylGis 1.3 rollback blocked: required 1.3 tables are missing.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM recipe_versions;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'KylGis 1.3 rollback blocked: recipe_versions is not empty.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM recipe_components;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'KylGis 1.3 rollback blocked: recipe_components is not empty.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM product_uom_conversions;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'KylGis 1.3 rollback blocked: product_uom_conversions is not empty.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM inventory_transformations;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'KylGis 1.3 rollback blocked: inventory_transformations is not empty.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM inventory_transformation_lines;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'KylGis 1.3 rollback blocked: inventory_transformation_lines is not empty.';
    END IF;

    SELECT COUNT(*) INTO v_version_rows FROM applications WHERE version = '1.3';
    IF v_version_rows <> 1 THEN
        RAISE EXCEPTION 'KylGis 1.3 rollback blocked: expected exactly one applications row at version 1.3.';
    END IF;
END
$kylgis_rollback$;

DROP TABLE inventory_transformation_lines;
DROP TABLE inventory_transformations;
DROP TABLE recipe_components;
DROP TABLE product_uom_conversions;
DROP TABLE recipe_versions;

UPDATE applications SET version = '1.2.1' WHERE version = '1.3';

COMMIT;
