-- ====================================================================
-- 为现有服务设置默认部署路径
-- 创建时间：2026-02-23
-- 描述：
--   修复部署路径为空导致发版失败的问题
--   根据 project.deployRootPath 和 service.projectPath 自动计算服务部署路径
-- ====================================================================

-- 为 deploy_path 为空的服务设置默认值
UPDATE `service` s
JOIN `project` p ON s.project_id = p.id
SET s.deploy_path = CASE
    -- 如果 deploy_path 为空，根据项目配置计算默认路径
    WHEN s.deploy_path IS NULL OR s.deploy_path = '' THEN
        CONCAT(COALESCE(p.deploy_root_path, '/var/opster'), '/', p.project_code, '/', COALESCE(s.project_path, ''))
    -- 否则保持原值
    ELSE s.deploy_path
    END
WHERE s.deploy_path IS NULL OR s.deploy_path = '';

-- 显示更新结果
SELECT
    s.id,
    s.service_name,
    p.project_name,
    s.deploy_path AS '部署路径',
    p.project_code,
    s.project_path
FROM `service` s
JOIN `project` p ON s.project_id = p.id
ORDER BY s.id;

-- 说明：
-- 1. 此脚本会根据 project.deploy_root_path 和 project.project_code 自动计算部署路径
-- 2. 如果 service.project_path 不为空，会追加到路径末尾
-- 3. 计算公式：{deployRootPath}/{projectCode}/{projectPath}
-- 4. 如果项目的 deploy_root_path 也为空，则使用默认值 /var/opster
-- 5. 执行后建议手动检查并调整每个服务的部署路径
