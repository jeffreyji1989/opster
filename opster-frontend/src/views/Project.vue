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
      <el-table-column label="代码仓库" width="120">
        <template #default="{ row }">
          <el-tag>{{ row.repositoryCount || 0 }} 个仓库</el-tag>
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

    <!-- 项目编辑弹窗 -->
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
        <el-form-item label="业务线">
          <el-input v-model="form.businessLine" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 代码仓库管理 -->
        <el-form-item label="代码仓库">
          <el-button @click="handleAddRepository" type="primary" plain size="small">
            + 新增代码仓库
          </el-button>

          <el-table :data="form.repositories" style="width: 100%; margin-top: 10px" border>
            <el-table-column prop="name" label="仓库名称" width="200" />
            <el-table-column prop="gitUrl" label="Git 地址" show-overflow-tooltip />
            <el-table-column prop="gitAccountName" label="Git 账号" width="200" />
            <el-table-column label="操作" width="150">
              <template #default="{ $index }">
                <el-button size="small" @click="handleEditRepository($index)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDeleteRepository($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 代码仓库编辑弹窗 -->
    <el-dialog v-model="repositoryDialogVisible" :title="repositoryForm.id ? '编辑代码仓库' : '新增代码仓库'" width="600px">
      <el-form :model="repositoryForm" label-width="120px">
        <el-form-item label="仓库名称" required>
          <el-input v-model="repositoryForm.name" placeholder="例如: 前端项目" />
        </el-form-item>
        <el-form-item label="Git 地址" required>
          <el-input v-model="repositoryForm.gitUrl" placeholder="https://github.com/xxx/xxx.git" />
        </el-form-item>
        <el-form-item label="Git 账号" required>
          <el-select v-model="repositoryForm.gitAccountId" placeholder="请选择 Git 账号" style="width: 100%">
            <el-option
              v-for="account in gitAccountList"
              :key="account.id"
              :label="`${account.accountName} (${account.gitPlatform})`"
              :value="account.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="repositoryForm.description" type="textarea" :rows="3" placeholder="请输入描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repositoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRepository">确认</el-button>
      </template>
    </el-dialog>

    <!-- 仓库详情弹窗 -->
    <el-dialog v-model="repoDetailVisible" title="代码仓库详情" width="800px">
      <el-table :data="currentRepositories" border>
        <el-table-column prop="subProjectName" label="仓库名称" width="200" />
        <el-table-column prop="gitUrl" label="Git 地址" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as gitAccountApi from '../api/git-account'
import * as subProjectApi from '../api/sub-project'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const repoDetailVisible = ref(false)
const repositoryDialogVisible = ref(false)
const currentRepositories = ref([])
const gitAccountList = ref([])
const deletedRepositoryIds = ref([])

const form = reactive({
  id: null,
  projectCode: '',
  projectName: '',
  projectOwner: '',
  businessLine: '',
  status: 1,
  repositories: []
})

const repositoryForm = reactive({
  id: null,
  name: '',
  gitUrl: '',
  gitAccountId: null,
  description: ''
})

const queryForm = reactive({
  projectName: '',
  businessLine: '',
  status: ''
})

// 加载 Git 账号列表
const loadGitAccounts = async () => {
  try {
    const res = await gitAccountApi.getGitAccountList({ status: 1 })
    gitAccountList.value = res
  } catch (error) {
    console.error('加载 Git 账号列表失败:', error)
  }
}

