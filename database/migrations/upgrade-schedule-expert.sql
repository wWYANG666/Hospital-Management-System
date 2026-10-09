-- 为排班表新增门诊类型字段，支持普通门诊和专家诊
ALTER TABLE schedule
ADD COLUMN schedule_type ENUM('GENERAL', 'EXPERT') NOT NULL DEFAULT 'GENERAL' COMMENT '门诊类型：普通门诊/专家诊'
AFTER work_time;
