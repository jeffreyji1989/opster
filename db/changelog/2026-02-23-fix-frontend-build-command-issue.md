# 修复前端项目使用了错误的构建命令问题

## 问题描述

前端项目发版时，虽然系统正确识别为前端项目（使用 npm 构建），但**构建命令**却是 Maven 命令，导致构建失败。

## 日志分析

从日志 `20260223001408791_service12.log` 可以看出：

```
[2026-02-23 00:14:09] === 开始npm项目打包 ===
[2026-02-23 00:14:09] 构建命令: mvn clean package -DskipTests
...
[2026-02-23 00:14:18] 执行npm构建命令: mvn clean package -DskipTests --loglevel=verbose
[2026-02-23 00:14:19] Unable to parse command line options: Unrecognized option: --loglevel=verbose
[2026-02-23 00:14:19] >>> Command failed with exit code: 1
```

**问题：**
1. 系统识别为前端项目（使用 npm 构建）
2. 但构建命令是 `mvn clean package -DskipTests`（Maven 命令）
3. 系统在 Maven 命令后添加 npm 参数 `--loglevel=verbose`
4. 导致命令执行失败

## 根本原因

在数据库中，该前端服务的 `build_script` 字段存储的是 Maven 命令而不是 npm 命令。

**可能的原因：**
1. 创建服务时，服务类型被设置为后端（1），保存了 Maven 命令
2. 后来修改了服务类型为前端（0），但 `build_script` 字段没有同步更新
3. 或者创建时没有根据项目类型自动设置构建命令

## 修复方案

### 方案 1：执行 SQL 修复脚本（推荐）

**文件：** `db/changelog/2026-02-23-fix-frontend-build-script.sql`

```sql
-- 修复前端服务的 buildScript（service_type = 0, 2, 3 为前端项目）
UPDATE `service`
SET `build_script` = 'npm install && npm run build'
WHERE `service_type` IN (0, 2, 3)  -- 0-前端, 2-管理后台, 3-移动端
  AND (`build_script` IS NULL
       OR `build_script` = ''
       OR `build_script` LIKE 'mvn%');

-- 修复后端服务的 buildScript（service_type = 1 为后端项目）
UPDATE `service`
SET `build_script` = 'mvn clean package -DskipTests'
WHERE `service_type` = 1  -- 1-后端
  AND (`build_script` IS NULL
       OR `build_script` = '');
```

**执行方式：**
```bash
mysql -u your_user -p opster < db/changelog/2026-02-23-fix-frontend-build-script.sql
```

### 方案 2：手动编辑服务

1. 打开服务管理页面
2. 点击"编辑"按钮
3. 修改"编译脚本"字段：
   - **前端项目**：`npm install && npm run build`
   - **后端项目**：`mvn clean package -DskipTests`
4. 保存

### 方案 3：代码层面添加验证（防止未来再发生）

在 `AppServiceServiceImpl.save()` 方法中添加自动修正逻辑：

```java
@Override
public AppService save(AppService service) {
    boolean isNew = service.getId() == null;

    // 自动计算源码目录
    if (isNew || service.getSourcePath() == null || service.getSourcePath().isEmpty()) {
        String computedSourcePath = computeSourcePath(service);
        service.setSourcePath(computedSourcePath);
    }

    // ========== 新增：自动修正构建命令 ==========
    // 如果 serviceType 为空，尝试从 repositoryType 获取（兼容旧数据）
    if (service.getServiceType() == null && service.getRepositoryType() != null) {
        service.setServiceType(service.getRepositoryType());
    }

    // 根据 serviceType 自动设置默认构建命令
    if (service.getBuildScript() == null || service.getBuildScript().isEmpty()) {
        if (service.getServiceType() != null) {
            if (service.getServiceType() == 1) {
                // 后端项目
                service.setBuildScript("mvn clean package -DskipTests");
            } else {
                // 前端项目（0-前端, 2-管理后台, 3-移动端）
                service.setBuildScript("npm install && npm run build");
            }
        }
    }

    // ========== 其他兼容性处理 ==========

    // 如果 buildScript 为空，尝试从 mavenCmd 或 buildCmd 获取（兼容旧数据）
    if (service.getBuildScript() == null || service.getBuildScript().isEmpty()) {
        if (service.getMavenCmd() != null && !service.getMavenCmd().isEmpty()) {
            service.setBuildScript(service.getMavenCmd());
        } else if (service.getBuildCmd() != null && !service.getBuildCmd().isEmpty()) {
            service.setBuildScript(service.getBuildCmd());
        }
    }

    AppService savedService = appServiceRepository.save(service);

    // Update server deployed count
    if (isNew) {
        updateServerDeployedCount(savedService.getServerId(), 1);
    }

    return savedService;
}
```

## 验证方法

1. 执行 SQL 修复脚本
2. 重新发版前端项目
3. 检查日志，确认使用正确的构建命令：
   ```
   === 开始npm项目打包 ===
   构建命令: npm install && npm run build
   >>> 开始npm install...
   >>> 开始npm构建...
   执行npm构建命令: npm install && npm run build --loglevel=verbose
   ```

## 预防措施

### 1. 前端表单自动设置（已完成）

前端在新增服务时会根据项目类型自动设置默认构建命令：
- **前端项目**：`npm install && npm run build`
- **后端项目**：`mvn clean package -DskipTests`

### 2. 后端保存时验证（建议添加）

在 `AppServiceServiceImpl.save()` 中添加构建命令验证：
- 如果构建命令为空，根据服务类型自动设置
- 如果构建命令与服务类型不匹配，给出警告

### 3. 数据库约束（可选）

可以考虑在数据库层面添加检查约束，确保 `build_script` 与 `service_type` 匹配。

## 相关文件

- SQL 修复脚本：`db/changelog/2026-02-23-fix-frontend-build-script.sql`
- 前端表单：`opster-frontend/src/views/Service.vue`
- 后端服务：`opster-backend/src/main/java/com/opster/module/service/service/impl/AppServiceServiceImpl.java`
- 后端实体：`opster-backend/src/main/java/com/opster/module/service/entity/AppService.java`

## 服务类型映射

| service_type | 类型名称 | 默认构建命令 |
|-------------|---------|------------|
| 0 | 前端 | `npm install && npm run build` |
| 1 | 后端 | `mvn clean package -DskipTests` |
| 2 | 管理后台 | `npm install && npm run build` |
| 3 | 移动端 | `npm install && npm run build` |
