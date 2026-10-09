-- 为 medical_record 表添加缺失的列（安全版本，先检查再添加）
USE `yiyuan`;

-- 检查并添加 hospitalization_id 列
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = 'yiyuan'
               AND TABLE_NAME = 'medical_record'
               AND COLUMN_NAME = 'hospitalization_id');
SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `medical_record` ADD COLUMN `hospitalization_id` BIGINT COMMENT ''住院ID'' AFTER `appointment_id`',
    'SELECT ''Column hospitalization_id already exists'' AS result');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 condition_update 列
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = 'yiyuan'
               AND TABLE_NAME = 'medical_record'
               AND COLUMN_NAME = 'condition_update');
SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `medical_record` ADD COLUMN `condition_update` TEXT COMMENT ''病情更新'' AFTER `medical_record_content`',
    'SELECT ''Column condition_update already exists'' AS result');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加外键约束（如果不存在）
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = 'yiyuan'
               AND TABLE_NAME = 'medical_record'
               AND CONSTRAINT_NAME = 'fk_medical_record_hospitalization');
SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `medical_record` ADD CONSTRAINT `fk_medical_record_hospitalization` FOREIGN KEY (`hospitalization_id`) REFERENCES `hospitalization`(`id`) ON DELETE SET NULL',
    'SELECT ''Foreign key fk_medical_record_hospitalization already exists'' AS result');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'SQL execution completed!' AS result;
