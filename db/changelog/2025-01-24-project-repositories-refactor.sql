-- =====================================================
-- 项目管理模块 Git 仓库多地址支持
-- 变更日期: 2025-01-24
-- 变更说明: 将单个 git_url 字段改造为支持多个仓库的 repositories 字段（JSON 格式）
-- =====================================================

-- 变更原因：
-- 1. 一个项目通常包含多个 Git 仓库（前端、后端、管理后台、移动端等）
-- 2. 原有设计每次只能添加一个 git 地址，操作繁琐
-- 3. 需要支持不同类型仓库的独立配置（Git地址、项目路径、描述）

-- 影响范围：
-- 1. 后端 Project 实体类
-- 2. 前端 Project.vue 组件
-- 3. 新增枚举类 RepositoryType
-- 4. 新增 DTO 类 RepositoryDTO
-- 5. 新增转换器 RepositoriesConverter

-- =====================================================
-- 第一步：删除原有 git_url 字段（SQLite 不支持 ALTER TABLE DROP COLUMN）
-- =====================================================
-- 注意：SQLite 不支持直接删除列，需要重建表

-- 1. 创建临时表（不含 git_url 字段）
CREATE TABLE IF NOT EXISTS project_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_name VARCHAR(255) NOT NULL,
    project_owner VARCHAR(100),
    repositories TEXT,
    monitor_url VARCHAR(500),
    business_line VARCHAR(100),
    status INTEGER,
    create_by VARCHAR(50),
    create_time DATETIME,
    update_by VARCHAR(50),
    update_time DATETIME,
    del_flag INTEGER DEFAULT 0
);

-- 2. 迁移数据（将 git_url 转换为 repositories JSON 格式）
INSERT INTO project_new (
    id, project_name, project_owner, repositories,
    monitor_url, business_line, status,
    create_by, create_time, update_by, update_time, del_flag
)
SELECT
    id,
    project_name,
    project_owner,
    -- 如果 git_url 不为空，转换为 JSON 数组格式
    CASE
        WHEN git_url IS NOT NULL AND git_url != ''
        THEN '[{"type":1,"gitUrl":"' || git_url || '","projectPath":"","description":""}]'
        ELSE '[]'
    END as repositories,
    monitor_url,
    business_line,
    status,
    create_by,
    create_time,
    update_by,
    update_time,
    del_flag
FROM project
WHERE del_flag = 0;

-- 3. 删除原表
DROP TABLE project;

-- 4. 重命名新表
ALTER TABLE project_new RENAME TO project;

-- =====================================================
-- 第二步：创建索引（可选）
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_project_name ON project(project_name);
CREATE INDEX IF NOT EXISTS idx_project_business_line ON project(business_line);
CREATE INDEX IF NOT EXISTS idx_project_status ON project(status);

-- =====================================================
-- 数据迁移说明
-- =====================================================
-- 原有 git_url 字段的数据会被迁移到 repositories 字段中
-- 默认类型为后端（type=1），其他字段为空值
-- 示例：原有的 git_url = 'https://github.com/xxx/project.git'
--      迁移后 repositories = '[{"type":1,"gitUrl":"https://github.com/xxx/project.git","projectPath":"","description":""}]'

-- =====================================================
-- 回滚方案（如需回滚，执行以下步骤）
-- =====================================================
-- 1. 创建临时表（含 git_url 字段）
-- CREATE TABLE IF NOT EXISTS project_old (
--     id INTEGER PRIMARY KEY AUTOINCREMENT,
--     project_name VARCHAR(255) NOT NULL,
--     project_owner VARCHAR(100),
--     git_url VARCHAR(500),
--     monitor_url VARCHAR(500),
--     business_line VARCHAR(100),
--     status INTEGER,
--     create_by VARCHAR(50),
--     create_time DATETIME,
--     update_by VARCHAR(50),
--     update_time DATETIME,
--     del_flag INTEGER DEFAULT 0
-- );

-- 2. 从 repositories 提取第一个后端类型的 git_url
-- INSERT INTO project_old (
--     id, project_name, project_owner, git_url,
--     monitor_url, business_line, status,
--     create_by, create_time, update_by, update_time, del_flag
-- )
-- SELECT
--     id,
--     project_name,
--     project_owner,
--     json_extract(repositories, '$[0].gitUrl') as git_url,
--     monitor_url,
--     business_line,
--     status,
--     create_by,
--     create_time,
--     update_by,
--     update_time,
--     del_flag
-- FROM project;

-- 3. 删除新表并重命名
-- DROP TABLE project;
-- ALTER TABLE project_old RENAME TO project;
