-- ============================================
-- 创建处方相关表
-- ============================================

USE `yiyuan`;

-- 1. 创建处方表
CREATE TABLE IF NOT EXISTS `prescription` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '处方ID',
    `appointment_id` BIGINT COMMENT '挂号ID',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
    `prescription_number` VARCHAR(50) NOT NULL UNIQUE COMMENT '处方编号',
    `status` ENUM('PENDING', 'DISPENSED', 'CANCELLED') DEFAULT 'PENDING' COMMENT '状态：待发药/已发药/已取消',
    `paid` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已付款',
    `paid_at` DATETIME COMMENT '付款时间',
    `collected_at` DATETIME COMMENT '取药确认时间（患者签字时间）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `dispensed_at` DATETIME COMMENT '发药时间',
    `dispensed_by` BIGINT COMMENT '发药人ID',
    `patient_signature` VARCHAR(100) COMMENT '患者签字确认',
    FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`patient_id`) REFERENCES `patient`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`id`) ON DELETE CASCADE,
    INDEX `idx_patient_id` (`patient_id`),
    INDEX `idx_doctor_id` (`doctor_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方表';

-- 2. 创建处方明细表
CREATE TABLE IF NOT EXISTS `prescription_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '明细ID',
    `prescription_id` BIGINT NOT NULL COMMENT '处方ID',
    `medicine_id` BIGINT NOT NULL COMMENT '药品ID',
    `medicine_name` VARCHAR(100) COMMENT '药品名称（冗余字段）',
    `specification` VARCHAR(100) COMMENT '规格',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `unit` VARCHAR(20) COMMENT '单位',
    `usage` VARCHAR(100) COMMENT '用法',  -- usage 是 MySQL 保留关键字，需要用反引号
    `dosage` VARCHAR(100) COMMENT '用量',
    `frequency` VARCHAR(100) COMMENT '频次',
    `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '单价',
    `total_price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '总价',
    `notes` VARCHAR(200) COMMENT '备注',
    FOREIGN KEY (`prescription_id`) REFERENCES `prescription`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`medicine_id`) REFERENCES `medicine`(`id`) ON DELETE CASCADE,
    INDEX `idx_prescription_id` (`prescription_id`),
    INDEX `idx_medicine_id` (`medicine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方明细表';
