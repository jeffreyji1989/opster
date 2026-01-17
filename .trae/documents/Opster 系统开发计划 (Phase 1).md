# Opster 系统开发计划 (Phase 1)

根据您的需求，我们将开发一个基于 Spring Boot 4.0.1 + JDK 25 + SQLite + Vue 3 的运维管理系统。

## 1. 后端开发 (Spring Boot)

### 1.1 依赖与配置
- **依赖更新**: 在 `pom.xml` 中添加 `mybatis-plus-spring-boot3-starter` (3.5.15), `sqlite-jdbc`, `hutool-all`。
- **数据库配置**: 配置 SQLite 数据源及 MyBatis-Plus。
- **公共字段处理**: 创建 `BaseEntity` 及 `MyMetaObjectHandler`，实现 `create_by`, `update_by` 等字段的自动填充（默认用户 admin, id=1）。

### 1.2 数据库设计 & SQL
- 设计 `project` (项目管理), `server` (服务器管理), `service` (服务管理) 三张核心表。
- 编写建表语句并保存至项目根目录 `opster.sql`。

### 1.3 核心模块实现
所有代码将包含详细注释。
- **项目管理 (Project)**: CRUD 实现。
- **服务器管理 (Server)**: CRUD 实现。
- **服务管理 (Service)**:
    - CRUD 实现。
    - 扩展接口: `compileAndRestart`, `restart`, `start`, `viewLog` (使用 Hutool 实现 SSH/Shell 调用逻辑框架)。
- **首页监控 (Dashboard)**: 聚合统计项目数、服务器数、服务状态。

## 2. 前端开发 (Vue 3)

### 2.1 基础建设
- 安装 `vue-router` 用于页面导航。
- 安装 `element-plus` (推荐) 以快速构建表格和表单 UI。
- 封装 `axios` 统一处理 API 请求。

### 2.2 页面开发
- **Layout**: 包含侧边栏导航的主布局。
- **首页**: 展示统计卡片。
- **项目管理页**: 表格展示及新增/编辑弹窗。
- **服务器管理页**: 表格展示及新增/编辑弹窗。
- **服务管理页**: 关联项目与服务器，提供操作按钮 (部署、重启、日志)。

## 3. 文档与交付
- 更新 `README.md`: 包含系统功能说明、变更日志。
- 确保 `opster.sql` 包含所有 DDL。

## 实施步骤
1.  **配置更新**: 修改 `pom.xml` 和 `application.yml`。
2.  **SQL 编写**: 创建 `opster.sql`。
3.  **后端编码**: 依次实现 Entity, Mapper, Service, Controller。
4.  **前端编码**: 搭建路由和页面。
5.  **验证**: 启动前后端进行联调。
