-- KylGis POS 1.3 -> 1.2.1 guarded schema rollback (MariaDB/MySQL)
-- ADMINISTRATIVE USE ONLY. Do not execute during normal startup.
-- This rollback is intentionally allowed only before any 1.3 data exists.

DELIMITER $$

DROP PROCEDURE IF EXISTS kylgis_guarded_rollback_13_121$$
CREATE PROCEDURE kylgis_guarded_rollback_13_121()
BEGIN
    DECLARE v_count BIGINT DEFAULT 0;
    DECLARE v_version_rows BIGINT DEFAULT 0;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'recipe_versions'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'recipe_components'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'product_uom_conversions'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'inventory_transformations'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'inventory_transformation_lines'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'KylGis 1.3 rollback blocked: required 1.3 tables are missing.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM recipe_versions;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'KylGis 1.3 rollback blocked: recipe_versions is not empty.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM recipe_components;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'KylGis 1.3 rollback blocked: recipe_components is not empty.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM product_uom_conversions;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'KylGis 1.3 rollback blocked: product_uom_conversions is not empty.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM inventory_transformations;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'KylGis 1.3 rollback blocked: inventory_transformations is not empty.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM inventory_transformation_lines;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'KylGis 1.3 rollback blocked: inventory_transformation_lines is not empty.';
    END IF;

    SELECT COUNT(*) INTO v_version_rows FROM applications WHERE version = '1.3';
    IF v_version_rows <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'KylGis 1.3 rollback blocked: expected exactly one applications row at version 1.3.';
    END IF;

    DROP TABLE inventory_transformation_lines;
    DROP TABLE inventory_transformations;
    DROP TABLE recipe_components;
    DROP TABLE product_uom_conversions;
    DROP TABLE recipe_versions;

    UPDATE applications SET version = '1.2.1' WHERE version = '1.3';
END$$

CALL kylgis_guarded_rollback_13_121()$$
DROP PROCEDURE kylgis_guarded_rollback_13_121$$

DELIMITER ;
