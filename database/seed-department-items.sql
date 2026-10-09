-- ============================================
-- 科室药品/检查项目种子数据（可重复执行，安全版）
-- 目标：为 内科、口腔科、外科 各添加 5 个药品 + 5 个检查项目
-- 说明：
-- - 若 medicine / examination 表缺少 department_id 列，会自动补齐
-- - 使用 INSERT IGNORE 避免重复插入（依赖 department.name 唯一、medicine.code 唯一、examination.name 唯一）
-- ============================================

USE `yiyuan`;
SET NAMES utf8mb4;

-- 1) 确保科室存在
INSERT IGNORE INTO `department` (`name`, `description`) VALUES
('内科', '内科门诊与常见内科疾病诊疗'),
('口腔科', '口腔疾病诊疗与牙周护理'),
('外科', '外科门诊与常见外科处置');

-- 2) 补齐 department_id 列（若缺失）
DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN alter_sql LONGTEXT CHARACTER SET utf8mb4)
BEGIN
    DECLARE col_cnt INT DEFAULT 0;
    SELECT COUNT(*)
      INTO col_cnt
      FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = tbl
       AND COLUMN_NAME = col;

    IF col_cnt = 0 THEN
        SET @s = alter_sql;
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('medicine', 'department_id',
  'ALTER TABLE `medicine` ADD COLUMN `department_id` BIGINT NULL COMMENT ''所属科室ID（NULL=全院通用）'' AFTER `status`'
);
CALL add_column_if_missing('examination', 'department_id',
  'ALTER TABLE `examination` ADD COLUMN `department_id` BIGINT NULL COMMENT ''所属科室ID'' AFTER `status`'
);

-- 外键（若你希望强约束可打开；默认不强制，避免旧库失败）
-- CALL add_column_if_missing('medicine', 'fk_medicine_department_dummy',
--   'ALTER TABLE `medicine` ADD CONSTRAINT `fk_medicine_department` FOREIGN KEY (`department_id`) REFERENCES `department`(`id`) ON DELETE SET NULL'
-- );
-- CALL add_column_if_missing('examination', 'fk_exam_department_dummy',
--   'ALTER TABLE `examination` ADD CONSTRAINT `fk_examination_department` FOREIGN KEY (`department_id`) REFERENCES `department`(`id`) ON DELETE SET NULL'
-- );

DROP PROCEDURE IF EXISTS add_column_if_missing;

-- 3) 内科：药品（5）
INSERT IGNORE INTO `medicine`
(`name`, `code`, `type`, `specification`, `unit`, `price`, `stock`, `description`, `status`, `department_id`)
VALUES
('阿莫西林胶囊', 'IM-MED-001', '西药', '0.5g*24粒', '盒', 18.00, 100, '抗菌药物（示例）', 1, (SELECT id FROM `department` WHERE name='内科' LIMIT 1)),
('对乙酰氨基酚片', 'IM-MED-002', '西药', '0.5g*20片', '盒', 8.00, 120, '解热镇痛（示例）', 1, (SELECT id FROM `department` WHERE name='内科' LIMIT 1)),
('奥美拉唑肠溶胶囊', 'IM-MED-003', '西药', '20mg*14粒', '盒', 22.00, 80, '抑酸护胃（示例）', 1, (SELECT id FROM `department` WHERE name='内科' LIMIT 1)),
('氯沙坦钾片', 'IM-MED-004', '西药', '50mg*14片', '盒', 28.00, 70, '降压（示例）', 1, (SELECT id FROM `department` WHERE name='内科' LIMIT 1)),
('二甲双胍片', 'IM-MED-005', '西药', '0.5g*30片', '盒', 15.00, 60, '降糖（示例）', 1, (SELECT id FROM `department` WHERE name='内科' LIMIT 1));

-- 4) 内科：检查项目（5）
INSERT IGNORE INTO `examination`
(`name`, `type`, `description`, `price`, `status`, `department_id`)
VALUES
('血常规', '检验', '常规血液细胞计数', 25.00, 1, (SELECT id FROM `department` WHERE name='内科' LIMIT 1)),
('尿常规', '检验', '尿液常规检查', 20.00, 1, (SELECT id FROM `department` WHERE name='内科' LIMIT 1)),
('肝功能', '检验', 'ALT/AST 等肝功能指标', 60.00, 1, (SELECT id FROM `department` WHERE name='内科' LIMIT 1)),
('心电图', '检查', '12 导联心电图检查', 35.00, 1, (SELECT id FROM `department` WHERE name='内科' LIMIT 1)),
('胸部X线', '影像', '胸片检查（示例）', 60.00, 1, (SELECT id FROM `department` WHERE name='内科' LIMIT 1));

