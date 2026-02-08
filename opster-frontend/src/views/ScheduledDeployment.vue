<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增定时发版</el-button>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="任务名称" min-width="150" />
      <el-table-column label="服务" min-width="200">
        <template #default="scope">
          <el-button link type="primary" @click="showTaskServices(scope.row)">
            查看 {{ scope.row.serviceCount }} 个服务
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="执行日期" width="120">
        <template #default="scope">
          {{ scope.row.executeDate }} {{ scope.row.executeTime }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)" :disabled="scope.row.status === 1">编辑</el-button>
          <el-button size="small" type="warning" @click="handleCancel(scope.row)" :disabled="scope.row.status !== 0">取消</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑/新增对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑定时发版' : '新增定时发版'" width="600px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="选择服务" prop="serviceIds">
          <el-button @click="serviceSelectorVisible = true" style="width: 100%">
            已选择 {{ selectedServiceList.length }} 个服务
          </el-button>
          <div v-if="selectedServiceList.length > 0" style="margin-top: 10px">
            <el-tag
              v-for="s in selectedServiceList"
              :key="s.id"
              closable
              @close="removeService(s.id)"
              style="margin: 5px"
            >
              {{ getServiceLabel(s) }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="执行日期" prop="executeDate">
          <el-date-picker
            v-model="form.executeDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="执行时间" prop="executeTime">
          <el-time-picker
            v-model="form.executeTime"
            placeholder="选择时间"
            format="HH:mm"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 服务选择对话框 -->
    <el-dialog v-model="serviceSelectorVisible" title="选择服务" width="90%">
      <el-table
        :data="availableServices"
        @selection-change="handleServiceSelectionChange"
        ref="serviceTableRef"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="项目" width="200">
          <template #default="scope">
            {{ getProjectDisplay(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <el-tag :type="getRepoTypeColor(scope.row.repositoryType)" size="small">
              {{ getRepoTypeLabel(scope.row.repositoryType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="服务器" width="150">
          <template #default="scope">
            {{ getServerIp(scope.row.serverId) }}
          </template>
        </el-table-column>
        <el-table-column prop="env" label="环境" width="80" />
        <el-table-column prop="gitBranch" label="分支" width="100" />
        <el-table-column label="Git地址" min-width="200">
          <template #default="scope">
            <span style="font-size: 12px; color: #666;">{{ scope.row.repoGitUrl || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="serviceSelectorVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmServiceSelection">
          确认选择 ({{ tempSelectedServices.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 任务服务列表对话框 -->
    <el-dialog v-model="taskServicesVisible" title="任务服务列表" width="70%">
      <el-table :data="taskServices">
        <el-table-column prop="serviceId" label="服务ID" width="80" />
        <el-table-column prop="projectName" label="项目" min-width="150" />
        <el-table-column label="服务器" min-width="150">
          <template #default="scope">
            {{ scope.row.serverAlias ? scope.row.serverAlias + ' (' + scope.row.serverIp + ')' : scope.row.serverIp }}
          </template>
        </el-table-column>
        <el-table-column prop="deployStatus" label="部署状态" width="100">
          <template #default="s">
            <el-tag :type="s.row.deployStatus === 1 ? 'success' : s.row.deployStatus === 2 ? 'danger' : 'info'">
              {{ s.row.deployStatus === 1 ? '成功' : s.row.deployStatus === 2 ? '失败' : '待执行' }}
            </el-tag>
          </template>
        </el-table-column>
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
const services = ref([])
const projects = ref([])
const servers = ref([])
const dialogVisible = ref(false)
const serviceSelectorVisible = ref(false)
const taskServicesVisible = ref(false)
const formRef = ref(null)
const serviceTableRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  serviceIds: [],
  executeDate: '',
  executeTime: '',
  remark: ''
})

const selectedServiceList = ref([])
const tempSelectedServices = ref([])
const availableServices = ref([])
const taskServices = ref([])

const rules = {
  name: [
    { required: true, message: '请输入任务名称', trigger: 'blur' }
  ],
  executeDate: [
    { required: true, message: '请选择执行日期', trigger: 'change' }
  ],
  executeTime: [
    { required: true, message: '请选择执行时间', trigger: 'change' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/scheduled-deployments')
    tableData.value = res
  } finally {
    loading.value = false
  }
}

const fetchServices = async () => {
  try {
    const [serviceRes, projectRes, serverRes] = await Promise.all([
      request.get('/service/list'),
      request.get('/project/list'),
      request.get('/server/list')
    ])
    services.value = serviceRes
    projects.value = projectRes
    servers.value = serverRes
    availableServices.value = serviceRes
  } catch (e) {
    console.error(e)
  }
}

const getProjectName = (id) => {
  const p = projects.value.find(i => i.id === id)
  return p ? p.projectName : id
}

const getServerName = (id) => {
  const s = servers.value.find(i => i.id === id)
  return s ? s.alias || s.ip : id
}

// 仓库类型映射
const repoTypeMap = {
  0: '前端',
  1: '后端',
  2: '管理后台',
  3: '移动端'
}

// 获取仓库类型标签
const getRepoTypeLabel = (type) => repoTypeMap[type] || '未知'

// 获取仓库别名(从项目的repositories列表中查找)
const getRepoAlias = (projectId, repoGitUrl) => {
  const project = projects.value.find(p => p.id === projectId)
  if (!project || !project.repositories || !repoGitUrl) return ''

  // 根据repoGitUrl查找对应的仓库
  const repo = project.repositories.find(r => {
    // 处理可能的.git后缀差异
    const normalizeUrl = (url) => url ? url.replace(/\.git$/, '') : ''
    return normalizeUrl(r.gitUrl) === normalizeUrl(repoGitUrl)
  })

  return repo ? (repo.alias || '') : ''
}

// 获取项目显示信息（项目名称-仓库别名）
const getProjectDisplay = (row) => {
  const p = projects.value.find(i => i.id === row.projectId)
  if (!p) return row.projectId

  const projectName = p.projectName

  // 从项目的 repositories 中找到对应的仓库别名
  // 通过 git仓库地址 + 项目路径 来区分
  let repoAlias = ''
  if (p.repositories && Array.isArray(p.repositories)) {
    const repo = p.repositories.find(r =>
      r.gitUrl === row.repoGitUrl &&
      (r.projectPath || '') === (row.projectPath || '')
    )
    if (repo && repo.alias) {
      repoAlias = repo.alias
    }
  }

  // 组合显示：项目名称-别名
  if (repoAlias) {
    return `${projectName}-${repoAlias}`
  }
  return projectName
}

// 获取仓库类型颜色
const getRepoTypeColor = (type) => {
  switch (type) {
    case 0: return 'success'  // 前端 - 绿色
    case 1: return 'primary'  // 后端 - 蓝色
    case 2: return 'warning'  // 管理后台 - 橙色
    case 3: return 'info'     // 移动端 - 灰色
    default: return ''
  }
}

// 获取服务器IP
const getServerIp = (id) => {
  const s = servers.value.find(i => i.id === id)
  return s ? (s.ip || '') : ''
}

// 获取服务显示标签
const getServiceLabel = (service) => {
  return `${getProjectDisplay(service)}-${getServerIp(service.serverId)}`
}

// 处理表格选择变化
const handleServiceSelectionChange = (selection) => {
  tempSelectedServices.value = selection
}

// 确认服务选择
const confirmServiceSelection = () => {
  selectedServiceList.value = [...tempSelectedServices.value]
  form.serviceIds = selectedServiceList.value.map(s => s.id)
  serviceSelectorVisible.value = false
}

// 移除已选择的服务
const removeService = (serviceId) => {
  selectedServiceList.value = selectedServiceList.value.filter(s => s.id !== serviceId)
  form.serviceIds = selectedServiceList.value.map(s => s.id)
}

const handleAdd = () => {
  Object.assign(form, {
    id: null,
    name: '',
    serviceIds: [],
    executeDate: '',
    executeTime: '',
    remark: ''
  })
  selectedServiceList.value = []
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  // 获取任务的服务列表
  try {
    const serviceList = await request.get(`/scheduled-deployments/${row.id}/services`)
    taskServices.value = serviceList

    Object.assign(form, {
      id: row.id,
      name: row.name,
      serviceIds: serviceList.map(s => s.serviceId),
      executeDate: row.executeDate,
      executeTime: row.executeTime,
      remark: row.remark
    })

    // 设置已选择的服务
    selectedServiceList.value = serviceList.map(s => {
      const service = services.value.find(svc => svc.id === s.serviceId)
      return service || { id: s.serviceId }
    }).filter(s => s.id)

    dialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取任务服务列表失败')
  }
}

// 显示任务的服务列表
const showTaskServices = async (row) => {
  try {
    const serviceList = await request.get(`/scheduled-deployments/${row.id}/services`)
    taskServices.value = serviceList
    taskServicesVisible.value = true
  } catch (e) {
    ElMessage.error('获取任务服务列表失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (form.serviceIds.length === 0) {
    ElMessage.warning('请至少选择一个服务')
    return
  }

  try {
    const payload = {
      task: {
        name: form.name,
        executeDate: form.executeDate,
        executeTime: form.executeTime,
        remark: form.remark
      },
      serviceIds: form.serviceIds
    }

    if (form.id) {
      await request.put('/scheduled-deployments', payload)
      ElMessage.success('更新成功')
    } else {
      await request.post('/scheduled-deployments', payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该定时发版任务?', '警告', {
    type: 'warning'
  }).then(async () => {
    await request.delete(`/scheduled-deployments/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  })
}

const handleCancel = (row) => {
  ElMessageBox.confirm('确认取消该定时发版任务?', '警告', {
    type: 'warning'
  }).then(async () => {
    await request.post(`/scheduled-deployments/${row.id}/cancel`)
    ElMessage.success('取消成功')
    fetchData()
  })
}

const getStatusType = (status) => {
  switch (status) {
    case 0:
      return 'info'
    case 1:
      return 'success'
    case 2:
      return 'danger'
    default:
      return ''
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 0:
      return '待执行'
    case 1:
      return '已完成'
    case 2:
      return '已取消'
    default:
      return ''
  }
}

onMounted(() => {
  fetchData()
  fetchServices()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
}
</style>
