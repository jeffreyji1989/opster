-- ====================================================================
-- 服务管理优化：支持一个 Git 地址配置多个部署服务
-- 创建时间：2026-02-22
-- 描述：
--   1. 新增 service_name 字段：服务名称，用于区分同一 Git 仓库的多个部署服务
--   2. 新增 service_type 字段：服务类型（原 repository_type 重命名）
--   3. 新增 source_path 字段：源码目录（系统自动计算）
--   4. 新增 compile_path 字段：编译目录（用户手动填写）
--   5. 新增 build_script 字段：编译脚本（统一 maven 命令和构建命令）
-- ====================================================================

-- 新增 service_name 字段（服务名称）
ALTER TABLE `service` ADD COLUMN `service_name` VARCHAR(100) DEFAULT NULL COMMENT '服务名称（用于区分同一 Git 仓库的多个部署服务）' AFTER `last_deploy_time`;

-- 新增 service_type 字段（服务类型，原 repository_type）
ALTER TABLE `service` ADD COLUMN `service_type` INT DEFAULT 1 COMMENT '服务类型: 0-前端 1-后端 2-管理后台 3-移动端' AFTER `service_name`;

-- 新增 source_path 字段（源码目录）
ALTER TABLE `service` ADD COLUMN `source_path` VARCHAR(500) DEFAULT NULL COMMENT '源码目录（系统自动计算：opster.deploy-path + projectCode + source）' AFTER `service_type`;

-- 新增 compile_path 字段（编译目录）
ALTER TABLE `service` ADD COLUMN `compile_path` VARCHAR(500) DEFAULT NULL COMMENT '编译目录（用户手动填写）' AFTER `source_path`;

-- 新增 build_script 字段（编译脚本）
ALTER TABLE `service` ADD COLUMN `build_script` TEXT DEFAULT NULL COMMENT '编译脚本（统一 maven 命令和构建命令）' AFTER `compile_path`;

-- 数据迁移：将 repository_type 的值迁移到 service_type
UPDATE `service` SET `service_type` = `repository_type` WHERE `repository_type` IS NOT NULL;

-- 数据迁移：将 maven_cmd 或 build_cmd 的值迁移到 build_script（优先使用非空值）
UPDATE `service` SET `build_script` = COALESCE(`maven_cmd`, `build_cmd`) WHERE `maven_cmd` IS NOT NULL OR `build_cmd` IS NOT NULL;
