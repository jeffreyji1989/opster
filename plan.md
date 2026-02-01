# Node.js 版本管理实现方案

## 概述

本方案为 Opster 系统添加 Node.js 版本管理功能，实现不同前端项目可以使用不同的 Node.js 版本进行构建，类似现有的 Maven 和 Java 版本管理机制。

## 核心目标

1. **版本隔离**：每个前端服务可指定独立的 Node.js 版本
2. **自动安装**：通过 nvm 自动安装缺失的 Node.js 版本
3. **会话隔离**：不影响全局环境，仅在构建会话中生效
4. **用户友好**：在服务管理界面方便配置和查看

---

## 一、数据库设计

### 1.1 添加 Node.js 版本字段

**SQL 文件**: `/opster-backend/db/changelog/20260131_add_nodejs_version_field.sql`

```sql
-- 为服务表添加 Node.js 版本字段
ALTER TABLE service ADD COLUMN node_version VARCHAR(20) DEFAULT NULL;
```

**字段说明**：
- `node_version`: VARCHAR(20)，存储 Node.js 版本号（如 v18.17.0、v20.10.0）
- 允许 NULL，前端项目需要配置，后端项目为 NULL
- 默认值：NULL（未配置时使用系统默认 Node.js）

---

## 二、后端实现

### 2.1 配置文件修改

**文件**: `/opster-backend/src/main/resources/application.yml`

添加 Node.js 配置段：

```yaml
opster:
  # ... 现有配置 ...
  
  # 新增：Node.js 配置
  nodejs:
    # nvm 安装目录
    nvm-dir: ${NVM_DIR:-/Users/deffrey/.nvm}
    # 是否自动安装缺失的 Node.js 版本
    auto-install: true
    # Node 版本缓存目录
    cache-dir: ${opster.deploy-path}/nodejs-cache
```

### 2.2 配置类修改

**文件**: `/opster-backend/src/main/java/com/opster/config/OpsterProperties.java`

添加内部配置类：

```java
/**
 * Node.js 配置
 */
private NodejsConfig nodejs = new NodejsConfig();

@Data
public static class NodejsConfig {
    private String nvmDir;
    private boolean autoInstall = true;
    private String cacheDir;
}
```

### 2.3 实体类修改

**文件**: `/opster-backend/src/main/java/com/opster/module/service/entity/AppService.java`

添加字段：

```java
/**
 * Node.js 版本号（如：v18.17.0、v20.10.0）
 * 仅前端项目（repositoryType = 0 或 3）需要配置
 */
@Column(name = "node_version")
private String nodeVersion;
```

### 2.4 核心服务实现

**新建文件**: `/opster-backend/src/main/java/com/opster/module/service/service/NodeVersionService.java`

**新建文件**: `/opster-backend/src/main/java/com/opster/module/service/service/impl/NodeVersionServiceImpl.java`

主要功能：
- `getInstalledVersions()`: 获取已安装的 Node.js 版本列表
- `isVersionInstalled(String version)`: 检查版本是否已安装
- `installVersion(String version)`: 使用 nvm 安装指定版本
- `getVersionPath(String version)`: 获取版本的安装路径
- `isValidVersion(String version)`: 验证版本号格式

**新建文件**: `/opster-backend/src/main/java/com/opster/module/service/controller/NodeVersionController.java`

API 接口：
- `GET /nodejs/versions`: 获取已安装版本列表
- `GET /nodejs/versions/{version}/check`: 检查版本是否已安装
- `POST /nodejs/versions/{version}/install`: 安装指定版本

### 2.5 本地构建服务修改

**文件**: `/opster-backend/src/main/java/com/opster/module/service/service/impl/LocalBuildServiceImpl.java`

修改要点：
1. 在 `buildNpmArtifact` 方法中添加 Node 版本检测逻辑
2. 修改 `executeNpmInstall` 和 `executeNpmBuild` 方法，添加环境变量设置
3. 实现自动安装流程（检测到版本未安装时自动调用 nvm）

**关键实现逻辑**：

```java
// 1. 检测配置的 Node 版本
String nodeVersion = service.getNodeVersion();

// 2. 验证版本是否已安装
if (!nodeVersionService.isVersionInstalled(nodeVersion)) {
    // 自动安装
    nodeVersionService.installVersion(nodeVersion);
}

// 3. 获取版本路径并设置环境变量
String nodePath = nodeVersionService.getVersionPath(nodeVersion);
String[] envVars = {"PATH=" + nodePath + "/bin:" + systemPath};

// 4. 使用指定版本执行命令
LocalCommandUtils.executeCommand(projectRoot, "npm install", wsSession, envVars, logger);
```

---

## 三、前端界面实现

### 3.1 服务管理界面修改

**文件**: `/opster-frontend/src/views/Service.vue`

**修改点**：
1. 在表格中添加"Node版本"列（仅前端项目显示）
2. 在编辑表单中添加 Node.js 版本选择器
3. 添加响应式数据和加载逻辑

