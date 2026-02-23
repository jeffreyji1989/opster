# 修复部署路径为空导致发版失败的问题

## 问题描述

后端项目发版时，Maven 打包成功，但部署失败，错误信息：
```
>>> 部署失败: 部署路径不能为空
```

## 日志分析

从日志 `20260223100913783_service11.log` 可以看出：

```
[2026-02-23 10:09:27] === Maven项目打包完成 ===
[2026-02-23 10:09:27] >>> 本地打包完成! 产物: 20260223100914_opster-backend-0.0.1-SNAPSHOT.jar (79 MB)
[2026-02-23 10:09:27] >>> 连接远程服务器 172.16.113.220...
[2026-02-23 10:09:27] >>> 已连接
[2026-02-23 10:09:27] >>> 部署失败: 部署路径不能为空
```

**问题：**
1. Maven 打包成功
2. 连接远程服务器成功
3. 但在构建远程部署目录时失败

## 根本原因

### 原因 1：代码层面

在 `DeploymentOrchestrationServiceImpl.buildRemoteDir()` 方法中：

```java
// 旧代码（问题）
String deployPath = project.getDeployRootPath();
if (StrUtil.isBlank(deployPath)) {
    throw new IllegalArgumentException("部署路径不能为空");
}
```

**问题：**
- 方法只使用项目的 `deployRootPath`
- 没有使用服务的 `deployPath` 字段
- 项目的 `deployRootPath` 可能为空

### 原因 2：数据层面

数据库中服务的 `deploy_path` 字段：
- 在之前的重构中（2026-02-13）被删除了
- 在刚才的修复中（2026-02-23）重新添加了
- 但已存在的服务记录的该字段值为 `NULL`

## 修复方案

### 方案 1：代码修复（已修复）

修改 `DeploymentOrchestrationServiceImpl.buildRemoteDir()` 方法：

```java
// 新代码（已修复）
// 优先使用服务的 deployPath（用户手动配置），如果为空则使用项目的 deployRootPath
String deployPath = service.getDeployPath();
if (StrUtil.isBlank(deployPath)) {
    deployPath = project.getDeployRootPath();
}
if (StrUtil.isBlank(deployPath)) {
    throw new IllegalArgumentException("部署路径不能为空（请在服务配置中填写部署路径，或在项目配置中设置部署根路径）");
}
```

**改进：**
1. ✅ 优先使用服务的 `deployPath`（用户手动配置）
2. ✅ 如果服务的 `deployPath` 为空，则使用项目的 `deployRootPath`
3. ✅ 如果两者都为空，抛出更友好的错误提示

### 方案 2：数据修复（建议执行）

执行 SQL 脚本为现有服务设置默认部署路径：

```bash
mysql -u your_user -p opster < db/changelog/2026-02-23-update-service-deploy-path.sql
```

**脚本功能：**
- 根据 `project.deployRootPath` 和 `project.projectCode` 自动计算部署路径
- 如果 `service.projectPath` 不为空，追加到路径末尾
- 计算公式：`{deployRootPath}/{projectCode}/{projectPath}`
- 如果项目的 `deployRootPath` 也为空，使用默认值 `/var/opster`

### 方案 3：手动配置（最灵活）

1. 打开服务管理页面
2. 找到需要发版的服务（ID = 11）
3. 点击"编辑"
4. 在"部署路径"字段填写路径，例如：
   - `/var/opster/opster-backend`
   - `/opt/app/opster`
   - `/home/deploy/opster-backend`
   - 或其他符合你服务器环境的路径
5. 保存后重新发版

## 部署路径说明

### 部署路径的作用

部署路径用于：
1. 在远程服务器上创建目录结构
2. 上传打包产物（JAR 文件或 ZIP 文件）
3. 备份旧版本
4. 重启服务

### 路径计算规则

**自动计算（推荐）：**
```
{project.deployRootPath}/{project.projectCode}/{service.projectPath}
```

**示例：**
- `deployRootPath` = `/var/opster`
- `projectCode` = `opster`
- `projectPath` = `opster-backend`
- **结果** = `/var/opster/opster/opster-backend`

**手动配置（灵活）：**
在服务配置中直接填写完整的部署路径，例如：
- `/var/opster/opster-backend`
- `/opt/app/opster/backend`
- `/home/deploy/app`

### 路径建议

**Linux 生产环境：**
- `/var/app/{projectName}` - 推荐
- `/opt/{company}/{projectName}` - 推荐
- `/home/{user}/app/{projectName}` - 可用

**测试环境：**
- `/home/{user}/test/{projectName}`
- `/tmp/deploy/{projectName}`

**避免：**
- ❌ `/tmp` - 临时目录，可能被清理
- ❌ `/root` - 不建议在 root 目录下部署
- ❌ 路径包含中文或特殊字符

## 执行步骤

### 1. 代码部署（必须）

后端代码已修改，需要重启后端服务：
```bash
cd opster-backend
mvn spring-boot:run
```

### 2. 数据修复（建议）

执行 SQL 脚本为现有服务设置默认路径：
```bash
mysql -u your_user -p opster < db/changelog/2026-02-23-update-service-deploy-path.sql
```

### 3. 验证修复

1. 检查服务的部署路径是否已设置
2. 重新发版
3. 查看日志，应该看到：
   ```
   >>> 创建远程目录: /var/opster/opster/opster-backend
   >>> 上传打包产物...
   ```

## 验证方法

### 检查部署路径是否已设置

```sql
SELECT
    s.id,
    s.service_name,
    p.project_name,
    s.deploy_path AS '部署路径',
    p.deploy_root_path AS '项目部署根路径',
    p.project_code,
    s.project_path
FROM `service` s
JOIN `project` p ON s.project_id = p.id
WHERE s.id = 11;  -- 替换为你的服务ID
```

### 检查发版日志

发版时应该看到：
```
>>> 创建远程目录: /var/opster/opster/opster-backend
>>> 上传打包产物 (79 MB)...
>>> 打包产物上传成功
>>> 部署新版本...
>>> 重启服务...
```

## 相关文件

- 代码修复：`opster-backend/.../DeploymentOrchestrationServiceImpl.java`
- 数据修复：`db/changelog/2026-02-23-update-service-deploy-path.sql`
- 相关文档：`db/changelog/2026-02-23-add-service-deploy-path.sql`
- 相关文档：`db/changelog/2026-02-23-fix-service-deploy-path-display.md`

## 总结

| 修复层面 | 修复内容 | 状态 |
|---------|---------|------|
| 代码层面 | `buildRemoteDir()` 优先使用 service.deployPath | ✅ 已修复 |
| 数据层面 | 为现有服务设置默认部署路径 | ⚠️ 建议执行 |
| 用户配置 | 手动填写每个服务的部署路径 | ⚠️ 最灵活 |

**建议：**
1. ✅ 先执行 SQL 脚本批量设置默认路径
2. ✅ 再手动调整每个服务的路径（如需要）
3. ✅ 重启后端服务使代码修改生效
4. ✅ 重新发版验证
