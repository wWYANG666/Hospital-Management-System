-- ============================================
-- 创建药品入库相关表
-- ============================================

USE `yiyuan`;

-- 1. 创建药品库存批次表
CREATE TABLE IF NOT EXISTS `medicine_stock` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '库存ID',
    `medicine_id` BIGINT NOT NULL COMMENT '药品ID',
    `batch_number` VARCHAR(50) NOT NULL COMMENT '批次号',
    `production_date` DATE COMMENT '生产日期',
    `expiry_date` DATE NOT NULL COMMENT '有效期',
    `quantity` INT NOT NULL DEFAULT 0 COMMENT '数量',
    `purchase_price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '采购单价',
    `location` VARCHAR(50) DEFAULT '药房' COMMENT '存放位置（药房/住院药房）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-已用完，-1-已过期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`medicine_id`) REFERENCES `medicine`(`id`) ON DELETE CASCADE,
    INDEX `idx_medicine_id` (`medicine_id`),
    INDEX `idx_batch_number` (`batch_number`),
    INDEX `idx_expiry_date` (`expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品库存批次表';

-- 2. 创建药品入库表
CREATE TABLE IF NOT EXISTS `medicine_inbound` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '入库ID',
    `medicine_id` BIGINT NOT NULL COMMENT '药品ID',
    `batch_number` VARCHAR(50) NOT NULL COMMENT '批次号',
    `production_date` DATE COMMENT '生产日期',
    `expiry_date` DATE NOT NULL COMMENT '有效期',
    `quantity` INT NOT NULL COMMENT '数量',
    `purchase_price` DECIMAL(10,2) NOT NULL COMMENT '采购单价',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `location` VARCHAR(50) DEFAULT '药房' COMMENT '存放位置',
    `operator` VARCHAR(50) COMMENT '操作人',
    `inbound_date` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待审核，1-已审核，-1-已取消',
    FOREIGN KEY (`medicine_id`) REFERENCES `medicine`(`id`) ON DELETE CASCADE,
    INDEX `idx_medicine_id` (`medicine_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_inbound_date` (`inbound_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品入库表';
