-- 为服务表添加 Node.js 版本字段
-- 用于前端项目指定 Node.js 版本
-- 作者: Claude
-- 日期: 2026-01-31

ALTER TABLE service ADD COLUMN node_version VARCHAR(20);

-- 添加注释
COMMENT ON COLUMN service.node_version IS 'Node.js 版本号，格式：v18.17.0，仅前端项目使用';
