-- ============================================
-- Opster MySQL 数据库初始化脚本
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS opster DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE opster;

-- ============================================
-- 项目表
-- ============================================
CREATE TABLE IF NOT EXISTS project (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_code VARCHAR(50) COMMENT '项目编号',
    project_name VARCHAR(255) NOT NULL COMMENT '项目名称',
    project_owner VARCHAR(100) COMMENT '项目负责人',
    repositories TEXT COMMENT '项目Git仓库列表（JSON格式）',
    git_username VARCHAR(100) COMMENT 'Git认证用户名',
    git_password VARCHAR(255) COMMENT 'Git认证密码',
    deploy_path VARCHAR(500) COMMENT '部署根目录',
    monitor_url VARCHAR(500) COMMENT '项目监控地址',
    business_line VARCHAR(100) COMMENT '业务线名称',
    status INT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_by INT DEFAULT 1 COMMENT '创建人ID',
    create_by_name VARCHAR(100) COMMENT '创建人姓名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by INT DEFAULT 1 COMMENT '更新人ID',
    update_by_name VARCHAR(100) COMMENT '更新人姓名',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志: 0-正常 1-删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ============================================
-- 服务器表
-- ============================================
CREATE TABLE IF NOT EXISTS server (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    ip VARCHAR(50) NOT NULL COMMENT 'IP地址',
    alias VARCHAR(100) COMMENT '别名',
    username VARCHAR(100) COMMENT '用户名',
    password VARCHAR(255) COMMENT '密码',
    group_name VARCHAR(100) COMMENT '分组',
    env VARCHAR(50) COMMENT '环境: 生产/测试',
    status INT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    deployed_count INT DEFAULT 0 COMMENT '部署项目个数',
    java_home VARCHAR(500) COMMENT 'JAVA_HOME路径',
    maven_home VARCHAR(500) COMMENT 'MAVEN_HOME路径',
    create_by INT DEFAULT 1 COMMENT '创建人ID',
    create_by_name VARCHAR(100) COMMENT '创建人姓名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by INT DEFAULT 1 COMMENT '更新人ID',
    update_by_name VARCHAR(100) COMMENT '更新人姓名',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志: 0-正常 1-删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器表';

-- ============================================
-- 服务表 (项目+服务器关联)
-- ============================================
CREATE TABLE IF NOT EXISTS service (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    server_id INT NOT NULL COMMENT '服务器ID',
    project_id INT NOT NULL COMMENT '项目ID',
    repo_git_url VARCHAR(500) COMMENT 'Git仓库地址',
    git_branch VARCHAR(100) COMMENT '项目git分支',
    env VARCHAR(50) COMMENT '环境: 生产/测试',
    port INT COMMENT '服务端口号',
    log_path VARCHAR(500) COMMENT '日志路径',
    run_status INT DEFAULT 0 COMMENT '运行状态: 0-未启动 1-正常 2-异常',
    status INT DEFAULT 1 COMMENT '是否启用: 0-禁用 1-启用',
    maven_cmd VARCHAR(500) COMMENT 'maven命令',
    start_script TEXT COMMENT '服务启动脚本',
    monitor_url VARCHAR(500) COMMENT '监控地址',
    repository_type INT DEFAULT 1 COMMENT '仓库类型: 0-前端 1-后端 2-管理后台 3-移动端',
    build_cmd VARCHAR(500) COMMENT '前端构建命令',
    project_path VARCHAR(255) COMMENT '项目路径（相对Git仓库的子目录路径）',
    script_uploaded INT DEFAULT 0 COMMENT '启动脚本是否已上传: 0-未上传 1-已上传',
    node_version VARCHAR(20) COMMENT '运行时版本号（Node.js或JDK版本）',
    create_by INT DEFAULT 1 COMMENT '创建人ID',
    create_by_name VARCHAR(100) COMMENT '创建人姓名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by INT DEFAULT 1 COMMENT '更新人ID',
    update_by_name VARCHAR(100) COMMENT '更新人姓名',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志: 0-正常 1-删除',
    FOREIGN KEY (server_id) REFERENCES server(id),
    FOREIGN KEY (project_id) REFERENCES project(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务表';

-- ============================================
-- 部署记录表
-- ============================================
CREATE TABLE IF NOT EXISTS deployment_record (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_id INT NOT NULL COMMENT '项目ID',
    project_name VARCHAR(255) NOT NULL COMMENT '项目名称',
    server_id INT NOT NULL COMMENT '服务器ID',
    server_ip VARCHAR(50) NOT NULL COMMENT '服务器IP',
    server_alias VARCHAR(100) COMMENT '服务器别名',
    service_id INT NOT NULL COMMENT '服务ID',
    service_name VARCHAR(255) NOT NULL COMMENT '服务名称',
    status INT NOT NULL DEFAULT 0 COMMENT '部署状态: 0-进行中 1-完成 2-失败',
    log_path VARCHAR(500) COMMENT '部署日志路径',
    backup_file_path VARCHAR(500) COMMENT '备份文件路径',
    backup_file_size BIGINT COMMENT '备份文件大小（字节）',
    version_description VARCHAR(500) COMMENT '版本描述',
    git_commit_hash VARCHAR(100) COMMENT 'Git提交哈希',
    version_tag VARCHAR(50) COMMENT '版本标签',
    is_rollback TINYINT DEFAULT 0 COMMENT '是否为回退记录',
    rollback_from_id INT COMMENT '回退源记录ID',
    create_by INT DEFAULT 1 COMMENT '创建人ID',
    create_by_name VARCHAR(100) COMMENT '创建人姓名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by INT DEFAULT 1 COMMENT '更新人ID',
    update_by_name VARCHAR(100) COMMENT '更新人姓名',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志: 0-正常 1-删除',
    FOREIGN KEY (project_id) REFERENCES project(id),
    FOREIGN KEY (server_id) REFERENCES server(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部署记录表';

-- ============================================
-- 定时发版任务表
-- ============================================
CREATE TABLE IF NOT EXISTS scheduled_deployment (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(255) NOT NULL COMMENT '任务名称',
    service_id INT NOT NULL COMMENT '服务ID',
    project_name VARCHAR(255) COMMENT '项目名称',
    server_ip VARCHAR(50) COMMENT '服务器IP',
    server_alias VARCHAR(100) COMMENT '服务器别名',
    execute_date VARCHAR(20) NOT NULL COMMENT '执行日期 yyyy-MM-dd',
    execute_time VARCHAR(10) NOT NULL COMMENT '执行时间 HH:mm',
    status INT NOT NULL DEFAULT 0 COMMENT '状态: 0-待执行 1-已完成 2-已取消',
    remark TEXT COMMENT '备注',
    create_by INT DEFAULT 1 COMMENT '创建人ID',
    create_by_name VARCHAR(100) COMMENT '创建人姓名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by INT DEFAULT 1 COMMENT '更新人ID',
    update_by_name VARCHAR(100) COMMENT '更新人姓名',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志: 0-正常 1-删除',
    FOREIGN KEY (service_id) REFERENCES service(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时发版任务表';
