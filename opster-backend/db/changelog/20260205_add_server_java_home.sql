-- 为 server 表添加 java_home 和 maven_home 字段
-- 用于支持每个服务器配置独立的 Java 和 Maven 路径

-- 添加 java_home 字段
ALTER TABLE server ADD COLUMN java_home TEXT;

-- 添加 maven_home 字段
ALTER TABLE server ADD COLUMN maven_home TEXT;
