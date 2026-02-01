-- 为服务表添加脚本上传标识字段
-- 用于记录启动脚本是否已上传到服务器
ALTER TABLE service ADD COLUMN script_uploaded INTEGER DEFAULT 0;
