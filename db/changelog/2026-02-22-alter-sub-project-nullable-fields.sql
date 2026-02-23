-- 修改 SubProject 表字段为可空（MySQL 版本）
-- 日期: 2026-02-22
-- 原因: 简化代码仓库管理功能，部分字段改为可选

-- 项目类型改为可空
ALTER TABLE sub_project MODIFY COLUMN project_type VARCHAR(255) NULL;

-- Git 分支改为可空
ALTER TABLE sub_project MODIFY COLUMN git_branch VARCHAR(255) NULL;

-- 部署服务器 ID 改为可空
ALTER TABLE sub_project MODIFY COLUMN server_id INT NULL;

-- 部署路径改为可空
ALTER TABLE sub_project MODIFY COLUMN deploy_path VARCHAR(255) NULL;
