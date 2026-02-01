<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增项目</el-button>
      <el-form :inline="true" :model="queryForm" style="margin-left: 20px;">
        <el-form-item label="项目名称">
          <el-input v-model="queryForm.projectName" placeholder="请输入项目名称" style="width: 150px;" />
        </el-form-item>
        <el-form-item label="业务线">
          <el-input v-model="queryForm.businessLine" placeholder="请输入业务线" style="width: 150px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" style="width: 100px;">
            <el-option label="全部" value="" />
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="projectCode" label="项目编号" width="150" />
      <el-table-column prop="projectName" label="项目名称" />
      <el-table-column prop="projectOwner" label="负责人" />
      <el-table-column label="Git 仓库" width="120">
        <template #default="{ row }">
          <el-tag>{{ row.repositories?.length || 0 }} 个仓库</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="businessLine" label="业务线" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="handleToggleStatus(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="scope">
          <el-button size="small" @click="handleViewRepositories(scope.row)">查看仓库</el-button>
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑项目' : '新增项目'" width="1000px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="项目编号">
          <el-input v-model="form.projectCode" placeholder="请输入项目编号" />
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="form.projectName" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.projectOwner" />
        </el-form-item>
        <el-form-item label="Git 认证">
          <el-input v-model="form.gitUsername" placeholder="用户名（可选）" style="width: 200px; margin-right: 10px" />
          <el-input v-model="form.gitPassword" type="password" placeholder="密码（可选）" style="width: 200px" show-password />
          <span style="font-size: 12px; color: #999; margin-left: 10px">用于 Git 仓库认证（HTTP/HTTPS）</span>
        </el-form-item>
        <el-form-item label="Git 仓库">
          <div v-for="(repo, index) in form.repositories" :key="index" class="repo-container">
            <div class="repo-row">
              <el-select v-model="repo.type" placeholder="类型" style="width: 90px">
                <el-option label="前端" :value="0" />
                <el-option label="后端" :value="1" />
                <el-option label="管理后台" :value="2" />
                <el-option label="移动端" :value="3" />
              </el-select>

              <el-input v-model="repo.gitUrl" placeholder="Git 仓库地址" style="flex: 1" class="repo-git-url" />

              <el-input v-model="repo.projectPath" placeholder="项目路径" style="width: 140px" />

              <el-input v-model="repo.description" placeholder="描述" style="width: 100px" />

              <el-button @click="removeRepository(index)" :disabled="form.repositories.length <= 1" type="danger" plain icon="Delete">
              </el-button>
            </div>
          </div>

          <el-button @click="addRepository" type="primary" plain style="margin-top: 8px; width: 100%;">
            + 添加仓库
          </el-button>
        </el-form-item>
        <el-form-item label="部署根目录">
          <el-input v-model="form.deployPath" placeholder="例如: /data/deploy" />
        </el-form-item>
        <el-form-item label="业务线">
          <el-input v-model="form.businessLine" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 仓库详情弹窗 -->
    <el-dialog v-model="repoDetailVisible" title="仓库详情" width="800px">
      <el-table :data="currentRepositories" border>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            {{ getRepositoryTypeLabel(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="gitUrl" label="Git 地址" show-overflow-tooltip />
        <el-table-column prop="projectPath" label="项目路径" />
        <el-table-column prop="description" label="描述" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const repoDetailVisible = ref(false)
const currentRepositories = ref([])

// 仓库类型映射
const repositoryTypeMap = {
  0: '前端',
  1: '后端',
  2: '管理后台',
  3: '移动端'
}

// 获取仓库类型标签
const getRepositoryTypeLabel = (type) => {
  return repositoryTypeMap[type] || '未知'
}

const form = reactive({
  id: null,
  projectCode: '',
  projectName: '',
  projectOwner: '',
  gitUsername: '',
  gitPassword: '',
  repositories: [
    { type: 0, gitUrl: '', projectPath: '', description: '' },
    { type: 1, gitUrl: '', projectPath: '', description: '' }
  ],
  deployPath: '',
  businessLine: '',
  status: 1
})

const queryForm = reactive({
  projectName: '',
  businessLine: '',
  status: ''
})

const fetchData = async () => {
  loading.value = true
  try {
    const params = {}
    if (queryForm.projectName) params.projectName = queryForm.projectName
    if (queryForm.businessLine) params.businessLine = queryForm.businessLine
    if (queryForm.status !== '') params.status = queryForm.status

    const res = await request.get('/project/list', { params })
    // 处理状态值，确保是数字类型，避免菜单切换时触发 el-switch 的 change 事件
    tableData.value = res.map(item => {
      let statusValue = 0
      if (item.status === 'ENABLED' || item.status === 1 || item.status === '1') {
        statusValue = 1
      } else if (item.status === 'DISABLED' || item.status === 0 || item.status === '0') {
        statusValue = 0
      }
      // 确保 repositories 是数组
      const repositories = Array.isArray(item.repositories) ? item.repositories : []
      return {
        ...item,
        repositories,
        status: statusValue
      }
    })
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  form.id = null
  form.projectCode = ''
  form.projectName = ''
  form.projectOwner = ''
  form.gitUsername = ''
  form.gitPassword = ''
  form.repositories = [
    { type: 0, gitUrl: '', projectPath: '', description: '' },
    { type: 1, gitUrl: '', projectPath: '', description: '' }
  ]
  form.deployPath = ''
  form.businessLine = ''
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row) => {
  // 只复制需要的字段，避免包含已删除的数据库字段
  Object.assign(form, {
    id: row.id,
    projectCode: row.projectCode,
    projectName: row.projectName,
    projectOwner: row.projectOwner,
    gitUsername: row.gitUsername || '',
    gitPassword: row.gitPassword || '',
    repositories: Array.isArray(row.repositories) && row.repositories.length > 0
      ? JSON.parse(JSON.stringify(row.repositories))
      : [
          { type: 0, gitUrl: '', projectPath: '', description: '' },
          { type: 1, gitUrl: '', projectPath: '', description: '' }
        ],
    deployPath: row.deployPath || '',
    businessLine: row.businessLine || '',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    // 构建提交数据，只包含必要字段
    const data = {
      projectCode: form.projectCode,
      projectName: form.projectName,
      projectOwner: form.projectOwner,
      gitUsername: form.gitUsername || null,
      gitPassword: form.gitPassword || null,
      repositories: form.repositories,
      deployPath: form.deployPath || null,
      businessLine: form.businessLine || null,
      status: form.status
    }

    if (form.id) {
      // 编辑模式，添加 id
      data.id = form.id
      console.log('提交的数据:', JSON.stringify(data, null, 2))
      await request.put('/project', data)
      ElMessage.success('更新成功')
    } else {
      console.log('提交的数据:', JSON.stringify(data, null, 2))
      await request.post('/project', data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error('保存失败:', e)
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该项目?', '警告', {
    type: 'warning'
  }).then(async () => {
    await request.delete(`/project/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  })
}

const handleToggleStatus = async (row) => {
  try {
    // 确保发送的是数字类型的状态值
    const updatedRow = {
      ...row,
      status: row.status === 1 ? 1 : 0
    }
    await request.put('/project', updatedRow)
    ElMessage.success('状态更新成功')
  } catch (e) {
    ElMessage.error('状态更新失败')
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleViewRepositories = (row) => {
  currentRepositories.value = Array.isArray(row.repositories) ? row.repositories : []
  repoDetailVisible.value = true
}

const addRepository = () => {
  form.repositories.push({
    type: '',
    gitUrl: '',
    projectPath: '',
    description: ''
  })
}

const removeRepository = (index) => {
  if (form.repositories.length > 1) {
    form.repositories.splice(index, 1)
  }
}

const handleSearch = () => {
  fetchData()
}

const handleReset = () => {
  Object.assign(queryForm, {
    projectName: '',
    businessLine: '',
    status: ''
  })
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
}

.repo-container {
  margin-bottom: 8px;
  padding: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #fafafa;
}

.repo-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.repo-git-url {
  min-width: 0;
}
</style>
