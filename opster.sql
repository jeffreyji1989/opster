-- 项目表
CREATE TABLE IF NOT EXISTS project (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_name TEXT NOT NULL COMMENT '项目名称',
    project_owner TEXT COMMENT '项目负责人',
    git_url TEXT COMMENT '项目git地址',
    monitor_url TEXT COMMENT '项目监控地址',
    business_line TEXT COMMENT '业务线名称',
    status INTEGER DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_by INTEGER DEFAULT 1,
    create_by_name TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INTEGER DEFAULT 1,
    update_by_name TEXT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 服务器表
CREATE TABLE IF NOT EXISTS server (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ip TEXT NOT NULL COMMENT 'IP地址',
    alias TEXT COMMENT '别名',
    username TEXT COMMENT '用户名',
    password TEXT COMMENT '密码',
    group_name TEXT COMMENT '分组',
    env TEXT COMMENT '环境: 生产/测试',
    status INTEGER DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    deployed_count INTEGER DEFAULT 0 COMMENT '部署项目个数',
    create_by INTEGER DEFAULT 1,
    create_by_name TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INTEGER DEFAULT 1,
    update_by_name TEXT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 服务表 (项目+服务器关联)
CREATE TABLE IF NOT EXISTS service (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_id INTEGER NOT NULL COMMENT '服务器ID',
    project_id INTEGER NOT NULL COMMENT '项目ID',
    git_branch TEXT COMMENT '项目git分支',
    deploy_path TEXT COMMENT '部署路径',
    log_path TEXT COMMENT '日志路径',
    status INTEGER DEFAULT 0 COMMENT '服务状态: 0-未启动 1-正常 2-异常',
    maven_cmd TEXT COMMENT 'maven命令',
    start_script TEXT COMMENT '服务启动脚本',
    monitor_url TEXT COMMENT '监控地址',
    enabled INTEGER DEFAULT 1 COMMENT '是否启用: 0-禁用 1-启用',
    create_by INTEGER DEFAULT 1,
    create_by_name TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INTEGER DEFAULT 1,
    update_by_name TEXT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(server_id) REFERENCES server(id),
    FOREIGN KEY(project_id) REFERENCES project(id)
);
