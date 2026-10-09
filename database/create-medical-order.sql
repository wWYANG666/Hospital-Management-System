-- 快速创建 medical_order 表
-- 请复制以下SQL语句到MySQL客户端执行

USE `yiyuan`;

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
