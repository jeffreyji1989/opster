-- ====================================================================
-- 添加 service 表的 deploy_path 字段
-- 创建时间：2026-02-23
-- 描述：
--   修复服务管理中编辑部署路径保存后重新打开为空的问题
--   原因：在 2026-02-13 的重构中，deploy_path 字段被删除了
--   解决：重新添加 deploy_path 字段，允许用户手动覆盖默认部署路径
-- ====================================================================

-- 新增 deploy_path 字段（部署路径，用户手动填写）
ALTER TABLE `service` ADD COLUMN `deploy_path` VARCHAR(500) DEFAULT NULL COMMENT '部署路径（用户手动填写，例如：/var/opster/eip）' AFTER `compile_path`;
