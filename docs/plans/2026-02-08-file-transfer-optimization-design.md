# 文件传输优化设计文档

**创建时间**: 2026-02-08
**作者**: Claude Code
**状态**: 设计阶段

## 一、问题分析

### 当前问题

1. **SCP 连接脆弱**：79MB 文件传输时容易因网络波动导致连接断开
2. **Session 管理简陋**：每次传输都创建新的 Session，没有连接池和保活机制
3. **重试机制不完善**：失败后简单重试，没有重新建立 Session
4. **缺乏断点续传**：传输失败后需要从头开始
5. **监控不足**：无法实时查看传输状态和进度
6. **配置不灵活**：超时、缓冲区等参数硬编码

### 设计目标

1. **高可靠性**：通过连接保活、智能重试、断点续传确保传输成功
2. **高性能**：通过连接池复用、并行传输、流式处理提升速度
3. **可观测性**：详细日志和进度推送，便于问题排查
4. **可配置性**：所有参数可通过配置文件调整

## 二、架构设计

### 分层架构

```
┌─────────────────────────────────────────┐
│   部署编排服务 (DeploymentOrchestration)  │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      文件传输服务 (FileTransferService)   │
│  - 协调传输策略                           │
│  - 进度跟踪和推送                         │
└─────────────────────────────────────────┘
                    ↓
┌──────────────────┬──────────────────────┐
│  SSH 连接池       │   传输策略层          │
│  - 连接复用       │   - SCP 策略          │
│  - 连接保活       │   - Rsync 策略        │
│  - 自动重连       │   - 分块传输          │
└──────────────────┴──────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      JSch (SSH 客户端库)                 │
└─────────────────────────────────────────┘
```

### 核心组件

#### 1. SSH 连接管理层

**SshConnectionPool**
- 维护服务器 → Session 的映射关系
- 每个服务器维护 1-2 个长连接（避免资源浪费）
- 连接保活：每 30 秒发送心跳包
- 连接健康检查：传输前验证连接可用性
- 自动重连：连接断开时自动重建
- 优雅关闭：应用关闭时释放所有连接

**SshConfig**
- 配置化的连接参数（超时、保活间隔、密钥交换等）
- 使用 JSch 的 ServerAliveInterval 和 ServerAliveCountMax 实现连接保活

#### 2. 传输策略层

**TransferStrategy 接口**
- 定义传输策略的统一接口
- 支持多种传输实现

**ScpTransferStrategy**
- 传统 SCP 传输（优化版）
- 支持分块传输和校验
- 增强错误处理

**RsyncTransferStrategy**
- 基于 rsync 的传输
- 支持断点续传
- 自动检测服务器 rsync 支持情况

**策略选择逻辑**
- 优先使用 rsync（如果可用）
- rsync 不可用时降级到优化版 SCP
- 根据网络状况动态调整

#### 3. 传输执行层

**FileTransferServiceImpl**
- 主传输服务，协调各层
- 实现智能重试策略
- 进度跟踪和推送

**TransferProgress**
- 实时进度计算
- 速度监控
- 剩余时间估算

**TransferRetryPolicy**
- 智能重试策略（指数退避）
- 失败后重建 Session
- 自动降级策略

#### 4. 监控和诊断层

**传输前检查**
- 本地文件完整性验证
- 服务器磁盘空间检查
- 服务器负载检查
- 网络连通性测试

**传输中监控**
- 实时速度监控
- 剩余时间估算
- 网络状态检测

**传输后报告**
- 成功率统计
- 失败原因分析
- 优化建议

## 三、配置设计

### application.yml 配置

```yaml
opster:
  # SSH 连接池配置
  ssh:
    pool:
      enabled: true                    # 启用连接池
      max-connections-per-server: 2    # 每个服务器最大连接数
      keep-alive-interval: 30000       # 保活间隔（毫秒）
      connection-timeout: 30000        # 连接超时（毫秒）
      session-timeout: 300000          # Session 超时（5分钟）
      server-alive-count-max: 3        # 保活失败最大次数

  # 文件传输配置
  transfer:
    buffer-size: 1048576               # 缓冲区大小（1MB）
    chunk-size: 5242880                # 分块大小（5MB，用于断点续传）
    max-retries: 3                     # 最大重试次数
    retry-delays: "0,2000,5000"        # 重试延迟（毫秒）
    progress-interval: 10              # 进度推送间隔（每10%）
    prefer-rsync: true                 # 优先使用 rsync
    rsync-timeout: 600000              # rsync 超时（10分钟）

    # 传输前检查
    pre-check:
      enabled: true                    # 启用传输前检查
      check-disk-space: true           # 检查磁盘空间
      check-server-load: true          # 检查服务器负载
      min-free-space-ratio: 0.2        # 最小剩余空间比例（20%）
      max-load-average: 8.0            # 最大平均负载
```

