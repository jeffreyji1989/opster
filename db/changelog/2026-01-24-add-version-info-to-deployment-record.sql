-- ============================================================
-- 版本回退功能优化 - 添加版本信息字段到部署记录表
-- 创建时间: 2026-01-24
-- 说明: 为 deployment_record 表添加版本历史管理相关字段
-- ============================================================

-- 添加备份文件路径字段
ALTER TABLE deployment_record ADD COLUMN backup_file_path VARCHAR(500) DEFAULT NULL;

-- 添加版本描述字段
ALTER TABLE deployment_record ADD COLUMN version_description VARCHAR(500) DEFAULT NULL;

-- 添加Git提交哈希字段
ALTER TABLE deployment_record ADD COLUMN git_commit_hash VARCHAR(100) DEFAULT NULL;

-- 添加备份文件大小字段
ALTER TABLE deployment_record ADD COLUMN backup_file_size BIGINT DEFAULT NULL;

-- 添加版本标签字段
ALTER TABLE deployment_record ADD COLUMN version_tag VARCHAR(50) DEFAULT NULL;

-- 添加是否回滚记录字段
ALTER TABLE deployment_record ADD COLUMN is_rollback BOOLEAN DEFAULT FALSE;

-- 添加回滚源记录ID
ALTER TABLE deployment_record ADD COLUMN rollback_from_id INTEGER DEFAULT NULL;

-- 创建索引以优化查询性能
CREATE INDEX IF NOT EXISTS idx_deployment_service_id ON deployment_record(service_id);
CREATE INDEX IF NOT EXISTS idx_deployment_create_time ON deployment_record(create_time DESC);
CREATE INDEX IF NOT EXISTS idx_deployment_is_rollback ON deployment_record(is_rollback);

-- 添加字段注释（SQLite 不支持 COMMENT ON COLUMN，此处仅作文档说明）
-- backup_file_path: 备份文件在远程服务器上的完整路径
-- version_description: 版本描述信息，用于记录本次部署的变更内容
-- git_commit_hash: Git提交哈希值，用于追溯源代码版本
-- backup_file_size: 备份文件大小（字节）
-- version_tag: 版本标签，如 v1.0.0, stable, canary 等
-- is_rollback: 标识是否为回退记录
-- rollback_from_id: 回退源记录ID，指向被回退的版本记录
