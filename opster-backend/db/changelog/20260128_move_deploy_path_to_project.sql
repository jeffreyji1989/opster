-- ============================================================================
-- 数据库迁移: 将部署路径从服务表移到项目表，删除项目表的监控地址
-- 日期: 2026-01-28
-- 说明:
--   1. 在 project 表添加 deploy_path 字段
--   2. 从 service 表迁移 deploy_path 数据到 project 表（每个项目的第一个服务的部署路径）
--   3. 删除 service 表的 deploy_path 字段
--   4. 删除 project 表的 monitor_url 字段
-- ============================================================================

-- 步骤1: 在 project 表添加 deploy_path 字段
ALTER TABLE project ADD COLUMN deploy_path TEXT;

-- 步骤2: 从 service 表迁移部署路径数据到 project 表
-- 为每个项目设置其第一个服务的部署路径
UPDATE project
SET deploy_path = (
    SELECT s.deploy_path
    FROM service s
    WHERE s.project_id = project.id
    LIMIT 1
)
WHERE id IN (
    SELECT DISTINCT project_id FROM service WHERE deploy_path IS NOT NULL
);

-- 步骤3: 删除 service 表的 deploy_path 字段
ALTER TABLE service DROP COLUMN deploy_path;

-- 步骤4: 删除 project 表的 monitor_url 字段
ALTER TABLE project DROP COLUMN monitor_url;
