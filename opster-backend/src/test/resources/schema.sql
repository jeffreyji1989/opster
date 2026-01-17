-- 项目表
CREATE TABLE IF NOT EXISTS project (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_name TEXT NOT NULL,
    project_owner TEXT,
    git_url TEXT,
    monitor_url TEXT,
    business_line TEXT,
    status INTEGER DEFAULT 1,
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
    ip TEXT NOT NULL,
    alias TEXT,
    username TEXT,
    password TEXT,
    group_name TEXT,
    env TEXT,
    status INTEGER DEFAULT 1,
    deployed_count INTEGER DEFAULT 0,
    create_by INTEGER DEFAULT 1,
    create_by_name TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INTEGER DEFAULT 1,
    update_by_name TEXT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 服务表
CREATE TABLE IF NOT EXISTS service (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_id INTEGER NOT NULL,
    project_id INTEGER NOT NULL,
    git_branch TEXT,
    deploy_path TEXT,
    log_path TEXT,
    status INTEGER DEFAULT 0,
    maven_cmd TEXT,
    start_script TEXT,
    create_by INTEGER DEFAULT 1,
    create_by_name TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INTEGER DEFAULT 1,
    update_by_name TEXT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(server_id) REFERENCES server(id),
    FOREIGN KEY(project_id) REFERENCES project(id)
);
