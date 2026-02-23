# 修复前端项目被误判为后端项目的问题

## 问题描述
用户发版的是**前端项目**，但系统却编译了**后端项目**（使用 Maven 而不是 npm）。

## 日志分析

从日志 `/Users/deffrey/D1_JEFFREY/aiworkspace/deploy/opster/logs/20260223000751339_service12.log` 可以看出：

```
[2026-02-23 00:07:51] === 开始Maven项目打包 ===
[2026-02-23 00:07:51] 项目编码: opster
[2026-02-23 00:07:52] 项目根目录: /Users/deffrey/D1_JEFFREY/aiworkspace/deploy/opster/source/opster-backend
[2026-02-23 00:07:52] >>> 开始Maven打包...
[2026-02-23 00:07:54] [INFO] Building opster-backend 0.0.1-SNAPSHOT
```

系统使用了 Maven 构建，而不是 npm 构建。

## 根本原因

在 `DeploymentOrchestrationServiceImpl.java` 中，判断项目类型时使用了**已废弃的 `repositoryType` 字段**：

```java
// 旧代码（错误）
service.getRepositoryType() != null ?
    RepositoryType.values()[service.getRepositoryType()] : RepositoryType.BACKEND
```

根据新的数据库设计（2026-02-22 重构）：
- **`serviceType`**：新字段（0-前端, 1-后端, 2-管理后台, 3-移动端）
- **`repositoryType`**：已废弃字段

如果前端服务的 `repositoryType` 为 `null` 或默认值（1），系统会误判为后端项目，使用 Maven 构建。

## 修复方案

修改 `DeploymentOrchestrationServiceImpl.java`，将所有使用 `repositoryType` 的地方改为优先使用 `serviceType`：

### 1. 本地打包时的服务类型判断

**修改前：**
```java
Path artifact = localBuildService.buildArtifact(
    projectCode,
    extractServiceAliasFromGitUrl(gitUrl),
    service.getRepositoryType() != null ?
        RepositoryType.values()[service.getRepositoryType()] : RepositoryType.BACKEND,
    ...
);
```

**修改后：**
```java
// 获取服务类型（优先使用新的 serviceType 字段，兼容旧的 repositoryType）
Integer serviceTypeValue = service.getServiceType() != null ? service.getServiceType() : service.getRepositoryType();
RepositoryType repositoryType = serviceTypeValue != null ?
    RepositoryType.values()[serviceTypeValue] : RepositoryType.BACKEND;

// 根据服务类型选择构建命令（优先使用新的 buildScript 字段）
String buildCommand = service.getBuildScript();
if (buildCommand == null || buildCommand.isEmpty()) {
    // 兼容旧字段：前端使用 buildCmd，后端使用 mavenCmd
    buildCommand = (serviceTypeValue != null && serviceTypeValue == 0) ?
        service.getBuildCmd() : service.getMavenCmd();
}

Path artifact = localBuildService.buildArtifact(
    projectCode,
    extractServiceAliasFromGitUrl(gitUrl),
    repositoryType,
    ...
);
```

### 2. 上传打包产物时的类型判断

**修改前：**
```java
if (service.getRepositoryType() != null && service.getRepositoryType() == 0) {
    // 前端项目：上传zip并解压
    fileTransferService.uploadAndExtractZip(...);
} else {
    // 后端项目：上传jar文件
    fileTransferService.uploadFile(...);
}
```

**修改后：**
```java
if (serviceTypeValue != null && serviceTypeValue == 0) {
    // 前端项目：上传zip并解压
    fileTransferService.uploadAndExtractZip(...);
} else {
    // 后端项目：上传jar文件
    fileTransferService.uploadFile(...);
}
```

### 3. 确定 Git URL 和项目路径时的类型判断

**修改前：**
```java
RepositoryType repoType = service.getRepositoryType() != null ?
    RepositoryType.values()[service.getRepositoryType()] : RepositoryType.BACKEND;
```

**修改后：**
```java
// 获取服务类型（优先使用新的 serviceType 字段，兼容旧的 repositoryType）
Integer serviceTypeValue = service.getServiceType() != null ? service.getServiceType() : service.getRepositoryType();
RepositoryType repoType = serviceTypeValue != null ?
    RepositoryType.values()[serviceTypeValue] : RepositoryType.BACKEND;
```

## 修改位置

共修改了 **4 处**使用 `getRepositoryType()` 的地方：

1. **第 153-168 行**：`deploy()` 方法中的本地打包逻辑
2. **第 211 行**：`deploy()` 方法中的上传打包产物判断
3. **第 712-713 行**：`determineGitUrl()` 方法中的 Git URL 确定
4. **第 813-814 行**：`determineProjectPath()` 方法中的项目路径确定
5. **第 1429-1444 行**：`deployAsync()` 方法中的本地打包逻辑（异步发版）
6. **第 1486 行**：`deployAsync()` 方法中的上传打包产物判断

## 向后兼容性

所有修改都保持了向后兼容：
- 优先使用新的 `serviceType` 字段
- 如果 `serviceType` 为空，则使用旧的 `repositoryType` 字段
- 如果两者都为空，则默认为 `BACKEND` 类型

## 验证方法

1. 编辑一个前端服务，确保 `serviceType = 0`
2. 执行发版操作
3. 检查日志，确认使用 npm 构建：
   ```
   === 开始npm项目打包 ===
   项目根目录: /path/to/opster-frontend
   >>> 开始npm构建...
   ```

## 相关文件

- 修改文件：`opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentOrchestrationServiceImpl.java`
- 实体类：`opster-backend/src/main/java/com/opster/module/service/entity/AppService.java`
- 前端表单：`opster-frontend/src/views/Service.vue`

## 数据库变更

无新增数据库变更。使用已有的 `service_type` 字段（在 2026-02-22 添加）。
