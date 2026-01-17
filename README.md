# Opster System 开发指南

这是一个基于 Spring Boot 4.0.1 (后端) 和 Vue 3 (前端) 的全栈运维管理系统。

## 变更日志 (Change Log)

### Phase 1: 核心功能实现 (2026-01-17)
- **后端架构**: 升级至 JDK 25 + Spring Boot 4.0.1 + MyBatis-Plus 3.5.15 + SQLite + Hutool。
- **数据库设计**: 创建 `opster.sql`，包含 `project`, `server`, `service` 表，并集成自动审计字段 (`create_by`, `update_time` 等)。
- **模块实现**:
    - **项目管理**: CRUD (名称, 负责人, Git地址, 监控地址, 业务线, 状态)。
    - **服务器管理**: CRUD (IP, 别名, 账号密码, 分组, 环境, 状态)。
    - **服务管理**: CRUD (关联项目与服务器, 分支, 路径, 脚本) 及 模拟操作 (编译重启, 重启, 启动, 查看日志)。
    - **Dashboard**: 首页统计卡片 (项目数, 服务器数, 服务数)。
- **前端架构**: 集成 Vue Router 4, Element Plus, Axios。
- **UI 实现**: 侧边栏导航布局，完整的 CRUD 表格与表单弹窗，操作按钮集成。

## 目录结构

```
opster/
├── opster-backend/    # 后端项目 (Java/Spring Boot)
│   ├── src/main/java/com/opster/module/  # 业务模块 (project, server, service, dashboard)
│   └── src/main/java/com/opster/common/  # 公共组件 (BaseEntity, MetaObjectHandler)
├── opster-frontend/   # 前端项目 (Vue 3/Vite/Element Plus)
└── opster.sql         # 数据库初始化脚本
```

## 1. 环境准备

确保你的电脑上安装了以下软件：
- **Java JDK 25**: 运行后端 (环境变量配置)。
- **Maven 4+**: 构建后端 (环境变量配置)。
- **Node.js 18+**: 运行前端构建工具。

## 2. 运行后端 (Spring Boot)

1. 进入后端目录：
   ```bash
   cd opster-backend
   ```
2. 使用 Maven 运行：
   ```bash
   mvn spring-boot:run
   ```
   *注意: 系统会自动创建 SQLite 数据库文件 `opster.db` 并应用 `opster.sql` (需手动初始化或依赖 JPA/MyBatis 自动机制，当前为手动模式，请确保数据库表存在)。*
   *建议: 首次运行前可以使用 SQLite 客户端连接 `opster.db` 并执行 `opster.sql` 中的建表语句。*

后端启动后，将在 `http://localhost:8080` 监听。

## 3. 运行前端 (Vue)

1. 进入前端目录：
   ```bash
   cd opster-frontend
   ```
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```

前端启动后，访问 `http://localhost:5173` 即可看到系统首页。

## 4. 功能说明

### 4.1 基础功能
- 所有数据表包含审计字段，新增/更新时自动记录操作人(默认为 admin, id=1)和时间。
- 首页展示系统概览数据。

### 4.2 运维操作
在“服务管理”页面，可以对服务进行以下模拟操作：
- **编译并重启**: 模拟执行 Maven 打包命令和启动脚本。
- **重启/启动**: 模拟执行启动脚本。
- **查看日志**: 模拟读取日志文件内容。
*(注: 当前为模拟实现，真实环境需对接 SSH/Shell)*
