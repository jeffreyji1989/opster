-- 修改 service 表的 run_status 约束，支持 0-4 的状态值
-- 执行前请备份数据库！

USE opster;

-- 1. 删除旧的检查约束
ALTER TABLE service DROP CONSTRAINT service_chk_1;

-- 2. 添加新的检查约束（支持 0-4）
ALTER TABLE service ADD CONSTRAINT service_chk_1 CHECK (run_status BETWEEN 0 AND 4);

-- 验证约束
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
WHERE TABLE_NAME = 'service';
