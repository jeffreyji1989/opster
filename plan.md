# 服务管理发版功能优化实施计划

## 一、需求概述

优化服务管理中的发版功能，实现**本地打包 + 远程部署**的新架构：

### 核心需求
1. **本地打包**：所有服务打包在当前系统（Opster所在系统）进行，不在远程服务器
2. **统一本地工作目录**：使用 `opster.deploy-path` 作为本地部署根目录
3. **项目目录结构**：
   - 项目根目录名为项目编码（`project.projectCode`）
   - 源码放在 `source/` 子目录下
   - 打包产物归档到项目根目录
4. **远程部署**：打包完成后，通过SCP传输到目标服务器的 `service.deployPath` 目录
5. **打包工具**：支持 Maven 和 npm
6. **并发控制**：防止同一服务被多人同时部署

### 本地目录结构
```
${opster.deploy-path}/                    # 本地工作根目录
├── {project.projectCode}/                # 项目根目录（项目编码）
│   ├── source/                           # 源码目录
│   │   ├── backend/                      # 后端项目
│   │   │   └── target/*.jar
│   │   └── frontend/                     # 前端项目
│   │       └── dist/                     # npm打包输出
│   ├── artifacts/                        # 产物归档目录
│   │   ├── app-backend-{timestamp}.jar
│   │   └── app-frontend-{timestamp}.zip
│   └── logs/                             # 本地打包日志
│       └── build-{timestamp}.log
```

### 远程部署目录结构
```
{service.deployPath}/{project.projectCode}/
├── app.jar                              # 当前运行的jar
├── dist/                                # 前端静态文件
├── bak/                                 # 备份目录
│   └── app_{timestamp}.jar
├── logs/                                # 日志目录
└── restart.sh                           # 启动脚本
```

---

## 二、核心流程设计

### 2.1 完整部署流程

```
1. 【本地打包阶段】
   ├─ 1.1 获取部署锁（防止并发）
   ├─ 1.2 创建本地工作目录
   ├─ 1.3 Git clone/pull 源码到 {projectCode}/source/
   ├─ 1.4 执行打包
   │   ├─ Maven项目: mvn clean package -DskipTests
   │   └─ npm项目: npm install && npm run build
   ├─ 1.5 归档产物到 {projectCode}/artifacts/
   └─ 1.6 记录本地打包日志

2. 【远程部署阶段】
   ├─ 2.1 连接目标服务器 SSH
   ├─ 2.2 创建远程目录结构
   ├─ 2.3 SCP上传打包产物
   │   ├─ jar文件直接上传
   │   └─ zip文件上传后解压
   ├─ 2.4 备份当前运行的版本到 bak/
   ├─ 2.5 部署新版本
   ├─ 2.6 执行启动脚本
   ├─ 2.7 健康检查（端口检测）
   └─ 2.8 释放部署锁
```

### 2.2 错误处理策略

| 失败阶段 | 处理方式 |
|---------|---------|
| Git拉取失败 | 终止流程，释放锁，记录错误日志 |
| 本地打包失败 | 终止流程，释放锁，清理临时文件 |
| 文件传输失败 | 重试3次，仍失败则回滚 |
| 远程部署失败 | 自动从bak/目录恢复上一版本 |
| 服务启动失败 | 自动回滚，保留完整日志 |

---

## 三、数据库变更

### 3.1 新增字段到 service 表

**文件**: `db/changelog/2026-01-24-add-service-build-fields.sql`

```sql
-- 添加仓库类型字段
ALTER TABLE service ADD COLUMN repository_type INTEGER DEFAULT 1;

-- 添加前端构建命令字段
ALTER TABLE service ADD COLUMN build_cmd VARCHAR(500);

-- 添加注释
COMMENT ON COLUMN service.repository_type IS '仓库类型: 0-前端 1-后端 2-管理后台 3-移动端';
COMMENT ON COLUMN service.build_cmd IS '前端构建命令（如：npm run build）';

-- 数据迁移：现有服务默认为后端类型
UPDATE service SET repository_type = 1 WHERE repository_type IS NULL;
```

### 3.2 部署锁表（可选，也可以使用数据库直接实现）

```sql
CREATE TABLE deployment_lock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    service_id INTEGER NOT NULL,
    lock_id VARCHAR(50) NOT NULL,
    create_time DATETIME,
    expire_time DATETIME,
    UNIQUE(service_id)
);
```

