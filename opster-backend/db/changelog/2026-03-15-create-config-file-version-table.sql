-- 配置文件版本历史表
-- 用于存储配置文件的修改历史和部署状态

CREATE TABLE config_file_version (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sub_project_id INTEGER NOT NULL COMMENT '关联子项目 ID',
    filename VARCHAR(255) NOT NULL COMMENT '配置文件名',
    content TEXT NOT NULL COMMENT '配置文件内容',
    version_tag VARCHAR(50) COMMENT '版本标签',
    version_description VARCHAR(500) COMMENT '版本描述',
    git_commit_hash VARCHAR(100) COMMENT 'Git 提交哈希',
    created_by VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deployed INTEGER DEFAULT 0 COMMENT '是否已部署：0-未部署 1-已部署',
    deploy_time DATETIME COMMENT '部署时间'
);

-- 创建索引加速查询
CREATE INDEX idx_config_version_sub_project ON config_file_version(sub_project_id);
CREATE INDEX idx_config_version_create_time ON config_file_version(create_time);
CREATE INDEX idx_config_version_filename ON config_file_version(filename);
