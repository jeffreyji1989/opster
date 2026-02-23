-- ============================================================================
-- Git 账号管理表
-- 创建日期: 2026-02-13
-- 说明: 用于管理多个 Git 账号，支持 HTTPS 和 SSH 两种认证方式
-- ============================================================================

-- 创建 git_account 表
CREATE TABLE IF NOT EXISTS git_account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 账号基本信息: account_name(账号名称), git_platform(Git 平台: gitee/gitlab/github)
    account_name TEXT NOT NULL,
    git_platform TEXT NOT NULL,
    description TEXT,

    -- HTTPS 认证信息: git_username(Git 用户名), git_password(Git 密码，加密存储)
    git_username TEXT,
    git_password TEXT,

    -- SSH 认证信息: auth_type(认证方式: 0-HTTPS 1-SSH), ssh_key_path(SSH 私钥路径), ssh_key_passphrase(私钥密码)
    auth_type INTEGER DEFAULT 0,
    ssh_key_path TEXT,
    ssh_key_passphrase TEXT,

    -- 状态: 0-禁用 1-启用
    status INTEGER DEFAULT 1,

    -- 审计字段
    create_by INTEGER DEFAULT 1,
    create_by_name TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INTEGER DEFAULT 1,
    update_by_name TEXT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    del_flag INTEGER DEFAULT 0  -- 删除标志: 0-未删除 1-已删除
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_git_account_platform ON git_account(git_platform);
CREATE INDEX IF NOT EXISTS idx_git_account_status ON git_account(status);
CREATE INDEX IF NOT EXISTS idx_git_account_del_flag ON git_account(del_flag);