### OpsterProperties 配置类

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "opster")
public class OpsterProperties {
    // ... 现有配置 ...

    /**
     * SSH 连接池配置
     */
    private SshPoolConfig sshPool = new SshPoolConfig();

    /**
     * 文件传输配置
     */
    private TransferConfig transfer = new TransferConfig();

    @Data
    public static class SshPoolConfig {
        private Boolean enabled = true;
        private Integer maxConnectionsPerServer = 2;
        private Integer keepAliveInterval = 30000;
        private Integer connectionTimeout = 30000;
        private Integer sessionTimeout = 300000;
        private Integer serverAliveCountMax = 3;
    }

    @Data
    public static class TransferConfig {
        private Integer bufferSize = 1048576;
        private Integer chunkSize = 5242880;
        private Integer maxRetries = 3;
        private String retryDelays = "0,2000,5000";
        private Integer progressInterval = 10;
        private Boolean preferRsync = true;
        private Integer rsyncTimeout = 600000;
        private PreCheckConfig preCheck = new PreCheckConfig();
    }

    @Data
    public static class PreCheckConfig {
        private Boolean enabled = true;
        private Boolean checkDiskSpace = true;
        private Boolean checkServerLoad = true;
        private Double minFreeSpaceRatio = 0.2;
        private Double maxLoadAverage = 8.0;
    }
}
```

## 四、数据流设计

### 完整传输流程

```
1. 部署请求
   ↓
2. 获取/创建 SSH Session（从连接池）
   ↓
3. 传输前检查
   - 本地文件验证
   - 服务器磁盘空间检查
   - 服务器负载检查
   ↓
4. 选择传输策略
   - 检测服务器 rsync 支持情况
   - rsync 可用 → RsyncTransferStrategy
   - rsync 不可用 → ScpTransferStrategy（优化版）
   ↓
5. 执行传输
   - 实时进度推送
   - 网络速度监控
   - 剩余时间估算
   ↓
6. 传输后验证
   - 远程文件大小校验
   - 可选：MD5 校验
   ↓
7. 处理结果
   - 成功：更新状态，继续部署
   - 失败：智能重试或降级策略
   ↓
8. 归还 Session 到连接池
```

### 智能降级策略

- **Rsync 失败** → 降级到分块 SCP
- **SCP 失败** → 降低缓冲区大小重试
- **所有策略失败** → 记录详细错误，抛出异常

### 智能重试策略

**TransferRetryPolicy 重试逻辑：**

1. **第 1 次失败**：立即重试（可能是临时网络波动）
2. **第 2 次失败**：等待 2 秒后重建 Session 重试
3. **第 3 次失败**：等待 5 秒后更换传输策略（SCP → Rsync）
4. **记录详细信息**：每次失败的错误码、堆栈信息、已传输字节数

## 五、错误处理

### 错误分类

#### 1. 可恢复错误（自动重试）
- **网络超时**：重建 Session 后重试
- **连接断开**：重新连接后断点续传
- **临时服务器繁忙**：延迟后重试

#### 2. 需要人工介入的错误
- **认证失败**：密码错误或权限不足
- **磁盘空间不足**：清理空间或更换路径
- **文件系统错误**：服务器存储问题

#### 3. 自动降级场景
- **Rsync 未安装** → 降级到 SCP
- **大文件传输不稳定** → 启用分块传输模式
- **网络不稳定** → 降低并发和缓冲区大小

### 错误日志增强

每次传输失败记录：
- 失败阶段（连接/传输/验证）
- 错误码和详细信息
- 已传输字节数（用于断点续传）
- 网络状态（延迟、丢包率）
- 服务器状态（负载、内存）
- 建议的解决方案

### 监控指标

- 传输成功率
- 平均传输速度
- 重试次数分布
- 各策略使用比例
- 常见错误统计

## 六、核心功能实现要点

### 1. SSH 连接池实现

```java
@Component
public class SshConnectionPool {
    private final Map<String, Queue<Session>> connectionPool = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor;

    public Session getSession(Server server) throws Exception {
        String key = buildKey(server);
        Queue<Session> sessions = connectionPool.get(key);

        if (sessions != null && !sessions.isEmpty()) {
            Session session = sessions.poll();
            if (session.isConnected()) {
                return session;
            }
        }

        return createNewSession(server);
    }

    public void returnSession(Server server, Session session) {
        String key = buildKey(server);
        connectionPool.computeIfAbsent(key, k -> new LinkedList<>()).offer(session);
    }

