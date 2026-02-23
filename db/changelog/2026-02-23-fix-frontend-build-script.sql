-- ====================================================================
-- 修复前端服务的构建命令
-- 创建时间：2026-02-23
-- 描述：
--   修复前端服务使用了 Maven 构建命令的问题
--   根据 service_type 自动设置正确的 buildScript
-- ====================================================================

-- 修复前端服务的 buildScript（service_type = 0, 2, 3 为前端项目）
UPDATE `service`
SET `build_script` = 'npm install && npm run build'
WHERE `service_type` IN (0, 2, 3)  -- 0-前端, 2-管理后台, 3-移动端
  AND (`build_script` IS NULL
       OR `build_script` = ''
       OR `build_script` LIKE 'mvn%');

-- 修复后端服务的 buildScript（service_type = 1 为后端项目）
UPDATE `service`
SET `build_script` = 'mvn clean package -DskipTests'
WHERE `service_type` = 1  -- 1-后端
  AND (`build_script` IS NULL
       OR `build_script` = '');

-- 显示修复结果
SELECT
    id,
    service_name,
    service_type,
    CASE service_type
        WHEN 0 THEN '前端'
        WHEN 1 THEN '后端'
        WHEN 2 THEN '管理后台'
        WHEN 3 THEN '移动端'
        ELSE '未知'
    END AS type_name,
    build_script
FROM `service`
ORDER BY service_type, id;
