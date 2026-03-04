<template>
  <div class="service-monitor-container">
    <div class="header">
      <h2>服务监控</h2>
      <div class="actions">
        <el-button type="primary" @click="handleAdd">新增监控</el-button>
        <el-button @click="handleRefresh">刷新</el-button>
        <el-button @click="handleCheckAll">手动检测</el-button>
      </div>
    </div>

    <el-table :data="monitorList" v-loading="loading" stripe border>
      <el-table-column prop="projectName" label="项目名称" width="150" />
      <el-table-column prop="serviceName" label="服务名称" width="150" />
      <el-table-column prop="serverIp" label="服务器地址" width="140" />
      <el-table-column prop="port" label="端口号" width="80" />
      <el-table-column prop="monitorUrl" label="监控URL" min-width="200" show-overflow-tooltip />

      <el-table-column label="当前状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.currentStatus)" size="small">
            {{ getStatusText(row.currentStatus) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="当前耗时" width="100" align="right">
        <template #default="{ row }">
          {{ row.currentResponseTime || '-' }} ms
        </template>
      </el-table-column>

      <el-table-column label="上次状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.lastStatus !== null" :type="getStatusType(row.lastStatus)" size="small">
            {{ getStatusText(row.lastStatus) }}
          </el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <el-table-column label="上次耗时" width="200" align="right">
        <template #default="{ row }">
          <span v-if="row.lastResponseTime !== null">
            {{ row.lastResponseTime }} ms ({{ row.lastTimeStr }})
          </span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="text" @click="handleEdit(row)">编辑</el-button>
          <el-button type="text" @click="handleViewHistory(row.serviceId)">历史记录</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑监控对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增监控配置' : '编辑监控配置'"
      width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="选择服务" prop="serviceId" required>
          <el-select
            v-model="form.serviceId"
            placeholder="请选择服务"
            filterable
            clearable
            style="width: 100%"
            @change="handleServiceChange">
            <el-option
              v-for="item in availableServices"
              :key="item.id"
              :label="getServiceDisplayLabel(item)"
              :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="项目名称">
          <el-input v-model="selectedServiceInfo.projectName" disabled />
        </el-form-item>

        <el-form-item label="服务名称">
          <el-input v-model="selectedServiceInfo.serviceName" disabled />
        </el-form-item>

        <el-form-item label="服务器IP">
          <el-input v-model="selectedServiceInfo.serverIp" disabled />
        </el-form-item>

        <el-form-item label="端口号">
          <el-input v-model="selectedServiceInfo.port" disabled />
        </el-form-item>

        <el-form-item label="监控URL" prop="monitorUrl" required>
          <el-input
            v-model="form.monitorUrl"
            placeholder="例如: http://localhost:8080/health"
            clearable />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 历史记录对话框 -->
    <el-dialog v-model="historyDialogVisible" :title="`监控历史记录 - ${currentService?.serviceName}`" width="800px">
      <el-table :data="historyList" stripe border max-height="400">
        <el-table-column label="检测时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.recordTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseTime" label="响应耗时" width="100" align="right">
          <template #default="{ row }">
            {{ row.responseTime || '-' }} ms
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getMonitorStatistics, getMonitorHistory, checkAll as checkAllApi, getAvailableServices, updateMonitorUrl } from '../api/service-monitor'

// Data
const monitorList = ref([])
const loading = ref(false)
const historyDialogVisible = ref(false)
const historyList = ref([])
const currentService = ref(null)
const dialogVisible = ref(false)
const dialogMode = ref('add')
const saving = ref(false)
const availableServices = ref([])
const formRef = ref(null)

// 表单数据
const form = reactive({
  serviceId: null,
  monitorUrl: ''
})

// 选中的服务信息
const selectedServiceInfo = reactive({
  projectName: '',
  serviceName: '',
  serverIp: '',
  port: ''
})

// 表单验证规则
const rules = {
  serviceId: [
    { required: true, message: '请选择服务', trigger: 'change' }
  ],
  monitorUrl: [
    { required: true, message: '请输入监控URL', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL', trigger: 'blur' }
  ]
}

// 获取服务显示标签
const getServiceDisplayLabel = (service) => {
  return `${service.projectName} - ${service.serviceName || service.serviceAlias} (${service.serverIp}:${service.port})`
}

// 服务选择变化
const handleServiceChange = (serviceId) => {
  const service = availableServices.value.find(s => s.id === serviceId)
  if (service) {
    selectedServiceInfo.projectName = service.projectName
    selectedServiceInfo.serviceName = service.serviceName || service.serviceAlias
    selectedServiceInfo.serverIp = service.serverIp
    selectedServiceInfo.port = service.port
    form.monitorUrl = service.monitorUrl || ''
  }
}

// Methods
const loadMonitorList = async () => {
  loading.value = true
  try {
    const data = await getMonitorStatistics()
    monitorList.value = data || []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载监控数据失败')
  } finally {
    loading.value = false
  }
}

const loadAvailableServices = async () => {
  try {
    const data = await getAvailableServices()
    availableServices.value = data || []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载可用服务列表失败')
  }
}

const handleAdd = () => {
  dialogMode.value = 'add'
  form.serviceId = null
  form.monitorUrl = ''
  selectedServiceInfo.projectName = ''
  selectedServiceInfo.serviceName = ''
  selectedServiceInfo.serverIp = ''
  selectedServiceInfo.port = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogMode.value = 'edit'
  form.serviceId = row.serviceId
  form.monitorUrl = row.monitorUrl
  selectedServiceInfo.projectName = row.projectName
  selectedServiceInfo.serviceName = row.serviceName
  selectedServiceInfo.serverIp = row.serverIp
  selectedServiceInfo.port = row.port
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    saving.value = true

    await updateMonitorUrl(form.serviceId, form.monitorUrl)
    ElMessage.success('保存成功')
    dialogVisible.value = false

    // 刷新列表
    loadMonitorList()
    loadAvailableServices()
  } catch (e) {
    if (e !== false) {  // 忽略表单验证失败
      console.error(e)
      ElMessage.error('保存失败: ' + (e.message || '未知错误'))
    }
  } finally {
    saving.value = false
  }
}

const handleRefresh = () => {
  loadMonitorList()
}

const handleCheckAll = async () => {
  try {
    await checkAllApi()
    ElMessage.success('检测完成')
    loadMonitorList()
  } catch (e) {
    console.error(e)
  }
}

const handleViewHistory = async (serviceId) => {
  const service = monitorList.value.find(s => s.serviceId === serviceId)
  currentService.value = service

  try {
    const data = await getMonitorHistory(serviceId, 7)
    historyList.value = data || []
    historyDialogVisible.value = true
  } catch (e) {
    console.error(e)
    ElMessage.error('加载历史记录失败')
  }
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  // 处理数组格式 [2026, 3, 4, 16, 23, 22]
  if (Array.isArray(dateTime)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = dateTime
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
  }
  // 处理字符串格式
  if (typeof dateTime === 'string') {
    return dateTime.replace('T', ' ')
  }
  return dateTime
}

const getStatusType = (status) => {
  return status === 1 ? 'success' : 'danger'
}

const getStatusText = (status) => {
  return status === 1 ? '正常' : '异常'
}

// 初始化
const init = async () => {
  await Promise.all([
    loadMonitorList(),
    loadAvailableServices()
  ])
}

init()
</script>

<style scoped>
.service-monitor-container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.actions {
  display: flex;
  gap: 10px;
}

.text-muted {
  color: #999;
}
</style>