**表格列添加**：
```vue
<el-table-column label="Node版本" width="120">
  <template #default="scope">
    <span v-if="scope.row.repositoryType === 0 || scope.row.repositoryType === 3">
      {{ scope.row.nodeVersion || '系统默认' }}
    </span>
    <span v-else style="color: #ccc;">-</span>
  </template>
</el-table-column>
```

**表单选择器添加**：
```vue
<el-select v-model="item.nodeVersion" placeholder="选择Node.js版本" filterable allow-create>
  <el-option label="系统默认" value="" />
  <el-option v-for="version in installedNodeVersions" :key="version" :label="version" :value="version" />
</el-select>
```

---

## 四、自动化逻辑

### 4.1 构建前自动检测

```
开始构建
  ↓
检查 service.nodeVersion
  ↓
版本是否配置？
  ├─ 否 → 使用系统默认 Node.js
  └─ 是 → 检查版本是否已安装
         ├─ 是 → 获取版本路径 → 设置环境变量 → 继续
         └─ 否 → 自动安装开启？
                  ├─ 是 → nvm install → 验证 → 继续
                  └─ 否 → 抛出异常，构建失败
```

### 4.2 环境变量设置

通过修改 `PATH` 环境变量实现版本隔离：

```java
String nodePath = nodeVersionService.getVersionPath(nodeVersion);
String[] envVars = {
    "PATH=" + nodePath + "/bin:" + systemPath
};
```

这样在构建过程中，npm 会优先使用指定版本的 Node.js。

---

## 五、关键文件清单

### 5.1 需要修改的文件

**Java 文件**：
- `/opster-backend/src/main/java/com/opster/config/OpsterProperties.java` - 添加配置类
- `/opster-backend/src/main/java/com/opster/module/service/entity/AppService.java` - 添加字段
- `/opster-backend/src/main/java/com/opster/module/service/service/impl/LocalBuildServiceImpl.java` - 添加版本管理逻辑

**Vue 文件**：
- `/opster-frontend/src/views/Service.vue` - 添加版本选择器和显示

**配置文件**：
- `/opster-backend/src/main/resources/application.yml` - 添加 Node.js 配置

### 5.2 需要新建的文件

**Java 文件**：
- `/opster-backend/src/main/java/com/opster/module/service/service/NodeVersionService.java` - 服务接口
- `/opster-backend/src/main/java/com/opster/module/service/service/impl/NodeVersionServiceImpl.java` - 服务实现
- `/opster-backend/src/main/java/com/opster/module/service/controller/NodeVersionController.java` - API 控制器

**SQL 文件**：
- `/opster-backend/db/changelog/20260131_add_nodejs_version_field.sql` - 数据库变更

---

## 六、实施步骤

### 阶段 1：数据库和配置（1小时）
- 创建 SQL 变更文件
- 修改 application.yml
- 修改 OpsterProperties.java

### 阶段 2：后端核心服务（3小时）
- 创建 NodeVersionService 接口和实现
- 实现 nvm 集成逻辑
- 创建 NodeVersionController API
- 修改 LocalBuildServiceImpl

### 阶段 3：前端界面（2小时）
- 修改 Service.vue 添加版本选择器
- 添加版本列表加载逻辑
- 测试界面交互

### 阶段 4：测试验证（2小时）
- 测试版本检测功能
- 测试自动安装功能
- 测试前端项目构建
- 测试错误场景

### 阶段 5：文档和优化（1小时）
- 编写用户文档
- 添加代码注释
- 性能优化

**总计工时**：约 9 小时

---

## 七、注意事项

1. **nvm 依赖**：
   - 系统必须先安装 nvm（Node Version Manager）
   - 确保 `opster.nodejs.nvm-dir` 配置正确

2. **权限问题**：
   - nvm 安装目录需要读写权限
   - 系统级 nvm 可能需要 sudo 权限

3. **跨平台支持**：
   - 当前方案基于 Unix/Linux/macOS
   - Windows 需要使用 nvm-windows，路径不同

4. **版本格式**：
   - 版本号必须符合 v*.*.* 格式（如 v18.17.0）
   - 使用正则表达式验证，防止路径穿越攻击

5. **性能考虑**：
   - 版本检测不应阻塞构建流程
   - 大型项目建议异步安装版本

---

## 八、示例场景

### 场景 1：新项目配置 Node 版本

1. 新增前端服务（Vue 3 项目）
2. 在"Node版本"下拉框选择 v18.17.0
3. 保存配置
4. 执行发版，系统自动使用 v18.17.0 进行构建

### 场景 2：自动安装缺失版本

1. 配置 v22.0.0（系统中未安装）
2. 执行发版
3. 系统检测到版本未安装
4. 自动执行 `nvm install v22.0.0`
5. 安装成功后继续构建

### 场景 3：版本切换

1. 编辑服务配置
2. 修改 Node 版本从 v16.20.0 改为 v20.10.0
3. 保存配置
4. 下次发版自动使用新版本

---

## 附录：nvm 常用命令

```bash
# 列出已安装版本
nvm ls

# 安装指定版本
nvm install v18.17.0

# 切换版本
nvm use v18.17.0

# 设置默认版本
nvm alias default v18.17.0

# 查看远程可用版本
nvm ls-remote

# 卸载版本
nvm uninstall v18.17.0
```