    // 心跳保活
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        // 向所有活跃 Session 发送心跳包
    }
}
```

### 2. Rsync 传输策略

```java
public class RsyncTransferStrategy implements TransferStrategy {
    @Override
    public void transfer(Path localFile, Session session, String remotePath,
                        WebSocketSession wsSession) throws Exception {
        // 检查 rsync 是否可用
        if (!isRsyncAvailable(session)) {
            throw new UnsupportedOperationException("rsync not available");
        }

        // 构建 rsync 命令
        String command = buildRsyncCommand(localFile, remotePath);

        // 执行传输
        executeRsyncCommand(session, command, wsSession);
    }

    private String buildRsyncCommand(Path localFile, String remotePath) {
        return String.format(
            "rsync -avz --progress --partial --timeout=600 %s %s",
            localFile, remotePath
        );
    }
}
```

### 3. 传输前检查

```java
public class PreTransferChecker {
    public TransferCheckResult check(Path localFile, Server server, Session session) {
        TransferCheckResult result = new TransferCheckResult();

        // 1. 本地文件检查
        result.addCheck(checkLocalFile(localFile));

        // 2. 服务器磁盘空间检查
        result.addCheck(checkDiskSpace(server, localFile, session));

        // 3. 服务器负载检查
        result.addCheck(checkServerLoad(session));

        return result;
    }
}
```

## 七、实施计划

### 阶段一：基础优化（优先级高）

**任务清单：**

1. **优化 FileTransferServiceImpl.java**
   - [ ] 增强错误处理和日志
   - [ ] 优化流刷新策略
   - [ ] 改进重试机制（失败后重建 Session）
   - [ ] 添加详细的进度信息推送

2. **优化 SshUtils.java**
   - [ ] 添加连接保活配置
   - [ ] 增加超时配置参数
   - [ ] 改进错误日志

### 阶段二：连接池和配置化

3. **创建 SshConnectionPool 类**
   - [ ] 实现连接池核心逻辑
   - [ ] 实现连接保活机制
   - [ ] 实现健康检查
   - [ ] 实现优雅关闭

4. **扩展 OpsterProperties**
   - [ ] 添加 SshPoolConfig 配置类
   - [ ] 添加 TransferConfig 配置类
   - [ ] 添加 PreCheckConfig 配置类

5. **更新 application.yml**
   - [ ] 添加 SSH 连接池配置
   - [ ] 添加文件传输配置
   - [ ] 添加传输前检查配置

### 阶段三：断点续传和监控

6. **实现 RsyncTransferStrategy**
   - [ ] 实现 TransferStrategy 接口
   - [ ] 实现 rsync 传输逻辑
   - [ ] 实现进度解析

7. **实现传输前检查功能**
   - [ ] 创建 PreTransferChecker 类
   - [ ] 实现磁盘空间检查
   - [ ] 实现服务器负载检查

8. **增强进度推送和监控**
   - [ ] 添加传输速度监控
   - [ ] 添加剩余时间估算
   - [ ] 增强错误信息推送

### 阶段四：测试和优化

9. **编写测试**
   - [ ] 单元测试：连接池、重试策略
   - [ ] 集成测试：模拟网络中断、大文件传输

10. **性能测试和调优**
    - [ ] 测试不同缓冲区大小的影响
    - [ ] 测试不同分块大小的影响
    - [ ] 调优参数配置

## 八、兼容性保证

- **API 兼容**：保持现有 `FileTransferService` 接口不变
- **向下兼容**：新功能通过配置开关控制
- **默认行为**：默认使用优化的 SCP 策略
- **可选增强**：rsync 作为可选增强功能

## 九、预期效果

- **可靠性提升**：传输成功率从当前水平提升到 95%+
- **性能提升**：79MB 文件传输时间减少 30-50%（使用 rsync）
- **用户体验**：失败后可断点续传，避免重复传输
- **可维护性**：详细日志和监控，便于问题排查

## 十、风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| rsync 未安装 | 中 | 自动降级到 SCP，提示安装 rsync |
| 连接池资源泄漏 | 高 | 实现健康检查和自动清理，添加监控 |
| 并发传输冲突 | 中 | 连接池限制每服务器最大连接数 |
| 网络不稳定 | 高 | 智能重试 + 断点续传 + 降级策略 |

## 十一、后续优化方向

1. **SFTP 支持**：作为另一种传输选项
2. **压缩传输**：对于文本类文件启用压缩
3. **多线程传输**：超大文件分片并行传输
4. **传输加密**：支持自定义加密算法
5. **传输代理**：支持通过跳板机传输