---

## 四、实施步骤

### 阶段一：基础设施（1-2天）

#### 1.1 本地命令执行工具类
**新建文件**: `opster-backend/src/main/java/com/opster/common/LocalCommandUtils.java`

```java
public class LocalCommandUtils {
    /**
     * 执行本地命令并实时推送输出到WebSocket
     * @param workDir 工作目录
     * @param command 命令（如：git pull, mvn clean package）
     * @param wsSession WebSocket会话
     * @return 执行是否成功
     */
    public static boolean executeCommand(Path workDir, String command, WebSocketSession wsSession)
}
```

#### 1.2 配置属性类
**新建文件**: `opster-backend/src/main/java/com/opster/config/OpsterProperties.java`

```java
@Configuration
@ConfigurationProperties(prefix = "opster")
@Data
public class OpsterProperties {
    private String deployPath;      // 本地部署根目录
    private String mavenHome;       // Maven主目录
    private String javaHome;        // Java主目录
    private BuildConfig build = new BuildConfig();

    @Data
    public static class BuildConfig {
        private int timeoutMinutes = 30;      // 构建超时时间
        private int keepVersions = 5;         // 保留版本数量
    }
}
```

#### 1.3 数据库变更
**新建文件**: `db/changelog/2026-01-24-add-service-build-fields.sql`

执行SQL变更，更新 `AppService` 实体添加新字段：
- `repositoryType`: 仓库类型
- `buildCmd`: 前端构建命令

---

### 阶段二：本地打包服务（2-3天）

#### 2.1 本地打包服务接口和实现
**新建文件**:
- `opster-backend/src/main/java/com/opster/module/service/service/LocalBuildService.java`
- `opster-backend/src/main/java/com/opster/module/service/service/impl/LocalBuildServiceImpl.java`

**核心方法**:
```java
public interface LocalBuildService {
    /**
     * 执行本地打包
     * @param projectCode 项目编码
     * @param repositoryType 仓库类型（BACKEND/FRONTEND）
     * @param gitUrl Git地址
     * @param gitBranch 分支
     * @param buildCmd 构建命令
     * @param wsSession WebSocket会话
     * @return 打包产物的本地路径
     */
    Path buildArtifact(String projectCode, RepositoryType repositoryType,
                      String gitUrl, String gitBranch, String buildCmd,
                      WebSocketSession wsSession) throws Exception;

    /**
     * 清理临时文件（保留最近N个版本）
     */
    void cleanupOldArtifacts(String projectCode, int keepVersions);
}
```

**实现要点**:
1. Git操作：检测目录是否存在，决定是clone还是pull
2. Maven打包：执行 `mvn clean package -DskipTests`，查找target/*.jar
3. npm打包：执行 `npm install && npm run build`，压缩dist目录为zip
4. 产物归档：复制到 `{projectCode}/artifacts/` 目录，文件名带时间戳

#### 2.2 本地日志记录器
**新建文件**: `opster-backend/src/main/java/com/opster/common/LocalBuildLogger.java`

```java
public class LocalBuildLogger {
    private final Path logFile;
    private final WebSocketSession wsSession;

    public void log(String message) {
        // 1. 写入本地日志文件
        // 2. 推送到WebSocket
    }
}
```

---

### 阶段三：文件传输服务（1-2天）

#### 3.1 文件传输服务接口和实现
**新建文件**:
- `opster-backend/src/main/java/com/opster/module/service/service/FileTransferService.java`
- `opster-backend/src/main/java/com/opster/module/service/service/impl/FileTransferServiceImpl.java`

**核心方法**:
```java
public interface FileTransferService {
    /**
     * 通过SCP上传文件（带进度推送）
     */
    void uploadFile(Path localFile, Session sshSession, String remotePath,
                   WebSocketSession wsSession) throws Exception;

    /**
     * 上传并解压zip文件（前端项目）
     */
    void uploadAndExtractZip(Path localZip, Session sshSession,
                            String remoteDir, WebSocketSession wsSession) throws Exception;
}
```

**实现要点**:
1. 使用JSch的SCP协议传输文件
2. 每10%推送一次上传进度到WebSocket
3. 传输完成后校验文件大小
4. 失败自动重试3次

---

### 阶段四：部署编排服务（2-3天）

#### 4.1 部署编排服务接口和实现
**新建文件**:
- `opster-backend/src/main/java/com/opster/module/service/service/DeploymentOrchestrationService.java`
- `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentOrchestrationServiceImpl.java`

**核心方法**:
```java
public interface DeploymentOrchestrationService {
    /**
     * 执行完整部署流程（本地打包 + 远程部署）
     */
    void executeDeployment(Integer serviceId, WebSocketSession wsSession);

