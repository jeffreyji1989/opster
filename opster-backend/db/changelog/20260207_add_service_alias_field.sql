-- 为服务表添加 service_alias 字段
-- 用于构建部署目录结构：{deployPath}/{projectCode}/{serviceAlias}/
-- 创建日期：2026-02-07

ALTER TABLE service ADD COLUMN service_alias VARCHAR(100) COMMENT '服务别名（用于目录结构）';

-- 创建索引以提升查询性能
CREATE INDEX idx_service_alias ON service(service_alias);
