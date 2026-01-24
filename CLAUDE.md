# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# 特定规则
- 所有回复都必须是中文
- 所有代码都必须是中文注释
- 所有给出的执行计划都存储到 `plan.md` 文件中
- 所有的sql变更都存储到 `db/changelog` 目录下,新生成的表或者字段的变更都必须在sql文件中进行记录

## 项目概述

**Opster** 是一个全栈运维管理系统,用于服务器管理、服务部署和监控。

### 技术栈
- **后端**: Java 25, Spring Boot 3.5.9, Spring Data JPA, SQLite, Spring AI Alibaba (DashScope)
- **前端**: Vue 3 (Composition API), Vite, Element Plus, Xterm.js
- **通信**: REST API + WebSocket (实时日志、终端)

## 开发命令

### 后端运行
```bash
cd opster-backend
mvn spring-boot:run
```
启动后访问 http://localhost:8080

### 前端运行
```bash
cd opster-frontend
npm install      # 首次运行
npm run dev      # 开发模式 (http://localhost:5173)
npm run build    # 生产构建
npm run preview  # 预览生产构建
```

## 架构概览

### 后端架构 - 模块化分层设计

后端采用按功能模块划分的分层架构:

```
opster-backend/src/main/java/com/opster/
├── config/          # 配置类 (JPA, WebSocket, Web)
├── common/          # 公共组件
│   └── enums/       # 状态枚举 (DeploymentStatus, RunStatus, Status)
├── handler/         # WebSocket 处理器
│   ├── LogWebSocketHandler         # 实时日志流
│   └── MonitorWebSocketHandler     # 服务监控状态推送
├── module/          # 功能模块 (每个模块包含 controller/service/repository/entity)
│   ├── dashboard/   # 统计数据
│   ├── deployment/  # 部署记录管理
│   ├── monitor/     # 服务监控
│   ├── project/     # 项目管理
│   ├── schedule/    # 定时部署任务
│   ├── server/      # 服务器管理
│   ├── service/     # 服务管理 (关联项目与服务器)
│   └── terminal/    # Web终端 + AI命令生成
└── OpsterApplication.java
```

**关键设计模式**:
- **BaseEntity**: 所有实体类的基类,包含审计字段 (create_by, create_time, update_by, update_time, del_flag)
- **模块化**: 每个功能模块独立包含完整的 MVC 层
- **WebSocket 双向通信**: 用于日志流、终端输出、监控状态实时推送
- **JSch SSH 连接**: 通过 JSch 库执行远程服务器命令

### 前端架构

```
opster-frontend/src/
├── api/             # Axios API 封装
├── views/           # 页面组件
│   ├── Dashboard.vue
│   ├── Project.vue
│   ├── Server.vue
│   ├── Service.vue
│   ├── Monitor.vue
│   ├── DeploymentRecord.vue
│   └── ScheduledDeployment.vue
├── layout/
│   └── Layout.vue   # 主布局 (侧边栏导航)
└── App.vue
```

**关键特性**:
- **Vue 3 Composition API**: 使用 `<script setup>` 语法
- **Element Plus**: UI 组件库
- **Xterm.js**: 浏览器终端组件
- **Axios**: HTTP 客户端,配置了 `/api` 代理到后端 8080 端口

### 数据库设计

主要实体表:
- **project**: 项目元数据 (Git 地址、监控地址、业务线等)
- **server**: 服务器信息 (IP、账号密码、分组、环境等)
- **service**: 关联项目与服务器 (分支、路径、部署脚本)
- **deployment_record**: 部署历史记录
- **scheduled_deployment**: 定时部署任务

所有表都包含审计字段,通过 `MetaObjectHandler` 自动填充。

## 配置说明

### 后端配置 (`application.yml`)

关键配置项:
- **端口**: 8080
- **数据库**: SQLite (自动 DDL 更新)
- **AI 配置**: DashScope API Key (qwen-plus 模型用于命令生成)
- **外部工具路径**: `opster.maven-home` 和 `opster.java-home` (本地执行功能需要)

### 前端配置 (`vite.config.js`)

- **开发端口**: 5173 (自动打开浏览器)
- **API 代理**: `/api` → `http://localhost:8080`

## 核心功能模块

### 1. 项目管理 (Project)
- CRUD 项目信息
- 字段: 名称、负责人、Git 地址、监控地址、业务线、状态

### 2. 服务器管理 (Server)
- CRUD 服务器信息
- 字段: IP、别名、账号密码、分组、环境、状态
- 支持 SSH 连接测试

### 3. 服务管理 (Service)
- 关联项目与服务器
- 配置部署脚本和路径
- 操作: 编译重启、重启、启动、查看日志

### 4. 部署记录 (Deployment)
- 记录部署历史
- 状态追踪 (通过枚举 DeploymentStatus)

### 5. 定时部署 (Schedule)
- 自动化部署任务调度
- Cron 表达式配置

### 6. Web 终端 (Terminal)
- 基于 Xterm.js 的浏览器终端
- WebSocket 实时双向通信
- AI 辅助命令生成 (通过 DashScope)

### 7. 服务监控 (Monitor)
- 实时监控服务状态
- WebSocket 推送状态更新
- 健康检查端点监控

## 开发约定

### 后端规范
- 使用 Spring Data JPA Repository 进行数据访问
- 所有实体继承 BaseEntity 以获得审计功能
- 使用 Lombok 减少样板代码
- RESTful API 设计规范
- WebSocket 处理器用于实时数据流

### 前端规范
- 使用 Vue 3 Composition API 和 `<script setup>`
- Element Plus 组件库
- API 调用统一使用 Axios
- 响应式状态管理

### 通信模式
- **CRUD 操作**: 使用 REST API
- **实时数据**: 使用 WebSocket (日志、终端、监控状态)

## 常见任务

### 添加新功能模块
1. 在 `opster-backend/src/main/java/com/opster/module/` 下创建新模块目录
2. 创建 entity, repository, service, controller
3. 在 `opster-frontend/src/views/` 创建对应 Vue 组件
4. 在 Layout.vue 中添加路由和导航项

### WebSocket 通信
- 终端: `ws://localhost:8080/terminal`
- 日志: `ws://localhost:8080/log`
- 监控: `ws://localhost:8080/monitor`

### 数据库操作
- SQLite 数据库文件: `opster.db`
- 初始化脚本: `opster.sql`
- JPA 自动更新 DDL: `spring.jpa.hibernate.ddl-auto: update`
