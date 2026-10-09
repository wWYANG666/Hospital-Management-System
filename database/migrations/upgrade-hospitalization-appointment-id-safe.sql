-- 为 hospitalization 表添加 appointment_id（安全：列已存在则跳过）
-- 用于住院与发起住院时的挂号精确关联，医生端住院管理展示诊断用
-- 说明：部分环境未执行过 upgrade-hospitalization.sql，可能没有 request_status 列，
--       因此不强制 AFTER request_status，避免 ALTER 失败。
USE `yiyuan`;

SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'hospitalization'
               AND COLUMN_NAME = 'appointment_id');
SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `hospitalization` ADD COLUMN `appointment_id` BIGINT NULL COMMENT ''来源挂号ID''',
    'SELECT ''Column appointment_id already exists'' AS result');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 外键（可选；若 appointment 表名/库不一致请手动调整）
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'hospitalization'
               AND CONSTRAINT_NAME = 'fk_hospitalization_appointment');
SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `hospitalization` ADD CONSTRAINT `fk_hospitalization_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`id`) ON DELETE SET NULL',
    'SELECT ''Foreign key fk_hospitalization_appointment already exists'' AS result');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'upgrade-hospitalization-appointment-id completed' AS result;
