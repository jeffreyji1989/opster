-- ================================================================
-- 服务管理发版功能优化 - 数据库变更
-- 日期: 2026-01-24
-- 说明: 添加仓库类型和构建命令字段，支持本地打包+远程部署架构
-- ================================================================

-- 1. 添加仓库类型字段到 service 表
ALTER TABLE service ADD COLUMN repository_type INTEGER DEFAULT 1;

-- 2. 添加前端构建命令字段到 service 表
ALTER TABLE service ADD COLUMN build_cmd VARCHAR(500);

-- 3. 添加字段注释（SQLite 不支持 COMMENT ON COLUMN，此处仅作文档说明）
-- repository_type: 仓库类型 0-前端 1-后端 2-管理后台 3-移动端
-- build_cmd: 前端构建命令（如：npm run build）

-- 4. 数据迁移：现有服务默认为后端类型
UPDATE service SET repository_type = 1 WHERE repository_type IS NULL;

-- 5. 创建部署锁表，用于并发控制
CREATE TABLE IF NOT EXISTS deployment_lock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    service_id INTEGER NOT NULL,
    lock_id VARCHAR(50) NOT NULL,
    create_time DATETIME,
    expire_time DATETIME,
    UNIQUE(service_id)
);

-- 6. 创建索引以提升查询性能
CREATE INDEX IF NOT EXISTS idx_deployment_lock_service_id ON deployment_lock(service_id);
CREATE INDEX IF NOT EXISTS idx_deployment_lock_expire_time ON deployment_lock(expire_time);
