-- 添加上次发版时间字段
ALTER TABLE service ADD COLUMN last_deploy_time DATETIME COMMENT '上次发版时间';

-- 为现有数据初始化last_deploy_time（从deployment_record表获取）
UPDATE service
SET last_deploy_time = (
    SELECT MAX(dr.create_time)
    FROM deployment_record dr
    WHERE dr.service_id = service.id
      AND dr.status = 1  -- 1表示完成
);

-- 创建索引优化查询
CREATE INDEX idx_service_last_deploy_time ON service(last_deploy_time);
