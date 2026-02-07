# Opster 目录结构重构计划

## 目标

将服务发版的目录结构从 `{deployPath}/{projectCode}/` 优化为 `{deployPath}/{projectCode}/{serviceAlias}/`，实现更清晰的项目和服务隔离。

## 最终目录结构

```
{deployPath}/
└── {projectCode}/                          # 项目编码（如：opster）
    └── {serviceAlias}/                     # 服务别名（如：backend、frontend、admin）
        ├── source/                         # 源码目录
        │   └── {projectPath}/              # 可选，monorepo 子项目
        ├── p_log/                          # 部署日志目录
        ├── artifacts/                      # 构建产物目录
        └── {projectPath}/                  # 可选，远程部署路径
            ├── app.jar
            ├── dist/
            ├── bak/
            └── logs/
```

## 变更内容

### 1. 数据库变更

**文件**: `opster-mysql-init.sql` 或新建 SQL 变更文件

```sql
-- 在 service 表添加 service_alias 字段
ALTER TABLE service ADD COLUMN service_alias VARCHAR(100) COMMENT '服务别名（用于目录结构）';
```

### 2. 后端实体类

**文件**: `opster-backend/src/main/java/com/opster/module/service/entity/AppService.java`

在第 130 行后添加：

```java
/**
 * 服务别名（用于构建部署目录结构）
 * 例如：backend、frontend、admin
 */
@Column(name = "service_alias", length = 100)
private String serviceAlias;
```

### 3. 本地日志目录

**文件**: `opster-backend/src/main/java/com/opster/common/LocalDeploymentLogger.java`

**修改位置**: 第 53-56 行的 `createLogFile()` 方法

**当前代码**:
```java
private Path createLogFile(String projectCode, String deployPath) {
    Path logDir = Paths.get(deployPath, projectCode, "p_log");
    Files.createDirectories(logDir);
    // ...
}
```

**修改为**:
```java
private Path createLogFile(String projectCode, String serviceAlias, String deployPath) {
    // 如果没有配置 serviceAlias，使用服务 ID 作为默认值
    String alias = (StrUtil.isNotBlank(serviceAlias)) ? serviceAlias : "service";
    Path logDir = Paths.get(deployPath, projectCode, alias, "p_log");
    Files.createDirectories(logDir);
    // ...
}
```

**同时修改构造函数** (第 33-47 行):
```java
// 带 WebSocket 的构造函数
public LocalDeploymentLogger(String projectCode, String serviceAlias, String deployPath, WebSocketSession wsSession)

// 不带 WebSocket 的构造函数
public LocalDeploymentLogger(String projectCode, String serviceAlias, String deployPath)
```

### 4. 本地构建目录

**文件**: `opster-backend/src/main/java/com/opster/module/service/service/impl/LocalBuildServiceImpl.java`

**修改位置**: 源码目录和构建产物目录的创建方法

**当前代码** (约第 348-366 行):
```java
private Path getUniqueSourceDir(String projectCode, String gitUrl) {
    String deployPath = opsterProperties.getDeployPath();
    return Paths.get(deployPath, projectCode, "source");
}

public Path getArtifactsDir(String projectCode) {
    String deployPath = opsterProperties.getDeployPath();
    return Paths.get(deployPath, projectCode, "artifacts");
}
```

**修改为**:
```java
private Path getUniqueSourceDir(String projectCode, String serviceAlias, String gitUrl) {
    String deployPath = opsterProperties.getDeployPath();
    String alias = (StrUtil.isNotBlank(serviceAlias)) ? serviceAlias : "service";
    return Paths.get(deployPath, projectCode, alias, "source");
}

public Path getArtifactsDir(String projectCode, String serviceAlias) {
    String deployPath = opsterProperties.getDeployPath();
    String alias = (StrUtil.isNotBlank(serviceAlias)) ? serviceAlias : "service";
    return Paths.get(deployPath, projectCode, alias, "artifacts");
}
```

**同时修改**:
- `buildArtifact()` 方法 - 添加 serviceAlias 参数并传递
- `cleanupOldArtifacts()` 方法 - 添加 serviceAlias 参数

### 5. 远程部署目录

**文件**: `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentOrchestrationServiceImpl.java`

**修改位置**: 第 556-589 行的 `buildRemoteDir()` 方法

**当前代码**:
```java
private String buildRemoteDir(Project project, AppService service) {
    String projectCode = project.getProjectCode();
    String deployPath = project.getDeployPath();

    String normalizedBasePath = deployPath.trim().replaceAll("/+$", "");
    String normalizedProjectCode = projectCode.trim().replaceAll("^/+", "").replaceAll("/+$", "");

    String baseDir = normalizedBasePath + "/" + normalizedProjectCode;

    if (StrUtil.isNotBlank(service.getProjectPath())) {
        String normalizedProjectPath = service.getProjectPath().trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
        return baseDir + "/" + normalizedProjectPath;
    }

    return baseDir;
}
```

