-- ============================================================================
-- 数据库迁移: 将 Git 认证信息从仓库级别移到项目级别
-- 日期: 2026-01-28
-- 说明:
--   1. 在 project 表添加 git_username 和 git_password 字段
--   2. 从第一个仓库迁移认证信息到项目级别（如果存在）
--   3. repositories JSON 字段不再包含 username 和 password
-- ============================================================================

-- 步骤1: 在 project 表添加 Git 认证字段
ALTER TABLE project ADD COLUMN git_username VARCHAR(255);
ALTER TABLE project ADD COLUMN git_password VARCHAR(255);

-- 步骤2: 从现有的 repositories JSON 迁移认证信息
-- 注意：这里无法直接从 JSON 中提取数据，需要通过应用层处理
-- 这一步主要用于记录，实际迁移由应用代码在首次访问时完成
