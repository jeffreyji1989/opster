-- 添加项目编号字段
-- 日期: 2026-01-24
-- 描述: 为项目表添加项目编号字段，用于唯一标识项目

ALTER TABLE project ADD COLUMN project_code VARCHAR(50) DEFAULT NULL;
COMMENT ON COLUMN project.project_code IS '项目编号';
