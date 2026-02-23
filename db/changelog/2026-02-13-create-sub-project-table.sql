-- ============================================================================
-- 子项目管理表
-- 创建日期: 2026-02-13
-- 说明: 支持一个项目包含多个子项目（前端、后端、移动端等），每个子项目独立配置
-- ============================================================================

-- 创建 sub_project 表
CREATE TABLE IF NOT EXISTS sub_project (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    -- ========== 基本信息 ==========
    -- 关联项目 ID（应用层关联，无外键约束）
    project_id INTEGER NOT NULL,
    -- 子项目名称
    sub_project_name TEXT NOT NULL,
    -- 项目类型: frontend/backend/mobile/admin
    project_type TEXT NOT NULL,
    -- 服务别名（用于构建部署目录）
    service_alias TEXT,

    -- ========== Git 仓库信息 ==========
    -- 关联 Git 账号 ID（应用层关联，无外键约束）
    git_account_id INTEGER,
    -- Git 仓库地址
    git_url TEXT NOT NULL,
    -- Git 分支名称
    git_branch TEXT NOT NULL,
    -- 项目相对路径（相对 Git 仓库根目录）
    project_path TEXT,

    -- ========== 部署配置 ==========
    -- 部署服务器 ID（应用层关联，无外键约束）
    server_id INTEGER NOT NULL,
    -- 部署路径
    deploy_path TEXT NOT NULL,
    -- 服务端口号
    port INTEGER,

    -- ========== 构建配置 ==========
    -- 构建命令（支持多行命令）
    build_command TEXT,
    -- 运行版本（Node 版本或 JDK 版本）
    run_version TEXT,

    -- ========== 配置文件（JSON 格式存储）==========
    -- 存储格式: [{"filename":"application.yml","content":"server:\n  port: 8080"},{"filename":"config.properties","content":"app.name=opster"}]
    config_files TEXT,

    -- ========== 执行脚本 ==========
    -- 启动脚本（支持多行命令）
    start_script TEXT,
    -- 停止脚本（支持多行命令）
    stop_script TEXT,
    -- 重启脚本（支持多行命令）
    restart_script TEXT,

    -- ========== 监控配置 ==========
    -- 监控地址
    monitor_url TEXT,
    -- 日志路径
    log_path TEXT,

    -- ========== 状态信息 ==========
    -- 运行状态: 0-未启动 1-正常 2-异常
    run_status INTEGER DEFAULT 0,
    -- 启用状态: 0-禁用 1-启用
    status INTEGER DEFAULT 1,
    -- 上次部署时间
    last_deploy_time DATETIME,

    -- ========== 审计字段 ==========
    create_by INTEGER DEFAULT 1,
    create_by_name TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INTEGER DEFAULT 1,
    update_by_name TEXT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    del_flag INTEGER DEFAULT 0  -- 删除标志: 0-未删除 1-已删除
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_sub_project_project ON sub_project(project_id);
CREATE INDEX IF NOT EXISTS idx_sub_project_server ON sub_project(server_id);
CREATE INDEX IF NOT EXISTS idx_sub_project_git_account ON sub_project(git_account_id);
CREATE INDEX IF NOT EXISTS idx_sub_project_type ON sub_project(project_type);
CREATE INDEX IF NOT EXISTS idx_sub_project_status ON sub_project(status);
CREATE INDEX IF NOT EXISTS idx_sub_project_del_flag ON sub_project(del_flag);
