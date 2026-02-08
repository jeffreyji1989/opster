-- 定时发版多服务改造
-- 创建关联表支持一对多关系

-- 1. 创建关联表
CREATE TABLE IF NOT EXISTS scheduled_deployment_services (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    service_id INTEGER NOT NULL,
    project_name TEXT,
    server_ip TEXT,
    server_alias TEXT,
    deploy_status INTEGER DEFAULT 0,
    deployment_record_id INTEGER,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES scheduled_deployment(id) ON DELETE CASCADE
);

-- 2. 创建索引
CREATE INDEX IF NOT EXISTS idx_sds_task_id ON scheduled_deployment_services(task_id);
CREATE INDEX IF NOT EXISTS idx_sds_service_id ON scheduled_deployment_services(service_id);

-- 3. 添加服务数量字段到主表
ALTER TABLE scheduled_deployment ADD COLUMN service_count INTEGER DEFAULT 1;

-- 4. 迁移现有数据到关联表
INSERT INTO scheduled_deployment_services (task_id, service_id, project_name, server_ip, server_alias)
SELECT id, service_id, project_name, server_ip, server_alias
FROM scheduled_deployment
WHERE service_id IS NOT NULL;

-- 5. 更新主表的service_count
UPDATE scheduled_deployment SET service_count = 1 WHERE service_id IS NOT NULL;

-- 6. 保留原字段(暂不删除),待验证无误后删除
-- ALTER TABLE scheduled_deployment DROP COLUMN service_id;
-- ALTER TABLE scheduled_deployment DROP COLUMN project_name;
-- ALTER TABLE scheduled_deployment DROP COLUMN server_ip;
-- ALTER TABLE scheduled_deployment DROP COLUMN server_alias;
