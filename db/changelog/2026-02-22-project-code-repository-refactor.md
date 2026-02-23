# 项目管理代码仓库功能重构

## 变更日期
2026-02-22

## 变更概述
重新规划项目管理的代码仓库功能，复用现有的 SubProject 表来管理项目的代码仓库。

## 变更原因
1. 前端 Project.vue 仍在使用已删除的 `repositories` 字段
2. 需要实现一个项目对应多个代码仓库的功能
3. 需要集成 Git 账号选择功能

## 架构设计
```
Project (项目) 1:N SubProject (子项目/代码仓库) N:1 GitAccount (Git 账号)
```

## 数据库变更

### SubProject 表字段约束修改
将 SubProject 表的以下字段从必填改为可空：

**SQL 脚本**：`db/changelog/2026-02-22-alter-sub-project-nullable-fields.sql`

- **project_type** (项目类型) - 改为可空
- **git_branch** (Git 分支) - 改为可空
- **server_id** (部署服务器 ID) - 改为可空
- **deploy_path** (部署路径) - 改为可空

**对应的实体类修改**：
- `SubProject.java` - 移除上述字段的 `nullable = false` 约束

### 简化后的字段
- **Project 表**：保持现有字段不变
  - project_code (项目编号)
  - project_name (项目名称)
  - project_owner (负责人)
  - business_line (业务线)
  - status (状态)

- **SubProject 表（简化版）**：只保留代码仓库核心字段
  - project_id (关联项目 ID)
  - sub_project_name (仓库名称)
  - git_url (Git 仓库地址)
  - git_account_id (关联 Git 账号)
  - status (状态)

- **GitAccount 表**：Git 账号管理
  - account_name (账号名称)
  - git_platform (Git 平台: gitee/gitlab/github)
  - git_username (Git 用户名)
  - git_password (Git 密码)
  - auth_type (认证方式: 0-HTTPS, 1-SSH)
  - ssh_key_path (SSH 私钥路径)
  - ssh_key_passphrase (SSH 私钥密码)
  - description (描述)
  - status (状态)

## 前端变更

### 1. 创建 SubProject API 封装
**文件**：`opster-frontend/src/api/sub-project.js`

已创建完整的 SubProject API 封装，包括：
- `getSubProjectList(params)` - 查询子项目列表
- `getSubProjectById(id)` - 根据 ID 查询
- `getSubProjectsByProjectId(projectId)` - 根据项目 ID 查询
- `createSubProject(data)` - 新增子项目
- `updateSubProject(data)` - 更新子项目
- `deleteSubProject(id)` - 删除子项目
- `deploySubProject(id)` - 触发部署
- `getSubProjectConfigFiles(id)` - 获取配置文件
- `saveSubProjectConfigFiles(id, configFiles)` - 保存配置文件
- `countSubProjectsByProjectId(projectId)` - 统计子项目数量

### 2. 重构 Project.vue
**文件**：`opster-frontend/src/views/Project.vue`

#### 移除的旧代码
- 删除旧的 `repositories` 字段相关代码（Project 表的 JSON 字段）
- 删除 `gitUsername` 和 `gitPassword` 字段
- 删除旧的仓库类型选择（数字枚举改为字符串）

#### 新增的功能
1. **代码仓库管理区域**
   - 表格展示项目的所有代码仓库
   - 支持新增、编辑、删除代码仓库
   - 显示仓库名称、类型、Git 地址、分支、Git 账号

2. **代码仓库编辑弹窗**
   - 仓库名称（必填）
   - 项目类型（必填）：前端、后端、管理后台、移动端
   - 服务别名（可选）
   - Git 地址（必填）
   - Git 账号（必填，从 GitAccount 表选择）
   - Git 分支（必填）
   - 项目路径（可选）
   - 描述（可选）

3. **表单提交逻辑**
   - 先创建或更新项目
   - 然后处理代码仓库（子项目）
   - 最后删除标记为删除的子项目

4. **加载项目时同时加载代码仓库**
   - 从 SubProject 表加载项目的所有代码仓库
   - 显示 Git 账号名称（账号名 + 平台）

## 后端变更

### 无需修改后端代码
后端代码已经完整实现本次重构所需的功能：

- **SubProjectController**：提供完整的 CRUD API
- **SubProjectService**：实现业务逻辑
- **SubProjectRepository**：数据访问层
- **SubProject 实体**：数据模型

## 功能验证

### 测试步骤
1. 确保已创建 Git 账号（Git 账号管理页面）
2. 启动后端服务：`cd opster-backend && mvn spring-boot:run`
3. 启动前端服务：`cd opster-frontend && npm run dev`
4. 访问 http://localhost:5173
5. 点击侧边栏"项目管理"
6. 点击"新增项目"
7. 填写项目基本信息
8. 点击"新增代码仓库"
9. 填写代码仓库信息，选择 Git 账号
10. 提交表单
11. 验证项目创建成功，代码仓库关联成功

### 验证要点
- [ ] 项目基本信息保存正确
- [ ] 代码仓库保存到 SubProject 表
- [ ] Git 账号关联正确
- [ ] 编辑项目和代码仓库功能正常
- [ ] 删除代码仓库功能正常
- [ ] 查看代码仓库详情功能正常
- [ ] 代码仓库数量显示正确

## 注意事项

1. **无需修改数据库**：复用现有的 SubProject 表和 GitAccount 表
2. **无需修改后端代码**：后端功能已经完整实现
3. **前端为主**：主要工作在前端 Project.vue 的重构
4. **Git 账号管理**：确保已先创建 Git 账号，才能在代码仓库中选择
5. **数据迁移**：如果旧项目有 repositories 数据，需要手动迁移到 SubProject 表

## 数据迁移脚本（如果需要）

如果需要将旧 Project 表的 repositories JSON 字段数据迁移到 SubProject 表，可以使用以下脚本：

```sql
-- 示例迁移脚本（根据实际数据结构调整）
INSERT INTO sub_project (
    project_id,
    sub_project_name,
    project_type,
    git_url,
    git_branch,
    project_path,
    status,
    create_time,
    create_by
)
SELECT
    p.id AS project_id,
    json_extract.value, AS sub_project_name,
    -- 需要根据实际 JSON 结构解析
    ...
FROM project p
WHERE p.repositories IS NOT NULL;
```

建议手动迁移或编写专门的迁移脚本。

## 相关文件清单

### 需要修改的文件
1. `opster-frontend/src/views/Project.vue` - 重构项目管理页面

### 参考的文件（无需修改）
1. `opster-backend/src/main/java/com/opster/module/project/entity/SubProject.java` - 数据模型
2. `opster-backend/src/main/java/com/opster/module/project/controller/SubProjectController.java` - API 接口
3. `opster-frontend/src/views/GitAccountManagement.vue` - Git 账号管理参考

### 已创建的文件
1. `opster-frontend/src/api/sub-project.js` - SubProject API 封装
2. `db/changelog/2026-02-22-project-code-repository-refactor.md` - 本变更说明文档
