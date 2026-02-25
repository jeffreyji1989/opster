-- 为服务表添加 service_repo_alias 字段
-- 用于构建源码目录结构：{deployPath}/{projectCode}/source/{serviceRepoAlias}/
-- 创建日期：2026-02-25

ALTER TABLE service ADD COLUMN service_repo_alias VARCHAR(100) COMMENT '服务仓库别名（用于源码目录结构，从 Git URL 自动提取）';

-- 创建索引以提升查询性能
CREATE INDEX idx_service_repo_alias ON service(service_repo_alias);
