-- 为 service 表添加 project_path 字段
-- 用于支持同一Git仓库下的多子项目独立发版
-- 日期: 2026-01-25

ALTER TABLE service ADD COLUMN project_path VARCHAR(255);

COMMENT ON COLUMN service.project_path IS '项目路径（相对Git仓库的子目录路径），例如：opster-backend、opster-frontend';
