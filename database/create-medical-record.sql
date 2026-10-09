USE `yiyuan`;

CREATE TABLE IF NOT EXISTS `medical_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `appointment_id` BIGINT NULL,
    `hospitalization_id` BIGINT NULL,
    `patient_id` BIGINT NOT NULL,
    `doctor_id` BIGINT NULL,
    `diagnosis` TEXT,
    `prescription` TEXT,
    `examination_items` VARCHAR(500),
    `medical_record_content` TEXT,
    `condition_update` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`hospitalization_id`) REFERENCES `hospitalization`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`patient_id`) REFERENCES `patient`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
