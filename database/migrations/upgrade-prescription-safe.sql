-- ============================================
-- 安全升级处方表：增加付款/取药确认字段（可重复执行）
-- ============================================

USE `yiyuan`;

-- paid
SET @col_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'prescription'
    AND COLUMN_NAME = 'paid'
);
SET @alter_sql := IF(@col_exists = 0,
  'ALTER TABLE `prescription` ADD COLUMN `paid` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否已付款''',
  'SELECT ''Column paid already exists'' AS info'
);
PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- paid_at
SET @col_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'prescription'
    AND COLUMN_NAME = 'paid_at'
);
SET @alter_sql := IF(@col_exists = 0,
  'ALTER TABLE `prescription` ADD COLUMN `paid_at` DATETIME NULL COMMENT ''付款时间''',
  'SELECT ''Column paid_at already exists'' AS info'
);
PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- collected_at
SET @col_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'prescription'
    AND COLUMN_NAME = 'collected_at'
);
SET @alter_sql := IF(@col_exists = 0,
  'ALTER TABLE `prescription` ADD COLUMN `collected_at` DATETIME NULL COMMENT ''取药确认时间''',
  'SELECT ''Column collected_at already exists'' AS info'
);
PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