    /**
     * 执行重启（不打包，仅重启远程服务）
     */
    void executeRestart(Integer serviceId, WebSocketSession wsSession);

    /**
     * 执行回滚
     */
    void executeRollback(Integer serviceId, WebSocketSession wsSession);
}
```

**完整部署流程实现**:
```java
@Override
public void executeDeployment(Integer serviceId, WebSocketSession wsSession) {
    String lockId = null;
    try {
        // 1. 获取服务信息
        AppService service = appServiceRepository.findById(serviceId).orElseThrow();
        Project project = projectRepository.findById(service.getProjectId()).orElseThrow();
        Server server = serverRepository.findById(service.getServerId()).orElseThrow();

        // 2. 获取部署锁
        lockId = deploymentLockService.tryLock(serviceId);
        if (lockId == null) {
            sendMessage(wsSession, ">>> 该服务正在部署中，请稍后重试");
            return;
        }

        // 3. 本地打包
        String projectCode = project.getProjectCode();
        sendMessage(wsSession, ">>> 开始本地打包...");
        Path artifact = localBuildService.buildArtifact(
            projectCode,
            service.getRepositoryType(),
            service.getRepoGitUrl(),
            service.getGitBranch(),
            service.getRepositoryType() == RepositoryType.FRONTEND ? service.getBuildCmd() : service.getMavenCmd(),
            wsSession
        );

        // 4. 连接远程服务器
        sendMessage(wsSession, ">>> 连接远程服务器...");
        Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());

        // 5. 创建远程目录
        String remoteDir = service.getDeployPath() + "/" + projectCode;
        executeRemoteCommand(sshSession, "mkdir -p " + remoteDir + "/{bak,logs}", wsSession);

        // 6. 上传打包产物
        sendMessage(wsSession, ">>> 上传打包产物...");
        if (service.getRepositoryType() == RepositoryType.FRONTEND) {
            fileTransferService.uploadAndExtractZip(artifact, sshSession, remoteDir, wsSession);
        } else {
            fileTransferService.uploadFile(artifact, sshSession, remoteDir, wsSession);
        }

        // 7. 备份旧版本
        backupRemoteVersion(sshSession, remoteDir, wsSession);

        // 8. 部署新版本
        deployNewVersion(sshSession, remoteDir, artifact, wsSession);

        // 9. 重启服务
        restartRemoteService(sshSession, remoteDir, service.getStartScript(), wsSession);

        // 10. 健康检查
        boolean isHealthy = checkHealth(sshSession, service.getPort());
        if (!isHealthy) {
            throw new Exception("服务启动失败，端口未监听");
        }

        sendMessage(wsSession, ">>> 部署成功完成！");

    } catch (Exception e) {
        log.error("Deployment failed", e);
        sendMessage(wsSession, ">>> 部署失败: " + e.getMessage());

        // 自动回滚
        try {
            executeRollback(serviceId, wsSession);
        } catch (Exception rollbackException) {
            sendMessage(wsSession, ">>> 回滚失败: " + rollbackException.getMessage());
        }
    } finally {
        if (lockId != null) {
            deploymentLockService.unlock(lockId);
        }
    }
}
```

#### 4.2 远程命令执行辅助方法
```java
private void backupRemoteVersion(Session sshSession, String remoteDir, WebSocketSession wsSession) {
    String cmd = String.format(
        "bash -c 'if ls %s/*.jar 1> /dev/null 2>&1; then " +
        "timestamp=$(date +\"%%Y%%m%%d%%H%%M%%S\"); " +
        "cp -f %s/*.jar %s/bak/backup_${timestamp}.jar; " +
        "echo \"Backup completed\"; fi'",
        remoteDir, remoteDir, remoteDir
    );
    executeRemoteCommand(sshSession, cmd, wsSession);
}
```

---

### 阶段五：并发控制（1天）

#### 5.1 部署锁服务
**新建文件**:
- `opster-backend/src/main/java/com/opster/module/service/service/DeploymentLockService.java`
- `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentLockServiceImpl.java`

**实现方式**：基于数据库实现分布式锁

```java
@Service
public class DeploymentLockServiceImpl implements DeploymentLockService {