-- 5) 口腔科：药品（5）
INSERT IGNORE INTO `medicine`
(`name`, `code`, `type`, `specification`, `unit`, `price`, `stock`, `description`, `status`, `department_id`)
VALUES
('氯己定含漱液', 'ST-MED-001', '西药', '200ml', '瓶', 18.00, 60, '口腔消毒含漱（示例）', 1, (SELECT id FROM `department` WHERE name='口腔科' LIMIT 1)),
('甲硝唑片', 'ST-MED-002', '西药', '0.2g*24片', '盒', 12.00, 80, '口腔感染相关（示例）', 1, (SELECT id FROM `department` WHERE name='口腔科' LIMIT 1)),
('布洛芬缓释胶囊', 'ST-MED-003', '西药', '0.3g*10粒', '盒', 16.00, 80, '镇痛消炎（示例）', 1, (SELECT id FROM `department` WHERE name='口腔科' LIMIT 1)),
('复方氯化钠喷雾', 'ST-MED-004', '西药', '30ml', '瓶', 25.00, 40, '口咽局部护理（示例）', 1, (SELECT id FROM `department` WHERE name='口腔科' LIMIT 1)),
('复方维生素B片', 'ST-MED-005', '西药', '100片', '瓶', 22.00, 50, '口腔溃疡辅助（示例）', 1, (SELECT id FROM `department` WHERE name='口腔科' LIMIT 1));

-- 6) 口腔科：检查项目（5）
INSERT IGNORE INTO `examination`
(`name`, `type`, `description`, `price`, `status`, `department_id`)
VALUES
('口腔全景片（OPG）', '影像', '口腔全景X线片（示例）', 80.00, 1, (SELECT id FROM `department` WHERE name='口腔科' LIMIT 1)),
('牙周探诊评估', '检查', '牙周袋深度/出血指数评估', 50.00, 1, (SELECT id FROM `department` WHERE name='口腔科' LIMIT 1)),
('龋齿风险评估', '检查', '龋风险问卷与口内检查', 30.00, 1, (SELECT id FROM `department` WHERE name='口腔科' LIMIT 1)),
('口腔黏膜检查', '检查', '口腔黏膜病变筛查（示例）', 40.00, 1, (SELECT id FROM `department` WHERE name='口腔科' LIMIT 1)),
('口腔局部X线片', '影像', '牙片/根尖片（示例）', 35.00, 1, (SELECT id FROM `department` WHERE name='口腔科' LIMIT 1));

-- 7) 外科：药品（5）
INSERT IGNORE INTO `medicine`
(`name`, `code`, `type`, `specification`, `unit`, `price`, `stock`, `description`, `status`, `department_id`)
VALUES
('头孢呋辛酯片', 'SU-MED-001', '西药', '0.25g*12片', '盒', 38.00, 60, '抗菌药物（示例）', 1, (SELECT id FROM `department` WHERE name='外科' LIMIT 1)),
('碘伏消毒液', 'SU-MED-002', '西药', '100ml', '瓶', 10.00, 120, '皮肤消毒（示例）', 1, (SELECT id FROM `department` WHERE name='外科' LIMIT 1)),
('创可贴（医用）', 'SU-MED-003', '医疗耗材', '10片/盒', '盒', 8.00, 200, '小伤口保护（示例）', 1, (SELECT id FROM `department` WHERE name='外科' LIMIT 1)),
('双氯芬酸钠凝胶', 'SU-MED-004', '西药', '20g', '支', 20.00, 80, '局部消炎镇痛（示例）', 1, (SELECT id FROM `department` WHERE name='外科' LIMIT 1)),
('破伤风抗毒素（TAT）', 'SU-MED-005', '西药', '1500IU', '支', 55.00, 20, '外伤破伤风预防（示例）', 1, (SELECT id FROM `department` WHERE name='外科' LIMIT 1));

-- 8) 外科：检查项目（5）
INSERT IGNORE INTO `examination`
(`name`, `type`, `description`, `price`, `status`, `department_id`)
VALUES
('伤口清创评估', '检查', '伤口污染程度与处理方案评估', 60.00, 1, (SELECT id FROM `department` WHERE name='外科' LIMIT 1)),
('腹部彩超', '影像', '腹部超声检查（示例）', 120.00, 1, (SELECT id FROM `department` WHERE name='外科' LIMIT 1)),
('腹部CT（平扫）', '影像', '腹部CT检查（示例）', 280.00, 1, (SELECT id FROM `department` WHERE name='外科' LIMIT 1)),
('凝血功能', '检验', 'PT/APTT/INR 等凝血指标', 80.00, 1, (SELECT id FROM `department` WHERE name='外科' LIMIT 1)),
('术前常规检查包', '检查', '术前评估（示例组合项）', 150.00, 1, (SELECT id FROM `department` WHERE name='外科' LIMIT 1));

SELECT 'seed department items done' AS message;
