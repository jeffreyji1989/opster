-- ============================================================================
-- 修改 project 表结构
-- 创建日期: 2026-02-13
-- 说明:
--   1. 重命名 deploy_path 为 deploy_root_path
--   2. 删除 repositories、git_username、git_password 字段
-- 注意: SQLite 不支持 DROP COLUMN 和 ALTER COLUMN，需要重建表
-- ============================================================================

-- 步骤 1: 创建新的 project 表结构（不包含要删除的字段）
CREATE TABLE IF NOT EXISTS project_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 项目基本信息: project_code(项目编号), project_name(项目名称), project_owner(项目负责人)
    project_code TEXT,
    project_name TEXT NOT NULL,
    project_owner TEXT,

    -- 部署根目录（原 deploy_path 重命名）
    deploy_root_path TEXT,

    -- 业务信息: business_line(业务线名称)
    business_line TEXT,

    -- 状态: 0-禁用 1-启用
    status INTEGER DEFAULT 1,

    -- 审计字段
    create_by INTEGER DEFAULT 1,
    create_by_name TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INTEGER DEFAULT 1,
    update_by_name TEXT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    del_flag INTEGER DEFAULT 0  -- 删除标志: 0-未删除 1-已删除
);

-- 步骤 2: 迁移数据（排除要删除的字段，deploy_path 重命名为 deploy_root_path）
INSERT INTO project_new (
    id,
    project_code,
    project_name,
    project_owner,
    deploy_root_path,  -- 原 deploy_path
    business_line,
    status,
    create_by,
    create_by_name,
    create_time,
    update_by,
    update_by_name,
    update_time,
    del_flag
)
SELECT
    id,
    project_code,
    project_name,
    project_owner,
    deploy_path,  -- 迁移到新字段名
    business_line,
    status,
    create_by,
    create_by_name,
    create_time,
    update_by,
    update_by_name,
    update_time,
    del_flag
FROM project;

-- 步骤 3: 删除旧表
DROP TABLE IF EXISTS project;

-- 步骤 4: 重命名新表为 project
ALTER TABLE project_new RENAME TO project;

-- 步骤 5: 重新创建索引（如果存在）
CREATE INDEX IF NOT EXISTS idx_project_status ON project(status);
CREATE INDEX IF NOT EXISTS idx_project_del_flag ON project(del_flag);

-- 说明: repositories、git_username、git_password 字段已被删除
-- 说明: deploy_path 字段已重命名为 deploy_root_path
