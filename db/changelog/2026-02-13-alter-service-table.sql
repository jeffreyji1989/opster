-- ============================================================================
-- 修改 service 表结构
-- 创建日期: 2026-02-13
-- 说明:
--   1. 新增 sub_project_id 字段（关联子项目）
--   2. 删除以下字段（已迁移到 sub_project 表）:
--      - repo_git_url, git_branch, maven_cmd, build_cmd
--      - project_path, repository_type, node_version, service_alias
-- 注意: SQLite 不支持 DROP COLUMN，需要重建表
-- ============================================================================

-- 步骤 1: 创建新的 service 表结构（新增 sub_project_id，删除已迁移字段）
CREATE TABLE IF NOT EXISTS service_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 关联字段: server_id(服务器ID), project_id(项目ID), sub_project_id(子项目ID)
    server_id INTEGER NOT NULL,
    project_id INTEGER NOT NULL,
    sub_project_id,

    -- 基本配置（保留）: env(环境), port(端口号), log_path(日志路径)
    env TEXT,
    port INTEGER,
    log_path TEXT,

    -- 状态信息（保留）: run_status(运行状态: 0-未启动 1-正常 2-异常), status(启用: 0-禁用 1-启用)
    run_status INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,

    -- 监控信息（保留）: monitor_url(监控地址)
    monitor_url TEXT,

    -- 启动脚本（保留）: start_script(服务启动脚本)
    start_script TEXT,

    -- 脚本上传状态（保留）: script_uploaded(0-未上传 1-已上传)
    script_uploaded INTEGER DEFAULT 0,

    -- 部署时间（保留）: last_deploy_time(上次发版时间)
    last_deploy_time DATETIME,

    -- 审计字段
    create_by INTEGER DEFAULT 1,
    create_by_name TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INTEGER DEFAULT 1,
    update_by_name TEXT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    del_flag INTEGER DEFAULT 0  -- 删除标志: 0-未删除 1-已删除
);

-- 步骤 2: 迁移数据（排除要删除的字段）
INSERT INTO service_new (
    id,
    server_id,
    project_id,
    sub_project_id,  -- 新字段，初始为 NULL
    env,
    port,
    log_path,
    run_status,
    status,
    monitor_url,
    start_script,
    script_uploaded,
    last_deploy_time,
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
    server_id,
    project_id,
    NULL,  -- 新字段 sub_project_id 初始为 NULL
    env,
    port,
    log_path,
    run_status,
    status,
    monitor_url,
    start_script,
    script_uploaded,
    last_deploy_time,
    create_by,
    create_by_name,
    create_time,
    update_by,
    update_by_name,
    update_time,
    del_flag
FROM service;

-- 步骤 3: 删除旧表
DROP TABLE IF EXISTS service;

-- 步骤 4: 重命名新表为 service
ALTER TABLE service_new RENAME TO service;

-- 步骤 5: 重新创建索引（如果存在）
CREATE INDEX IF NOT EXISTS idx_service_server_id ON service(server_id);
CREATE INDEX IF NOT EXISTS idx_service_project_id ON service(project_id);
CREATE INDEX IF NOT EXISTS idx_service_sub_project_id ON service(sub_project_id);
CREATE INDEX IF NOT EXISTS idx_service_status ON service(status);
CREATE INDEX IF NOT EXISTS idx_service_del_flag ON service(del_flag);

-- 说明: 以下字段已被删除（迁移到 sub_project 表）:
--   - repo_git_url: Git 仓库地址 → sub_project.git_url
--   - git_branch: Git 分支 → sub_project.git_branch
--   - maven_cmd: Maven 命令 → sub_project.build_command
--   - build_cmd: 构建命令 → sub_project.build_command
--   - project_path: 项目路径 → sub_project.project_path
--   - repository_type: 仓库类型 → sub_project.project_type
--   - node_version: Node 版本 → sub_project.run_version
--   - service_alias: 服务别名 → sub_project.service_alias
-- 说明: 新增 sub_project_id 字段用于关联 sub_project 表