    @Autowired
    private DeploymentLockRepository lockRepository;

    @Override
    public String tryLock(Integer serviceId) {
        // 清理过期锁
        lockRepository.deleteExpiredLocks();

        // 检查是否已有锁
        DeploymentLock existingLock = lockRepository.findByServiceId(serviceId);
        if (existingLock != null) {
            return null; // 锁已被占用
        }

        // 创建新锁（30分钟过期）
        DeploymentLock lock = new DeploymentLock();
        lock.setServiceId(serviceId);
        lock.setLockId(UUID.randomUUID().toString());
        lock.setCreateTime(LocalDateTime.now());
        lock.setExpireTime(LocalDateTime.now().plusMinutes(30));
        lockRepository.save(lock);

        return lock.getLockId();
    }

    @Override
    public void unlock(String lockId) {
        lockRepository.deleteById(lockId);
    }
}
```

---

### 阶段六：WebSocket集成（1天）

#### 6.1 重构 ExecWebSocketHandler
**修改文件**: `opster-backend/src/main/java/com/opster/handler/ExecWebSocketHandler.java`

**变更要点**:
1. 保留WebSocket连接管理逻辑
2. 将实际部署逻辑委托给 `DeploymentOrchestrationService`
3. 简化代码，提高可维护性

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String path = session.getUri().getPath();
    String[] parts = path.split("/");
    String serviceIdStr = parts[3];
    String action = parts[4];
    Integer serviceId = Integer.parseInt(serviceIdStr);

    startExec(session, serviceId, action);
}

private void startExec(WebSocketSession wsSession, Integer serviceId, String action) {
    executorService.submit(() -> {
        try {
            switch (action) {
                case "deploy":
                    deploymentOrchestrationService.executeDeployment(serviceId, wsSession);
                    break;
                case "restart":
                    deploymentOrchestrationService.executeRestart(serviceId, wsSession);
                    break;
                case "start":
                    deploymentOrchestrationService.executeRestart(serviceId, wsSession);
                    break;
                case "rollback":
                    deploymentOrchestrationService.executeRollback(serviceId, wsSession);
                    break;
                default:
                    sendMessage(wsSession, "Unknown action: " + action);
            }
        } catch (Exception e) {
            log.error("Error executing action: " + action, e);
            sendMessage(wsSession, "ERROR: " + e.getMessage());
        } finally {
            try {
                wsSession.close();
            } catch (Exception ignored) {}
        }
    });
}
```

---

### 阶段七：前端适配（1-2天）

#### 7.1 扩展服务表单
**修改文件**: `opster-frontend/src/views/Service.vue`

**新增表单字段**:
1. **仓库类型**：下拉选择（前端/后端/管理后台/移动端）
2. **构建命令**：前端项目的构建命令（如：npm run build）

**新增操作按钮**:
- 现有的"编译重启"按钮保持不变，调用 `/ws/exec/{id}/deploy`
- 自动根据 `repositoryType` 决定打包方式

**优化日志显示**:
- 添加上传进度条
- 区分本地打包日志和远程部署日志

---

## 五、关键文件清单

### 新建文件

| 文件路径 | 说明 |
|---------|------|
| `opster-backend/src/main/java/com/opster/common/LocalCommandUtils.java` | 本地命令执行工具类 |
| `opster-backend/src/main/java/com/opster/common/LocalBuildLogger.java` | 本地构建日志记录器 |
| `opster-backend/src/main/java/com/opster/config/OpsterProperties.java` | 配置属性类 |
| `opster-backend/src/main/java/com/opster/module/service/service/LocalBuildService.java` | 本地打包服务接口 |
| `opster-backend/src/main/java/com/opster/module/service/service/impl/LocalBuildServiceImpl.java` | 本地打包服务实现 |
| `opster-backend/src/main/java/com/opster/module/service/service/FileTransferService.java` | 文件传输服务接口 |
| `opster-backend/src/main/java/com/opster/module/service/service/impl/FileTransferServiceImpl.java` | 文件传输服务实现 |
| `opster-backend/src/main/java/com/opster/module/service/service/DeploymentOrchestrationService.java` | 部署编排服务接口 |
| `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentOrchestrationServiceImpl.java` | 部署编排服务实现 |
| `opster-backend/src/main/java/com/opster/module/service/service/DeploymentLockService.java` | 部署锁服务接口 |
| `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentLockServiceImpl.java` | 部署锁服务实现 |
| `db/changelog/2026-01-24-add-service-build-fields.sql` | 数据库变更SQL |

