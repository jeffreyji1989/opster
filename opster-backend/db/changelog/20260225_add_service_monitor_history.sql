-- ============================================
-- 服务监控历史记录表
-- 用于记录每次HTTP监控检查的结果
-- 创建日期: 2026-02-25
-- ============================================

CREATE TABLE IF NOT EXISTS service_monitor_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    create_by INTEGER,
    create_by_name TEXT,
    create_time TEXT,
    update_by INTEGER,
    update_by_name TEXT,
    update_time TEXT,

    -- 关联信息
    service_id INTEGER NOT NULL,
    project_id INTEGER NOT NULL,
    server_id INTEGER NOT NULL,

    -- 服务信息（冗余字段，便于查询）
    project_name TEXT,
    service_name TEXT,
    server_ip TEXT,
    port INTEGER,
    monitor_url TEXT,

    -- 监控结果
    status INTEGER NOT NULL,  -- 0-异常 1-正常
    response_time INTEGER,     -- 响应耗时（毫秒）
    error_message TEXT,        -- 错误信息

    -- 记录时间
    record_time TEXT NOT NULL
);

-- 创建索引以提升查询性能
CREATE INDEX IF NOT EXISTS idx_service_monitor_history_service_id ON service_monitor_history(service_id);
CREATE INDEX IF NOT EXISTS idx_service_monitor_history_record_time ON service_monitor_history(record_time);
CREATE INDEX IF NOT EXISTS idx_service_monitor_history_service_record ON service_monitor_history(service_id, record_time DESC);