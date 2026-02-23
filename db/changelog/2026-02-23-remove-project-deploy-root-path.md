# 移除项目部署根路径依赖

## 日期
2026-02-23

## 背景
系统中已经不存在项目的部署根目录(`deploy_root_path`)概念,所有部署应该使用服务中配置的部署路径(`deploy_path`)。

## 修改内容

### 1. AppServiceServiceImpl.java
**文件**: `opster-backend/src/main/java/com/opster/module/service/service/impl/AppServiceServiceImpl.java`

**修改位置**: `uploadStartScript` 方法 (第 318-326 行)

**修改前**:
```java
// 从 Git URL 提取服务别名
String gitUrl = determineGitUrlForService(service, project);
String serviceAlias = extractServiceAliasFromGitUrl(gitUrl);

// 构建部署路径:{deployPath}/{projectCode}/{serviceAlias}/{projectPath}
String deployPath = project.getDeployRootPath();
if (StrUtil.isBlank(deployPath)) {
    throw new IllegalArgumentException("部署路径不能为空(请在项目配置中设置部署根路径)");
}

String projectCode = project.getProjectCode();
String projectPath = service.getProjectPath();

StringBuilder fullPath = new StringBuilder(deployPath);
if (StrUtil.isNotBlank(projectCode)) {
    fullPath.append("/").append(projectCode);
    if (StrUtil.isNotBlank(serviceAlias)) {
        fullPath.append("/").append(serviceAlias);
        if (StrUtil.isNotBlank(projectPath)) {
            fullPath.append("/").append(projectPath);
        }
    }
}

String deployDir = fullPath.toString();
```

**修改后**:
```java
// 使用服务配置的部署路径
String deployDir = service.getDeployPath();
if (StrUtil.isBlank(deployDir)) {
    throw new IllegalArgumentException("部署路径不能为空(请在服务配置中填写部署路径)");
}
```

### 2. DeploymentOrchestrationServiceImpl.java
**文件**: `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentOrchestrationServiceImpl.java`

**修改位置**: `resolveDeployPath` 方法 (第 679-714 行)

**修改前**:
- 优先使用服务的 `deployPath`
- 如果服务未配置,回退到项目的 `deployRootPath` 并自动构建路径

**修改后**:
- 直接使用服务配置的 `deployPath`
- 如果为空则抛出异常,要求用户配置

### 3. ScheduledDeploymentServiceImpl.java
**文件**: `opster-backend/src/main/java/com/opster/module/schedule/service/impl/ScheduledDeploymentServiceImpl.java`

**修改位置**: `buildRemoteDir` 方法 (第 390-399 行)

**修改前**:
```java
private String buildRemoteDir(Project project, AppService service) {
    String baseDir = project.getDeployRootPath() + "/" + project.getProjectCode();

    // 如果配置了项目路径,则追加到基础路径后
    if (StrUtil.isNotBlank(service.getProjectPath())) {
        return baseDir + "/" + service.getProjectPath();
    }

    return baseDir;
}
```

**修改后**:
```java
private String buildRemoteDir(Project project, AppService service) {
    // 使用服务配置的部署路径
    String serviceDeployPath = service.getDeployPath();
    if (StrUtil.isBlank(serviceDeployPath)) {
        throw new IllegalArgumentException("部署路径不能为空(请在服务配置中填写部署路径)");
    }

    return serviceDeployPath.trim().replaceAll("/+$", "");
}
```

## 影响
- 所有部署相关功能现在都依赖服务级别的 `deploy_path` 配置
- 项目的 `deploy_root_path` 字段不再被使用(可以考虑后续删除该字段)
- 错误提示更加明确,引导用户在服务配置中填写部署路径

## 相关错误修复
本次修改修复了以下错误:
```
java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
    at AppServiceServiceImpl.uploadStartScript(AppServiceServiceImpl.java:327)
```

根本原因是项目的 `deploy_root_path` 为 `null`,导致构建路径时出现 NPE。
