-- ============================================
-- 住院系统功能扩展脚本
-- 用于扩展现有数据库表结构
-- 注意：请先备份数据库，然后执行此脚本
-- ============================================

USE `yiyuan`;

-- 1. 扩展住院信息表，添加申请相关字段
-- 注意：如果字段已存在会报错，可以忽略或手动删除已存在的字段后重新执行

ALTER TABLE `hospitalization`
ADD COLUMN `admission_reason` TEXT COMMENT '住院原因' AFTER `diagnosis`;

ALTER TABLE `hospitalization`
ADD COLUMN `expected_days` INT COMMENT '预计住院天数' AFTER `admission_reason`;

ALTER TABLE `hospitalization`
ADD COLUMN `request_department_id` BIGINT COMMENT '申请科室ID' AFTER `expected_days`;

ALTER TABLE `hospitalization`
ADD COLUMN `request_time` DATETIME COMMENT '申请时间' AFTER `request_department_id`;

ALTER TABLE `hospitalization`
ADD COLUMN `request_status` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING' COMMENT '申请状态：待审批/已批准/已拒绝' AFTER `request_time`;

ALTER TABLE `hospitalization`
ADD COLUMN `discharge_diagnosis` TEXT COMMENT '出院诊断' AFTER `discharge_date`;

ALTER TABLE `hospitalization`
ADD COLUMN `discharge_notes` TEXT COMMENT '出院注意事项' AFTER `discharge_diagnosis`;

-- 添加外键约束（如果不存在）
ALTER TABLE `hospitalization`
ADD CONSTRAINT `fk_hospitalization_request_dept` FOREIGN KEY (`request_department_id`) REFERENCES `department`(`id`) ON DELETE SET NULL;

-- 2. 扩展病床信息表，添加病区字段
ALTER TABLE `bed`
ADD COLUMN `ward` VARCHAR(50) COMMENT '病区（如：内科病房、外科病房、妇产科病房）' AFTER `room_number`;

-- 3. 创建医嘱表
CREATE TABLE IF NOT EXISTS `medical_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '医嘱ID',
    `hospitalization_id` BIGINT NOT NULL COMMENT '住院ID',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `doctor_id` BIGINT COMMENT '医生ID',
    `order_type` ENUM('MEDICATION', 'EXAMINATION', 'NURSING', 'OTHER') NOT NULL COMMENT '医嘱类型：用药/检查/护理/其他',
    `order_content` TEXT NOT NULL COMMENT '医嘱内容',
    `medicine_id` BIGINT COMMENT '药品ID（如果是用药医嘱）',
    `quantity` INT NULL COMMENT '药品数量（用药医嘱）',
    `examination_id` BIGINT COMMENT '检查项目ID（如果是检查医嘱）',
    `dosage` VARCHAR(100) COMMENT '用法用量',
    `frequency` VARCHAR(50) COMMENT '频次',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `status` ENUM('ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'ACTIVE' COMMENT '状态：执行中/已完成/已取消',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`hospitalization_id`) REFERENCES `hospitalization`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`patient_id`) REFERENCES `patient`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`medicine_id`) REFERENCES `medicine`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`examination_id`) REFERENCES `examination`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医嘱表';

-- 4. 扩展病历记录表，添加住院关联字段（如果表不存在则创建）
CREATE TABLE IF NOT EXISTS `medical_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '病历ID',
    `appointment_id` BIGINT COMMENT '挂号ID',
    `hospitalization_id` BIGINT COMMENT '住院ID',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `doctor_id` BIGINT COMMENT '医生ID',
    `diagnosis` TEXT COMMENT '诊断',
    `prescription` TEXT COMMENT '处方',
    `examination_items` VARCHAR(500) COMMENT '检查项目',
    `medical_record_content` TEXT COMMENT '病历内容',
    `condition_update` TEXT COMMENT '病情更新',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`hospitalization_id`) REFERENCES `hospitalization`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`patient_id`) REFERENCES `patient`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子病历表';

-- 如果表已存在，添加住院关联字段（如果字段不存在）
-- 注意：如果字段已存在会报错，可以忽略
ALTER TABLE `medical_record`
ADD COLUMN `hospitalization_id` BIGINT COMMENT '住院ID' AFTER `appointment_id`;

ALTER TABLE `medical_record`
ADD COLUMN `condition_update` TEXT COMMENT '病情更新' AFTER `medical_record_content`;

-- 添加外键约束（如果不存在，如果已存在会报错，可以忽略）
ALTER TABLE `medical_record`
ADD CONSTRAINT `fk_medical_record_hospitalization` FOREIGN KEY (`hospitalization_id`) REFERENCES `hospitalization`(`id`) ON DELETE SET NULL;
