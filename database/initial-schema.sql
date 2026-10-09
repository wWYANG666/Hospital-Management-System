-- ============================================
-- 数据库初始化脚本
-- 用于手动创建数据库和表结构
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `yiyuan`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `yiyuan`;

-- 用户表（患者、医生、管理员）
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `role` ENUM('PATIENT', 'DOCTOR', 'ADMIN') NOT NULL COMMENT '角色：患者/医生/管理员',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 医生信息表
CREATE TABLE IF NOT EXISTS `doctor` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '医生ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `doctor_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '医生工号',
    `department` VARCHAR(50) COMMENT '科室',
    `title` VARCHAR(50) COMMENT '职称',
    `specialty` VARCHAR(200) COMMENT '专长',
    `introduction` TEXT COMMENT '简介',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生信息表';

-- 患者信息表
CREATE TABLE IF NOT EXISTS `patient` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '患者ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `patient_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '患者编号',
    `id_card` VARCHAR(18) COMMENT '身份证号',
    `gender` ENUM('MALE', 'FEMALE') COMMENT '性别',
    `birthday` DATE COMMENT '生日',
    `address` VARCHAR(200) COMMENT '地址',
    `emergency_contact` VARCHAR(50) COMMENT '紧急联系人',
    `emergency_phone` VARCHAR(20) COMMENT '紧急联系电话',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者信息表';

-- 科室表
CREATE TABLE IF NOT EXISTS `department` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '科室ID',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '科室名称',
    `description` VARCHAR(200) COMMENT '科室描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室表';

-- 排班信息表
CREATE TABLE IF NOT EXISTS `schedule` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '排班ID',
    `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
    `department_id` BIGINT COMMENT '科室ID',
    `work_date` DATE NOT NULL COMMENT '工作日期',
    `work_time` ENUM('MORNING', 'AFTERNOON', 'EVENING') NOT NULL COMMENT '工作时间：上午/下午/晚上',
    `schedule_type` ENUM('GENERAL', 'EXPERT') DEFAULT 'GENERAL' COMMENT '门诊类型：普通门诊/专家诊',
    `max_appointments` INT DEFAULT 20 COMMENT '最大预约数',
    `current_appointments` INT DEFAULT 0 COMMENT '当前预约数',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-可用，0-不可用',
    FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`department_id`) REFERENCES `department`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班信息表';

-- 挂号预约表
CREATE TABLE IF NOT EXISTS `appointment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '挂号ID',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
    `schedule_id` BIGINT COMMENT '排班ID',
    `appointment_date` DATE NOT NULL COMMENT '预约日期',
    `appointment_time` TIME COMMENT '预约时间',
    `status` ENUM('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING' COMMENT '状态：待确认/已确认/已完成/已取消',
    `symptoms` VARCHAR(500) COMMENT '症状描述',
    `diagnosis` TEXT COMMENT '诊断结果',
    `prescription` TEXT COMMENT '处方',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`patient_id`) REFERENCES `patient`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`schedule_id`) REFERENCES `schedule`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='挂号预约表';

-- 检查项目表
CREATE TABLE IF NOT EXISTS `examination` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '检查项目ID',
    `name` VARCHAR(100) NOT NULL COMMENT '检查项目名称',
    `type` VARCHAR(50) COMMENT '检查类型',
    `description` VARCHAR(500) COMMENT '项目描述',
    `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '价格',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-可用，0-不可用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查项目表';

-- 报告单表
CREATE TABLE IF NOT EXISTS `report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '报告单ID',
    `appointment_id` BIGINT COMMENT '挂号ID',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `doctor_id` BIGINT COMMENT '医生ID',
    `examination_id` BIGINT COMMENT '检查项目ID',
    `report_type` VARCHAR(50) COMMENT '报告类型',
    `report_content` TEXT COMMENT '报告内容',
    `report_date` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报告日期',
    `status` ENUM('PENDING', 'COMPLETED') DEFAULT 'PENDING' COMMENT '状态：待完成/已完成',
    `paid` TINYINT(1) NULL DEFAULT NULL COMMENT '患者是否已缴检查费（审阅前需为已缴）',
    `paid_at` DATETIME NULL DEFAULT NULL COMMENT '检查费缴费时间',
    FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`patient_id`) REFERENCES `patient`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`examination_id`) REFERENCES `examination`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告单表';

-- 药物信息表
CREATE TABLE IF NOT EXISTS `medicine` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '药物ID',
    `name` VARCHAR(100) NOT NULL COMMENT '药物名称',
    `code` VARCHAR(50) UNIQUE COMMENT '药物编码',
    `type` VARCHAR(50) COMMENT '药物类型',
    `specification` VARCHAR(100) COMMENT '规格',
    `unit` VARCHAR(20) COMMENT '单位',
    `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '价格',
    `stock` INT DEFAULT 0 COMMENT '库存',
    `description` VARCHAR(500) COMMENT '描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-可用，0-不可用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药物信息表';

-- 病床信息表
CREATE TABLE IF NOT EXISTS `bed` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '病床ID',
    `bed_number` VARCHAR(50) NOT NULL UNIQUE COMMENT '病床号',
    `room_number` VARCHAR(50) COMMENT '房间号',
    `department_id` BIGINT COMMENT '科室ID',
    `bed_type` ENUM('GENERAL', 'VIP', 'ICU') DEFAULT 'GENERAL' COMMENT '病床类型：普通/VIP/ICU',
    `status` ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') DEFAULT 'AVAILABLE' COMMENT '状态：可用/占用/维护中',
    `price_per_day` DECIMAL(10,2) DEFAULT 0.00 COMMENT '每日价格',
    FOREIGN KEY (`department_id`) REFERENCES `department`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病床信息表';

-- 住院信息表
CREATE TABLE IF NOT EXISTS `hospitalization` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '住院ID',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `doctor_id` BIGINT COMMENT '主治医生ID',
    `bed_id` BIGINT COMMENT '病床ID',
    `admission_date` DATE NOT NULL COMMENT '入院日期',
    `discharge_date` DATE COMMENT '出院日期',
    `diagnosis` TEXT COMMENT '诊断',
    `status` ENUM('ADMITTED', 'DISCHARGED') DEFAULT 'ADMITTED' COMMENT '状态：住院中/已出院',
    `total_cost` DECIMAL(10,2) DEFAULT 0.00 COMMENT '总费用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`patient_id`) REFERENCES `patient`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`bed_id`) REFERENCES `bed`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='住院信息表';

-- AI 生成审计日志表
CREATE TABLE IF NOT EXISTS `ai_generation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `who_user` VARCHAR(100) NOT NULL COMMENT '触发人（如 doctor:zhangsan）',
    `where_scene` VARCHAR(100) NOT NULL COMMENT '触发场景（doctor.diagnosis/patient.triage/admin.summary）',
    `input_payload` TEXT COMMENT '输入参数（JSON）',
    `output_text` TEXT COMMENT 'AI 输出文本',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 生成审计日志';
