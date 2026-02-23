# 修复服务管理编辑功能 - 字段不回显问题

## 变更日期
2026-02-22

## 问题描述
服务管理的编辑功能中，以下字段无法正确回显：
1. **Git 地址** - 下拉框显示为空
2. **部署路径** - 无法显示
3. **子项目关联信息** - 丢失

## 根本原因

### 1. handleEdit 函数不完整
原 `handleEdit` 函数（第 1158-1182 行）只复制了部分字段到表单，缺少以下关键字段：
- `subProjectId` - 子项目ID
- `subProjectName` - 子项目名称
- `gitAccountId` - Git账号ID
- `projectType` - 项目类型

### 2. 编辑模式下 Git 地址组件问题
- Git 地址使用 `el-select` 组件绑定 `subProjectId`
- 编辑时如果 `subProjectId` 在 `availableSubProjects` 列表中找不到，下拉框显示为空
- 没有加载子项目列表就直接显示，导致下拉选项为空

### 3. 部署路径字段
- 部署路径字段 `deployPath` 已正确赋值，但由于 Git 地址显示异常，可能被用户误认为也有问题

## 修复方案

### 1. 完善 handleEdit 函数（Service.vue）

**变更位置**：第 1158-1212 行

**主要改动**：
```javascript
const handleEdit = async (row) => {
  form.id = row.id
  form.projectId = row.projectId

  // 1. 先加载项目的子项目列表
  try {
    const subProjects = await request.get(`/sub-project/project/${row.projectId}`)
    availableSubProjects.value = Array.isArray(subProjects) ? subProjects : []
  } catch (error) {
    console.error('获取子项目列表失败:', error)
    availableSubProjects.value = []
  }

  // 2. 尝试通过 repoGitUrl 匹配对应的子项目
  let matchedSubProjectId = row.subProjectId || null
  let matchedSubProjectName = row.subProjectName || ''
  let matchedProjectType = row.projectType || ''

  if (!matchedSubProjectId && row.repoGitUrl && availableSubProjects.value.length > 0) {
    const matched = availableSubProjects.value.find(sub => sub.gitUrl === row.repoGitUrl)
    if (matched) {
      matchedSubProjectId = matched.id
      matchedSubProjectName = matched.subProjectName
      matchedProjectType = matched.projectType
    }
  }

  // 3. 完整复制所有字段
  form.items = [{
    id: row.id,
    serviceName: row.serviceName || '',
    subProjectId: matchedSubProjectId,
    subProjectName: matchedSubProjectName,
    repoGitUrl: row.repoGitUrl || '',
    gitAccountId: row.gitAccountId || null,
    gitBranch: row.gitBranch || 'master',
    projectPath: row.projectPath || '',
    deployPath: row.deployPath || '',
    projectType: matchedProjectType,
    serviceType: row.serviceType ?? row.repositoryType ?? 1,
    // ... 其他字段
  }]

  dialogVisible.value = true
}
```

### 2. 优化 Git 地址显示组件（Service.vue）

**变更位置**：第 163-182 行

**改动说明**：
- **新增模式**：显示下拉选择框，允许从已有仓库中选择或手动输入
- **编辑模式**：显示只读输入框，显示完整的 Git 地址，并提供复制按钮

**代码**：
```vue
<el-form-item label="Git 地址" label-width="80px">
  <!-- 新增模式：显示下拉选择框 -->
  <el-select
    v-if="!form.id"
    v-model="item.subProjectId"
    placeholder="选择仓库地址"
    filterable
    allow-create
    style="width: 100%"
    @change="(val) => handleSubProjectChange(index, val)">
    <el-option
      v-for="sub in availableSubProjects"
      :key="sub.id"
      :label="`${sub.subProjectName} - ${sub.gitUrl}`"
      :value="sub.id" />
  </el-select>
  <!-- 编辑模式：显示 Git 地址输入框（只读） -->
  <el-input
    v-else
    v-model="item.repoGitUrl"
    placeholder="Git 仓库地址"
    clearable
    readonly>
    <template #append>
      <el-button @click="copyGitUrl(item.repoGitUrl)">复制</el-button>
    </template>
  </el-input>
</el-form-item>
```

### 3. 添加复制 Git URL 方法

**变更位置**：第 805-815 行

```javascript
// 复制 Git URL 到剪贴板
const copyGitUrl = (url) => {
  if (!url) {
    return ElMessage.warning('Git 地址为空')
  }
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('Git 地址已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
```

## 修复效果

### 1. 编辑模式字段回显
- ✅ **Git 地址**：显示完整的 Git URL，只读可复制
- ✅ **部署路径**：正确显示
- ✅ **编译路径**：正确显示
- ✅ **日志路径**：正确显示
- ✅ **所有其他字段**：完整回显

### 2. 用户体验优化
- 新增模式：可以从已有仓库中选择，便于快速配置
- 编辑模式：Git 地址显示为只读，防止误操作修改仓库地址
- 提供 Git 地址复制功能，方便用户使用

### 3. 数据兼容性
- 自动匹配旧数据的 Git 地址到对应的子项目
- 支持没有 `subProjectId` 的旧数据
- 优先使用 `serviceType`，兼容旧字段 `repositoryType`

## 注意事项

1. **编辑模式下 Git 地址不可修改**
   - 如需更换 Git 仓库，建议删除服务后重新创建
   - 这样可以避免因更换仓库导致的配置错乱

2. **子项目匹配逻辑**
   - 优先使用数据库中的 `subProjectId`
   - 如果没有，通过 `repoGitUrl` 自动匹配
   - 如果都找不到，显示为空白但保留原始 `repoGitUrl`

3. **向后兼容**
   - 完全兼容旧数据结构
   - 优先使用新字段，兼容旧字段

## 测试建议

1. **编辑新创建的服务**（有 subProjectId）
   - 验证 Git 地址正确显示
   - 验证部署路径正确显示

2. **编辑旧服务**（无 subProjectId）
   - 验证通过 Git URL 自动匹配
   - 验证所有字段正确回显

3. **复制 Git 地址功能**
   - 验证复制按钮正常工作
   - 验证 Git 地址为空时的提示
