# 修复服务管理部署路径显示问题

## 问题描述
服务管理中，编辑部署路径，保存后，再打开还是空的。

## 根因分析

### 问题定位
1. **前端行为**：
   - 表单中使用 `item.deployPath` 字段收集用户输入
   - 编辑时从 `row.deployPath` 赋值
   - 提交时将 `deployPath` 发送到后端

2. **后端行为**：
   - `AppService` 实体类中**没有** `deployPath` 字段
   - 前端发送的 `deployPath` 数据被 JPA 忽略
   - 数据库中 `deploy_path` 字段不存在

3. **历史原因**：
   - 在 2026-02-13 的数据库重构中（`2026-02-13-alter-service-table.sql`）
   - `deploy_path` 字段从 `service` 表中被删除
   - 设计意图是使用自动计算的路径：`{deployRootPath}/{projectCode}/{projectPath}`
   - 但用户仍需要手动覆盖默认路径的需求未考虑

## 解决方案

### 1. 数据库变更
**文件**：`db/changelog/2026-02-23-add-service-deploy-path.sql`

```sql
ALTER TABLE `service` ADD COLUMN `deploy_path` VARCHAR(500) DEFAULT NULL COMMENT '部署路径（用户手动填写，例如：/var/opster/eip）' AFTER `compile_path`;
```

### 2. 后端实体类变更
**文件**：`opster-backend/src/main/java/com/opster/module/service/entity/AppService.java`

添加字段：
```java
/**
 * 部署路径（用户手动填写）
 * 每个部署服务有独立的部署路径
 * 例如：/var/opster/eip
 */
@Column(name = "deploy_path", length = 500)
private String deployPath;
```

## 字段说明

### deploy_path 字段用途
- **用户手动输入**的部署路径
- 用于覆盖默认的自动计算路径
- 每个服务可以有独立的部署路径

### 路径计算逻辑
1. **默认路径**（自动计算）：
   ```
   {project.deployRootPath}/{project.projectCode}/{subProject.projectPath}
   ```

2. **用户自定义路径**：
   ```
   {service.deployPath}
   ```
   - 优先使用 `service.deployPath`（如果用户手动填写）
   - 否则使用默认自动计算的路径

## 部署步骤
1. 执行数据库变更 SQL
2. 重启后端服务（JPA 会自动更新表结构）
3. 测试编辑和保存部署路径功能

## 测试验证
1. 打开服务管理页面
2. 编辑某个服务的部署路径
3. 保存后关闭对话框
4. 再次打开编辑对话框
5. 验证部署路径正确显示

## 相关文件
- 前端：`opster-frontend/src/views/Service.vue`
- 后端实体：`opster-backend/src/main/java/com/opster/module/service/entity/AppService.java`
- 后端服务：`opster-backend/src/main/java/com/opster/module/service/service/impl/AppServiceServiceImpl.java`
- 数据库变更：`db/changelog/2026-02-23-add-service-deploy-path.sql`