**修改为**:
```java
private String buildRemoteDir(Project project, AppService service) {
    String projectCode = project.getProjectCode();
    String deployPath = project.getDeployPath();

    // 获取服务别名，如果未配置则使用服务 ID
    String serviceAlias = service.getServiceAlias();
    if (StrUtil.isBlank(serviceAlias)) {
        serviceAlias = "service_" + service.getId();
    }

    // 标准化路径
    String normalizedBasePath = deployPath.trim().replaceAll("/+$", "");
    String normalizedProjectCode = projectCode.trim().replaceAll("^/+", "").replaceAll("/+$", "");
    String normalizedServiceAlias = serviceAlias.trim().replaceAll("^/+", "").replaceAll("/+$", "");

    // 构建基础路径：{deployPath}/{projectCode}/{serviceAlias}
    String baseDir = normalizedBasePath + "/" + normalizedProjectCode + "/" + normalizedServiceAlias;

    // 如果配置了项目路径，则追加
    if (StrUtil.isNotBlank(service.getProjectPath())) {
        String normalizedProjectPath = service.getProjectPath().trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
        return baseDir + "/" + normalizedProjectPath;
    }

    return baseDir;
}
```

### 6. 部署流程调用

**文件**: `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentOrchestrationServiceImpl.java`

**修改位置**: 所有创建 `LocalDeploymentLogger` 和调用 `LocalBuildService` 方法的地方

**需要修改的方法**:
- `executeDeployment()` - 第 91-263 行
- `doExecuteDeployment()` - 第 1202-1337 行
- `rollbackToSpecificVersion()` - 第 400-545 行
- `doRollbackToSpecificVersion()` - 第 1343-1466 行

**修改示例**:
```java
// 修改前
logger = new LocalDeploymentLogger(projectCode, opsterProperties.getDeployPath(), wsSession);
Path artifact = localBuildService.buildArtifact(projectCode, ...);

// 修改后
logger = new LocalDeploymentLogger(projectCode, service.getServiceAlias(), opsterProperties.getDeployPath(), wsSession);
Path artifact = localBuildService.buildArtifact(projectCode, service.getServiceAlias(), ...);
```

### 7. 前端表单

**文件**: `opster-frontend/src/views/Service.vue`

**修改位置**: 服务编辑表单，在项目名称和环境之间添加服务别名输入框

在第 145 行附近添加：

```vue
<el-col :span="3">
  <el-form-item label="服务别名" label-width="70px">
    <el-input v-model="item.serviceAlias" placeholder="如：backend" />
    <span style="font-size: 12px; color: #999;">
      用于构建部署目录
    </span>
  </el-form-item>
</el-col>
```

**同时修改**:
- `handleAdd()` 方法 - 新增时初始化 serviceAlias
- 数据请求和保存逻辑 - 确保字段正确传递

## 实施步骤

### 第一阶段：数据库和实体类
1. 执行 SQL 添加 `service_alias` 字段
2. 修改 `AppService.java` 实体类

### 第二阶段：本地目录结构
1. 修改 `LocalDeploymentLogger.java` 日志目录
2. 修改 `LocalBuildServiceImpl.java` 源码和产物目录

### 第三阶段：部署流程
1. 修改 `DeploymentOrchestrationServiceImpl.java` 远程目录构建
2. 更新所有调用方法传递 serviceAlias 参数

### 第四阶段：前端界面
1. 修改 `Service.vue` 添加服务别名输入框

### 第五阶段：测试验证
1. 测试新增服务功能
2. 测试发版流程
3. 验证目录结构符合预期
4. 测试回退功能

## 关键文件清单

### 后端
- `opster-backend/src/main/java/com/opster/module/service/entity/AppService.java`
- `opster-backend/src/main/java/com/opster/common/LocalDeploymentLogger.java`
- `opster-backend/src/main/java/com/opster/module/service/service/impl/LocalBuildServiceImpl.java`
- `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentOrchestrationServiceImpl.java`

### 前端
- `opster-frontend/src/views/Service.vue`

### 数据库
- `opster-mysql-init.sql` 或新建变更文件

## 验证标准

### 功能验证
- [ ] 新增服务时可以设置服务别名
- [ ] 发版后本地目录结构正确：`{deployPath}/{projectCode}/{serviceAlias}/{source,p_log,artifacts}/`
- [ ] 发版后远程目录结构正确：`{deployPath}/{projectCode}/{serviceAlias}/{projectPath}/`
- [ ] 未配置 serviceAlias 时使用默认值（service_{id}）
- [ ] 保留了 projectPath 功能（monorepo 场景）
- [ ] 部署日志正确记录在 `p_log` 目录

### 回归测试
- [ ] 现有服务发版功能正常
- [ ] 版本回退功能正常
- [ ] 构建产物清理功能正常
- [ ] 部署记录查看正常

## 风险和注意事项

1. **数据迁移**: 现有服务的 serviceAlias 为空，需要处理默认值逻辑
2. **目录迁移**: 旧的部署目录不会自动迁移，需要保留向后兼容或手动迁移
3. **路径长度**: 确保 serviceAlias 不会导致路径过长
4. **特殊字符**: serviceAlias 应该过滤特殊字符（如空格、斜杠等）
5. **唯一性**: 同一项目下的服务别名建议唯一，但不是强制要求

## 后续优化建议

1. 添加 serviceAlias 的唯一性校验（同一 projectCode 下）
2. 添加 serviceAlias 的格式校验（字母数字下划线）
3. 提供目录迁移工具，将旧目录结构迁移到新结构
4. 在前端添加目录预览功能，显示完整的部署路径
