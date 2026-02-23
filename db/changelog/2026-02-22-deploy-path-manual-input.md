# 修改服务管理 - 部署路径改为手动输入

## 变更日期
2026-02-22

## 变更说明
将部署路径从"系统自动计算（只读）"改为"手动输入（可编辑）"，但同时提供"自动填充"按钮方便用户。

## 修改原因
1. 部署路径需要支持更灵活的配置
2. 不同服务可能需要部署到不同的路径
3. 用户需要能够自定义部署路径

## 修改内容

### 1. 部署路径输入框（Service.vue）

**变更位置**：第 260-278 行

**修改前**：
```vue
<el-form-item label="部署路径" label-width="80px">
  <el-input v-if="!form.id" :value="getComputedDeployPath(item)" readonly />
  <el-input v-else :value="getFullDeployPathForEdit(item)" readonly />
  <span style="font-size: 12px; color: #999;">
    服务器上的部署路径（系统自动计算，不可编辑）
  </span>
</el-form-item>
```

**修改后**：
```vue
<el-form-item label="部署路径" label-width="80px">
  <el-input v-model="item.deployPath" placeholder="请输入部署路径，例如：/var/opster/eip" clearable />
  <div style="margin-top: 5px;">
    <span style="font-size: 12px; color: #999;">
      服务器上的部署路径（手动填写）
    </span>
    <el-button
      v-if="!item.deployPath"
      type="primary"
      size="small"
      link
      @click="handleAutoFillDeployPath(item)">
      自动填充
    </el-button>
  </div>
</el-form-item>
```

### 2. 新增自动填充方法（Service.vue）

**变更位置**：第 828-853 行

```javascript
// 自动填充部署路径
const handleAutoFillDeployPath = (item) => {
  const project = projects.value.find(p => p.id === form.projectId)
  if (!project) {
    return ElMessage.warning('请先选择项目')
  }

  // 使用项目配置的 deployRootPath 作为基础路径
  const deployRootPath = project.deployRootPath || project.deployPath || ''
  const projectCode = project.projectCode || ''
  const projectPath = item.projectPath || ''

  // 构建路径：{deployRootPath}/{projectCode}/{projectPath}
  let fullPath = deployRootPath
  if (projectCode) {
    fullPath += '/' + projectCode
    if (projectPath) {
      fullPath += '/' + projectPath
    }
  }

  if (fullPath) {
    item.deployPath = fullPath
    ElMessage.success('已自动填充部署路径')
  } else {
    ElMessage.warning('无法计算部署路径，请检查项目配置')
  }
}
```

### 3. 初始化时自动填充默认路径

#### handleProjectChange 函数
**变更位置**：第 686-728 行

在创建第一个服务时，自动计算并填充默认部署路径：
```javascript
// 获取项目信息以计算部署路径
const project = projects.value.find(p => p.id === projectId)
const deployRootPath = project?.deployRootPath || project?.deployPath || ''
const projectCode = project?.projectCode || ''
const projectPath = firstSubProject.projectPath || ''

// 计算默认部署路径
const defaultDeployPath = deployRootPath && projectCode
  ? `${deployRootPath}/${projectCode}${projectPath ? '/' + projectPath : ''}`
  : ''

form.items = [{
  // ... 其他字段
  deployPath: defaultDeployPath,  // 自动填充默认路径
  // ...
}]
```

#### handleAddService 函数
**变更位置**：第 1188-1244 行

在添加新服务时，也自动计算并填充默认部署路径：
```javascript
// 计算默认部署路径
const deployRootPath = project.deployRootPath || project.deployPath || ''
const projectCode = project.projectCode || ''
if (deployRootPath && projectCode) {
  defaultDeployPath = `${deployRootPath}/${projectCode}${defaultProjectPath ? '/' + defaultProjectPath : ''}`
}

form.items.push({
  // ... 其他字段
  deployPath: defaultDeployPath,  // 自动填充默认路径
  // ...
})
```

## 功能说明

### 1. 手动输入模式
- 用户可以自由输入部署路径
- 支持清空输入框重新填写
- 提供占位符提示格式

### 2. 自动填充功能
- **触发条件**：当部署路径为空时，显示"自动填充"按钮
- **填充规则**：`{deployRootPath}/{projectCode}/{projectPath}`
- **用户反馈**：填充成功后显示提示消息

### 3. 初始化行为
- **新增服务时**：自动填充默认路径
- **编辑服务时**：显示数据库中保存的路径
- **添加多个服务时**：每个服务都自动填充默认路径

## 路径计算规则

### 基础路径格式
```
{deployRootPath}/{projectCode}
```

### 带 projectPath 的路径
```
{deployRootPath}/{projectCode}/{projectPath}
```

### 示例
- deployRootPath: `/var/opster`
- projectCode: `eip`
- projectPath: `backend`
- 完整路径: `/var/opster/eip/backend`

## 用户体验优化

### 1. 智能提示
- 输入框占位符显示示例路径
- 下方提示说明"手动填写"

### 2. 快捷操作
- 空值时显示"自动填充"按钮
- 一键填充计算后的路径
- 填充后按钮自动隐藏

### 3. 灵活性
- 可以使用自动填充的默认路径
- 也可以手动修改为自定义路径
- 支持清空后重新自动填充

## 向后兼容性

### 1. 旧数据支持
- 编辑旧服务时，显示数据库中保存的 `deployPath` 值
- 如果旧数据为空，用户可以点击"自动填充"或手动输入

### 2. 字段兼容
- `deployRootPath` 和 `deployPath` 都支持
- 优先使用 `deployRootPath`，兼容旧字段 `deployPath`

## 测试建议

### 1. 新增服务测试
- 选择项目后，验证部署路径自动填充
- 修改 `projectPath` 后，点击"自动填充"，验证路径更新
- 手动输入自定义路径，验证可以保存

### 2. 编辑服务测试
- 编辑已有服务，验证部署路径正确显示
- 清空路径后点击"自动填充"，验证功能正常

### 3. 多服务测试
- 添加多个服务，验证每个服务都有独立的部署路径
- 验证不同服务可以使用相同或不同的部署路径
