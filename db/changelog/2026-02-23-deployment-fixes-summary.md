# 发版问题修复总结

修复日期：2026-02-23

## 问题描述汇总

### 问题 1：部署路径保存后重新打开为空
**现象：** 服务管理中，编辑部署路径，保存后，再打开还是空的

**根本原因：**
- 2026-02-13 的数据库重构中，`service` 表删除了 `deploy_path` 字段
- 后端实体类 `AppService` 中没有 `deployPath` 字段
- 前端发送的 `deployPath` 数据被后端 JPA 忽略

**修复方案：**
1. 在 `AppService` 实体类中添加 `deployPath` 字段
2. 创建数据库变更 SQL 添加 `deploy_path` 字段

**相关文件：**
- ✅ `db/changelog/2026-02-23-add-service-deploy-path.sql`
- ✅ `db/changelog/2026-02-23-fix-service-deploy-path-display.md`
- ✅ `opster-backend/.../AppService.java`

---

### 问题 2：前端项目被误判为后端项目
**现象：** 发版的是前端项目，但系统却编译的是后端项目（使用 Maven 而不是 npm）

**根本原因：**
- `DeploymentOrchestrationServiceImpl` 使用已废弃的 `repositoryType` 字段判断项目类型
- 新的设计应该使用 `serviceType` 字段
- 当 `repositoryType` 为 null 或默认值时，误判为后端项目

**修复方案：**
- 修改 `DeploymentOrchestrationServiceImpl.java` 中 6 处使用 `repositoryType` 的地方
- 全部改为优先使用 `serviceType`，保持向后兼容

**相关文件：**
- ✅ `db/changelog/2026-02-23-fix-service-type-detection.md`
- ✅ `opster-backend/.../DeploymentOrchestrationServiceImpl.java`

---

### 问题 3：前端项目使用了错误的构建命令
**现象：** 前端项目发版时，构建命令是 `mvn clean package -DskipTests` 而不是 npm 命令

**根本原因：**
- 数据库中前端服务的 `build_script` 字段存储的是 Maven 命令
- 可能是创建时类型设置错误，或后来修改类型但构建命令未同步更新

**修复方案：**
1. **立即修复**：执行 SQL 修复脚本，根据 `service_type` 自动修正 `build_script`
2. **预防措施**：在 `AppServiceServiceImpl.save()` 中添加自动修正和验证逻辑

**相关文件：**
- ✅ `db/changelog/2026-02-23-fix-frontend-build-script.sql`
- ✅ `db/changelog/2026-02-23-fix-frontend-build-command-issue.md`
- ✅ `opster-backend/.../AppServiceServiceImpl.java`

---

## 部署步骤

### 1. 数据库变更（必须按顺序执行）

```bash
# 1. 添加 deploy_path 字段
mysql -u your_user -p opster < db/changelog/2026-02-23-add-service-deploy-path.sql

# 2. 修复前端服务的构建命令
mysql -u your_user -p opster < db/changelog/2026-02-23-fix-frontend-build-script.sql
```

### 2. 后端代码变更

修改了以下文件：
- `opster-backend/src/main/java/com/opster/module/service/entity/AppService.java`
- `opster-backend/src/main/java/com/opster/module/service/service/impl/AppServiceServiceImpl.java`
- `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentOrchestrationServiceImpl.java`

### 3. 重启服务

```bash
# 停止后端服务
# 重新编译（如果需要）
cd opster-backend
mvn clean package -DskipTests

# 启动后端服务
mvn spring-boot:run
```

---

## 验证方法

### 验证问题 1：部署路径保存
1. 打开服务管理页面
2. 编辑某个服务的部署路径
3. 保存后关闭对话框
4. 再次打开编辑，验证部署路径正确显示

### 验证问题 2：前端项目类型判断
1. 编辑一个前端服务，确认 `serviceType = 0`
2. 重新发版
3. 检查日志，确认使用 npm 构建：
   ```
   === 开始npm项目打包 ===
   >>> 开始npm构建...
   ```

### 验证问题 3：构建命令正确性
1. 重新发版前端项目
2. 检查日志中的构建命令：
   ```
   构建命令: npm install && npm run build
   执行npm构建命令: npm install && npm run build --loglevel=verbose
   ```

---

## 服务类型与构建命令对照表

| service_type | 类型名称 | 默认构建命令 |
|-------------|---------|------------|
| 0 | 前端 | `npm install && npm run build` |
| 1 | 后端 | `mvn clean package -DskipTests` |
| 2 | 管理后台 | `npm install && npm run build` |
| 3 | 移动端 | `npm install && npm run build` |

---

## 代码变更摘要

### AppService.java
```java
/**
 * 部署路径（用户手动填写）
 * 每个部署服务有独立的部署路径
 * 例如：/var/opster/eip
 */
@Column(name = "deploy_path", length = 500)
private String deployPath;
```

### AppServiceServiceImpl.java
添加了自动修正和验证逻辑：
1. 根据 `serviceType` 自动设置默认构建命令
2. 验证构建命令与服务类型是否匹配
3. 不匹配时自动修正并记录警告日志

### DeploymentOrchestrationServiceImpl.java
修改了 6 处判断项目类型的逻辑：
```java
// 旧代码
service.getRepositoryType() != null ?
    RepositoryType.values()[service.getRepositoryType()] : RepositoryType.BACKEND

// 新代码
Integer serviceTypeValue = service.getServiceType() != null ?
    service.getServiceType() : service.getRepositoryType();
RepositoryType repositoryType = serviceTypeValue != null ?
    RepositoryType.values()[serviceTypeValue] : RepositoryType.BACKEND;
```

---

## 向后兼容性

所有修复都保持了向后兼容：
- ✅ 优先使用新字段（`serviceType`, `buildScript`）
- ✅ 如果新字段为空，则使用旧字段（`repositoryType`, `mavenCmd`, `buildCmd`）
- ✅ 如果两者都为空，则使用合理的默认值

---

## 相关文档

- `db/changelog/2026-02-23-add-service-deploy-path.sql` - 添加 deploy_path 字段
- `db/changelog/2026-02-23-fix-service-deploy-path-display.md` - 部署路径问题分析
- `db/changelog/2026-02-23-fix-service-type-detection.md` - 服务类型判断问题分析
- `db/changelog/2026-02-23-fix-frontend-build-script.sql` - 修复构建命令 SQL
- `db/changelog/2026-02-23-fix-frontend-build-command-issue.md` - 构建命令问题分析
- `db/changelog/2026-02-23-deployment-fixes-summary.md` - 本文档
