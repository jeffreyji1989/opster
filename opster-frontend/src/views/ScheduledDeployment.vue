<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增定时发版</el-button>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="任务名称" min-width="150" />
      <el-table-column prop="projectName" label="项目名称" min-width="120" />
      <el-table-column prop="serverIp" label="服务器IP" width="150">
        <template #default="scope">
          {{ scope.row.serverAlias ? scope.row.serverAlias + ' (' + scope.row.serverIp + ')' : scope.row.serverIp }}
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

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑定时发版' : '新增定时发版'" width="600px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="选择服务" prop="serviceId">
          <el-select v-model="form.serviceId" placeholder="请选择服务" style="width: 100%">
            <el-option v-for="item in services" :key="item.id" :label="`${getProjectName(item.projectId)} - ${getServerName(item.serverId)}`" :value="item.id" />
          </el-select>
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
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  serviceId: null,
  executeDate: '',
  executeTime: '',
  remark: ''
})

const rules = {
  name: [
    { required: true, message: '请输入任务名称', trigger: 'blur' }
  ],
  serviceId: [
    { required: true, message: '请选择服务', trigger: 'change' }
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

const handleAdd = () => {
  Object.assign(form, {
    id: null,
    name: '',
    serviceId: null,
    executeDate: '',
    executeTime: '',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (form.id) {
      await request.put('/scheduled-deployments', form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/scheduled-deployments', form)
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