// 加载项目列表
const fetchData = async () => {
  loading.value = true
  try {
    const params = {}
    if (queryForm.projectName) params.projectName = queryForm.projectName
    if (queryForm.businessLine) params.businessLine = queryForm.businessLine
    if (queryForm.status !== '') params.status = queryForm.status

    const res = await request.get('/project/list', { params })
    // 处理状态值，确保是数字类型，避免菜单切换时触发 el-switch 的 change 事件
    tableData.value = await Promise.all(res.map(async (item) => {
      let statusValue = 0
      if (item.status === 'ENABLED' || item.status === 1 || item.status === '1') {
        statusValue = 1
      } else if (item.status === 'DISABLED' || item.status === 0 || item.status === '0') {
        statusValue = 0
      }

      // 获取代码仓库数量
      let repositoryCount = 0
      try {
        const repositories = await subProjectApi.getSubProjectsByProjectId(item.id)
        repositoryCount = repositories.length
      } catch (error) {
        console.error('获取代码仓库数量失败:', error)
      }

      return {
        ...item,
        repositoryCount,
        status: statusValue
      }
    }))
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  form.id = null
  form.projectCode = ''
  form.projectName = ''
  form.projectOwner = ''
  form.businessLine = ''
  form.status = 1
  form.repositories = []
  deletedRepositoryIds.value = []
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  // 复制项目基本信息
  Object.assign(form, {
    id: row.id,
    projectCode: row.projectCode,
    projectName: row.projectName,
    projectOwner: row.projectOwner,
    businessLine: row.businessLine || '',
    status: row.status ?? 1
  })

  // 加载代码仓库列表
  try {
    const repositories = await subProjectApi.getSubProjectsByProjectId(row.id)
    form.repositories = repositories.map(repo => {
      // 查找 Git 账号名称
      const gitAccount = gitAccountList.value.find(acc => acc.id === repo.gitAccountId)
      return {
        id: repo.id,
        name: repo.subProjectName,
        gitUrl: repo.gitUrl,
        gitAccountId: repo.gitAccountId,
        gitAccountName: gitAccount ? `${gitAccount.accountName} (${gitAccount.gitPlatform})` : '',
        description: repo.description
      }
    })
  } catch (error) {
    console.error('加载代码仓库失败:', error)
    form.repositories = []
  }

  deletedRepositoryIds.value = []
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    // 1. 先创建或更新项目
    const projectData = {
      projectCode: form.projectCode,
      projectName: form.projectName,
      projectOwner: form.projectOwner,
      businessLine: form.businessLine || null,
      status: form.status
    }

    let projectId
    if (form.id) {
      await request.put('/project', { ...projectData, id: form.id })
      projectId = form.id
    } else {
      const res = await request.post('/project', projectData)
      projectId = res.id
    }

    // 2. 处理代码仓库（子项目）
    for (const repo of form.repositories) {
      const subProjectData = {
        projectId: projectId,
        subProjectName: repo.name,
        gitAccountId: repo.gitAccountId,
        gitUrl: repo.gitUrl,
        status: 1
      }

      if (repo.id) {
        await subProjectApi.updateSubProject({ ...subProjectData, id: repo.id })
      } else {
        await subProjectApi.createSubProject(subProjectData)
      }
    }

    // 3. 删除需要删除的子项目
    for (const id of deletedRepositoryIds.value) {
      await subProjectApi.deleteSubProject(id)
    }

    ElMessage.success(form.id ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('操作失败')
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

const handleViewRepositories = async (row) => {
  try {
    const repositories = await subProjectApi.getSubProjectsByProjectId(row.id)
    currentRepositories.value = repositories
    repoDetailVisible.value = true
  } catch (error) {
    console.error('加载代码仓库详情失败:', error)
    ElMessage.error('加载代码仓库详情失败')
  }
}

// 新增代码仓库
const handleAddRepository = () => {
  Object.assign(repositoryForm, {
    id: null,
    name: '',
    gitUrl: '',
    gitAccountId: null,
    description: ''
  })
  repositoryDialogVisible.value = true
}

// 编辑代码仓库
const handleEditRepository = (index) => {
  const repo = form.repositories[index]
  Object.assign(repositoryForm, {
    ...repo
  })
  repositoryForm._index = index
  repositoryDialogVisible.value = true
}

// 删除代码仓库
const handleDeleteRepository = (index) => {
  const repo = form.repositories[index]
  if (repo.id) {
    // 标记为需要删除
    deletedRepositoryIds.value.push(repo.id)
  }
  form.repositories.splice(index, 1)
}

// 保存代码仓库
const handleSaveRepository = () => {
  // 验证必填字段
  if (!repositoryForm.name) {
    ElMessage.error('请输入仓库名称')
    return
  }
  if (!repositoryForm.gitUrl) {
    ElMessage.error('请输入 Git 地址')
    return
  }
  if (!repositoryForm.gitAccountId) {
    ElMessage.error('请选择 Git 账号')
    return
  }

  // 查找 Git 账号名称
  const gitAccount = gitAccountList.value.find(acc => acc.id === repositoryForm.gitAccountId)
  const gitAccountName = gitAccount ? `${gitAccount.accountName} (${gitAccount.gitPlatform})` : ''

  const repoData = {
    id: repositoryForm.id,
    name: repositoryForm.name,
    gitUrl: repositoryForm.gitUrl,
    gitAccountId: repositoryForm.gitAccountId,
    gitAccountName: gitAccountName,
    description: repositoryForm.description
  }

  if (repositoryForm._index !== undefined) {
    // 编辑模式
    form.repositories[repositoryForm._index] = repoData
  } else {
    // 新增模式
    form.repositories.push(repoData)
  }

  repositoryDialogVisible.value = false
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

onMounted(async () => {
  await loadGitAccounts()
  await fetchData()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
}
</style>
