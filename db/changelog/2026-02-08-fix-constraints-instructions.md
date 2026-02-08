# 修复定时发版表约束

## 问题说明
定时发版表 `scheduled_deployment` 中废弃的字段 `service_id` 仍然是 `NOT NULL` 约束，导致新增多服务定时发版时报错。

## 解决方案

### 方式1: 使用MySQL命令行执行

```bash
mysql -h 39.155.134.146 -P 33306 -u admin -p'adminSCzn_135246!@#' opster
```

然后执行以下SQL：

```sql
-- 修改service_id可为NULL
ALTER TABLE scheduled_deployment MODIFY COLUMN service_id INTEGER NULL;

-- 修改project_name可为NULL
ALTER TABLE scheduled_deployment MODIFY COLUMN project_name VARCHAR(255) NULL;

-- 修改server_ip可为NULL
ALTER TABLE scheduled_deployment MODIFY COLUMN server_ip VARCHAR(255) NULL;

-- 修改server_alias可为NULL
ALTER TABLE scheduled_deployment MODIFY COLUMN server_alias VARCHAR(255) NULL;
```

### 方式2: 使用Navicat或其它MySQL客户端

连接信息：
- 主机: `39.155.134.146`
- 端口: `33306`
- 用户: `admin`
- 密码: `adminSCzn_135246!@#`
- 数据库: `opster`

执行上述SQL语句即可。

### 方式3: 使用数据库管理工具

如果您的系统有phpMyAdmin或其他Web数据库管理工具，也可以通过Web界面执行上述SQL。

## 验证修复

执行完成后，可以验证表结构：

```sql
SHOW COLUMNS FROM scheduled_deployment;
```

确认 `service_id`, `project_name`, `server_ip`, `server_alias` 字段的 `Null` 列为 `YES`。

## 修复后

修复完成后，重新尝试新增定时发版功能，应该可以正常工作。
