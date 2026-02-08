-- 修复定时发版表的约束
-- 将废弃的service_id等字段改为可为NULL，因为现在使用关联表scheduled_deployment_services

-- 修改service_id可为NULL
ALTER TABLE scheduled_deployment MODIFY COLUMN service_id INTEGER NULL;

-- 修改project_name可为NULL
ALTER TABLE scheduled_deployment MODIFY COLUMN project_name VARCHAR(255) NULL;

-- 修改server_ip可为NULL
ALTER TABLE scheduled_deployment MODIFY COLUMN server_ip VARCHAR(255) NULL;

-- 修改server_alias可为NULL
ALTER TABLE scheduled_deployment MODIFY COLUMN server_alias VARCHAR(255) NULL;
