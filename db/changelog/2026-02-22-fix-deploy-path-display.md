# 修复服务编辑 - 部署路径显示为空的问题

## 变更日期
2026-02-22

## 问题描述
服务管理编辑功能中，部署路径字段显示为空。

## 根本原因

### 1. 部署路径字段存储的是空值
- 数据库中 `AppService.deployPath` 字段可能为 `null`
- 该字段设计为手动填写，但实际使用中应该自动计算

### 2. 前端显示逻辑问题
- 编辑模式下，直接绑定 `item.deployPath`（数据库值）
- 当数据库值为空时，输入框显示为空
- 新增模式下使用 `getComputedDeployPath()` 计算路径并显示，但编辑模式没有使用相同逻辑

### 3. 目录结构已更新但计算函数未同步
- 之前已将目录结构从 `{deployPath}/{projectCode}/{serviceAlias}/` 简化为 `{deployPath}/{projectCode}/`
- 但前端的路径计算函数仍保留了旧的 `{serviceAlias}` 层级

## 修复方案

### 1. 更新部署路径显示组件（Service.vue）

**变更位置**：第 260-268 行

**修改前**：
```vue
<el-form-item label="部署路径" label-width="80px">
  <el-input v-model="item.deployPath" placeholder="请输入部署路径" clearable />
  <span style="font-size: 12px; color: #999;">
    服务器上的部署路径（可编辑）
  </span>
</el-form-item>
```

**修改后**：
```vue
<el-form-item label="部署路径" label-width="80px">
  <!-- 新增模式：显示计算后的路径（只读） -->
  <el-input v-if="!form.id" :value="getComputedDeployPath(item)" readonly />
  <!-- 编辑模式：显示计算后的完整路径（只读） -->
  <el-input v-else :value="getFullDeployPathForEdit(item)" readonly />
  <span style="font-size: 12px; color: #999;">
    服务器上的部署路径（系统自动计算，不可编辑）
  </span>
</el-form-item>
```

### 2. 添加编辑模式专用函数（Service.vue）

**变更位置**：第 1044-1063 行

```javascript
// 计算编辑模式下的完整部署路径
const getFullDeployPathForEdit = (item) => {
  const project = projects.value.find(p => p.id === form.projectId)
  if (!project) return ''

  // 使用项目配置的 deployRootPath 作为基础路径
  const deployRootPath = project.deployRootPath || project.deployPath || ''
  const projectCode = project.projectCode || ''

  // 从 Git URL 自动提取服务别名
  const gitUrl = item.repoGitUrl || ''
  const serviceAlias = extractServiceAliasFromGitUrl(gitUrl)
  const projectPath = item.projectPath || ''

  // 根据最新的目录结构构建路径：{deployRootPath}/{projectCode}
  // 注意：新目录结构移除了 {serviceAlias} 层级
  let fullPath = deployRootPath
  if (projectCode) {
    fullPath += '/' + projectCode
    // 如果有 projectPath，追加到路径末尾
    if (projectPath) {
      fullPath += '/' + projectPath
    }
  }
  return fullPath
}
```

### 3. 更新路径计算函数以匹配新目录结构

#### 更新 `getComputedDeployPath`（新增模式）
**变更位置**：第 1011-1034 行

```javascript
const getComputedDeployPath = (item) => {
  const project = projects.value.find(p => p.id === form.projectId)
  if (!project) return ''

  // 使用项目配置的 deployRootPath 作为基础路径
  const deployRootPath = project.deployRootPath || project.deployPath || ''
  const projectCode = project.projectCode || ''
  const projectPath = item.projectPath || ''

  // 根据最新的目录结构构建路径：{deployRootPath}/{projectCode}/{projectPath}
  let fullPath = deployRootPath
  if (projectCode) {
    fullPath += '/' + projectCode
    if (projectPath) {
      fullPath += '/' + projectPath
    }
  }
  return fullPath
}
```

#### 更新 `getFullDeployPath`（表格显示）
**变更位置**：第 981-1005 行

```javascript
const getFullDeployPath = (row) => {
  const project = projects.value.find(p => p.id === row.projectId)
  if (!project) return ''

  // 使用项目配置的 deployRootPath 作为基础路径
  const deployRootPath = project.deployRootPath || project.deployPath || ''
  const projectCode = project.projectCode || ''
  const projectPath = row.projectPath || ''

  // 根据最新的目录结构构建路径：{deployRootPath}/{projectCode}/{projectPath}
  let fullPath = deployRootPath
  if (projectCode) {
    fullPath += '/' + projectCode
    if (projectPath) {
      fullPath += '/' + projectPath
    }
  }
  return fullPath
}
```

## 目录结构对比

### 旧结构
```
{deployPath}/
└── {projectCode}/
    └── {serviceAlias}/          # 服务别名（从 Git URL 提取）
        ├── source/
        ├── artifacts/
        └── logs/
```

### 新结构
```
{deployPath}/
└── {projectCode}/              # 项目编码
    ├── source/                  # 源码目录（所有服务共享）
    ├── artifacts/               # 编译产物归档目录
    └── logs/                    # 日志目录
```

## 修复效果

### 1. 新增模式
- ✅ 部署路径：显示计算后的完整路径（只读）
- ✅ 格式：`{deployRootPath}/{projectCode}/{projectPath}`

### 2. 编辑模式
- ✅ 部署路径：显示计算后的完整路径（只读）
- ✅ 格式：`{deployRootPath}/{projectCode}/{projectPath}`
- ✅ 不再依赖数据库中的 `deployPath` 字段

### 3. 表格显示
- ✅ 自动显示计算后的部署路径
- ✅ 路径格式与新目录结构保持一致

## 注意事项

1. **部署路径改为只读**
   - 部署路径由系统根据项目配置自动计算
   - 用户无法手动编辑，避免路径配置错误
   - 如需修改部署路径，请修改项目配置中的 `deployRootPath`

2. **projectPath 的作用**
   - `projectPath` 用于区分同一仓库下的不同子项目
   - 例如：`backend`、`frontend`、`admin`
   - 如果没有子项目，`projectPath` 为空，路径为 `{deployRootPath}/{projectCode}`

3. **向后兼容**
   - 优先使用 `deployRootPath`，如果为空则使用 `deployPath`
   - 兼容旧的项目配置数据

## 测试建议

1. **测试新增服务**
   - 选择项目，验证部署路径正确显示
   - 切换不同的子项目，验证路径更新

2. **测试编辑服务**
   - 编辑已存在的服务，验证部署路径正确显示
   - 对比新增模式，确保路径格式一致

3. **测试表格显示**
   - 验证服务列表中的部署路径列正确显示
   - 确保路径格式与表单中的一致
