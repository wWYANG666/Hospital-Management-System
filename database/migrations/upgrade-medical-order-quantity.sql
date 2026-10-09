-- 医嘱表增加药品数量（住院用药医嘱）
-- 若列已存在会报错，可忽略后一条

USE `yiyuan`;

ALTER TABLE `medical_order`
    ADD COLUMN `quantity` INT NULL DEFAULT NULL COMMENT '药品数量（用药医嘱）' AFTER `medicine_id`;