### 修改文件

| 文件路径 | 变更说明 |
|---------|---------|
| `opster-backend/src/main/java/com/opster/handler/ExecWebSocketHandler.java` | 重构：委托部署逻辑到编排服务 |
| `opster-backend/src/main/java/com/opster/module/service/entity/AppService.java` | 新增字段：repositoryType, buildCmd |
| `opster-backend/src/main/resources/application.yml` | 新增配置项（可选扩展） |
| `opster-frontend/src/views/Service.vue` | 新增表单字段和优化UI |

---

## 六、验证测试方案

### 6.1 单元测试

1. **本地命令执行测试**
   - 测试Git clone/pull
   - 测试Maven打包
   - 测试npm打包
   - 测试错误处理

2. **文件传输测试**
   - 测试SCP上传
   - 测试进度推送
   - 测试重试机制

3. **并发锁测试**
   - 测试锁获取和释放
   - 测试过期锁清理

### 6.2 集成测试

1. **Maven项目完整部署流程**
   - 本地打包 → SCP传输 → 远程部署 → 服务启动 → 健康检查

2. **npm项目完整部署流程**
   - 本地打包（npm run build）→ 压缩dist → SCP传输 → 解压 → 部署

3. **错误场景测试**
   - Git拉取失败
   - Maven编译失败
   - SCP传输中断
   - 服务启动失败 → 自动回滚

### 6.3 端到端验证

**测试环境准备**:
1. 准备一个测试用的Maven后端项目
2. 准备一个测试用的Vue前端项目
3. 准备一台远程测试服务器

**验证步骤**:
1. 在前端创建项目和服务
2. 配置仓库类型、Git地址、分支等信息
3. 点击"编译重启"按钮
4. 观察WebSocket实时日志输出
5. 验证本地是否生成打包产物
6. 验证远程服务器是否接收到文件
7. 验证服务是否正常启动
8. 验证端口健康检查是否通过
9. 测试回滚功能

---

## 七、预估工作量

| 阶段 | 工作量 | 说明 |
|-----|-------|------|
| 阶段一：基础设施 | 1-2天 | 工具类、配置类、数据库变更 |
| 阶段二：本地打包服务 | 2-3天 | Git、Maven、npm打包逻辑 |
| 阶段三：文件传输服务 | 1-2天 | SCP上传、进度推送、重试机制 |
| 阶段四：部署编排服务 | 2-3天 | 完整部署流程、远程命令、健康检查 |
| 阶段五：并发控制 | 1天 | 分布式锁实现 |
| 阶段六：WebSocket集成 | 1天 | 重构ExecWebSocketHandler |
| 阶段七：前端适配 | 1-2天 | 表单字段、UI优化 |
| 测试与优化 | 2-3天 | 单元测试、集成测试、bug修复 |
| **总计** | **11-17天** | 约2-3周 |

---

## 八、风险与注意事项

### 8.1 技术风险

| 风险点 | 缓解措施 |
|-------|---------|
| 本地环境依赖（Maven、Node.js） | 启动时检测环境，提供友好的错误提示 |
| 大文件传输耗时 | 实现断点续传（可选）、调整缓冲区大小 |
| 并发部署冲突 | 使用分布式锁，提供友好的等待提示 |
| WebSocket连接中断 | 日志保存到文件，支持离线查看 |
| 打包产物占用磁盘空间 | 定期清理旧版本，保留最近5个 |

### 8.2 兼容性考虑

- 保持向后兼容：现有部署流程可以平滑迁移
- 配置灵活性：允许某些服务使用旧模式（远程直接部署）
- 逐步推广：先在测试环境验证，再推广到生产环境

---

## 九、后续扩展方向

1. **支持更多构建工具**：Gradle、yarn、pnpm
2. **Docker容器化打包**：隔离构建环境
3. **构建缓存优化**：Maven依赖本地缓存，加速构建
4. **断点续传**：大文件传输支持断点续传
5. **灰度发布**：支持多服务器依次部署
6. **版本回退增强**：支持回滚到指定历史版本
