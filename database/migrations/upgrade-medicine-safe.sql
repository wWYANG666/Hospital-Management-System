-- ============================================
-- medicine 表升级脚本（安全版，兼容 MySQL 5.7+）
-- 目标：补齐药品新增/编辑所需字段
-- - purchase_price  采购价
-- - min_stock_alert 最小库存预警值
-- - manufacturer    生产厂家
-- - approval_number 批准文号
-- - insurance_category 医保类别
-- ============================================

USE `yiyuan`;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_medicine_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_medicine_column_if_missing(IN col_name VARCHAR(64), IN alter_sql LONGTEXT CHARACTER SET utf8mb4)
BEGIN
    DECLARE col_cnt INT DEFAULT 0;
    SELECT COUNT(*)
      INTO col_cnt
      FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'medicine'
       AND COLUMN_NAME = col_name;

    IF col_cnt = 0 THEN
        SET @s = alter_sql;
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_medicine_column_if_missing(
  'purchase_price',
  'ALTER TABLE `medicine` ADD COLUMN `purchase_price` DECIMAL(10,2) DEFAULT 0.00 COMMENT ''purchase price'' AFTER `price`'
);

CALL add_medicine_column_if_missing(
  'min_stock_alert',
  'ALTER TABLE `medicine` ADD COLUMN `min_stock_alert` INT DEFAULT 10 COMMENT ''min stock alert'' AFTER `stock`'
);

CALL add_medicine_column_if_missing(
  'manufacturer',
  'ALTER TABLE `medicine` ADD COLUMN `manufacturer` VARCHAR(200) COMMENT ''manufacturer'' AFTER `specification`'
);

CALL add_medicine_column_if_missing(
  'approval_number',
  'ALTER TABLE `medicine` ADD COLUMN `approval_number` VARCHAR(100) COMMENT ''approval number'' AFTER `manufacturer`'
);

CALL add_medicine_column_if_missing(
  'insurance_category',
  'ALTER TABLE `medicine` ADD COLUMN `insurance_category` VARCHAR(20) COMMENT ''insurance category'' AFTER `type`'
);

DROP PROCEDURE IF EXISTS add_medicine_column_if_missing;

SELECT 'medicine upgrade done' AS message;
DESCRIBE `medicine`;
