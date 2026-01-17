<template>
  <div class="page-container">
    <div class="toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="项目">
          <el-select v-model="queryForm.projectId" placeholder="选择项目" clearable style="width: 200px">
            <el-option v-for="item in projects" :key="item.id" :label="item.projectName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务器IP">
          <el-input v-model="queryForm.ip" placeholder="输入IP" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="projectName" label="项目名称" />
      <el-table-column prop="serverIp" label="服务器IP" width="150" />
      <el-table-column prop="serverAlias" label="服务器别名" />
      <el-table-column prop="status" label="当前状态" width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="最后更新时间" width="180">
        <template #default="scope">
          {{ formatTime(scope.row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-link v-if="scope.row.monitorUrl" :href="scope.row.monitorUrl" target="_blank" type="primary">
            打开监控 <el-icon><TopRight /></el-icon>
          </el-link>
          <span v-else class="text-gray">无监控地址</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../api/request'

const loading = ref(false)
const tableData = ref([])
const projects = ref([])

const queryForm = reactive({
  projectId: null,
  ip: ''
})

const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 0: '未启动', 1: '正常', 2: '异常' }
  return map[status] || '未知'
}

const formatTime = (timeArr) => {
  if (!timeArr) return '-'
  // Handle LocalDateTime array format [yyyy, MM, dd, HH, mm, ss]
  if (Array.isArray(timeArr)) {
    const [y, m, d, h, min, s] = timeArr
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`
  }
  return timeArr
}

const fetchProjects = async () => {
  try {
    const res = await request.get('/project/list')
    projects.value = res
  } catch (e) {
    console.error(e)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = {}
    if (queryForm.projectId) params.projectId = queryForm.projectId
    if (queryForm.ip) params.ip = queryForm.ip
    
    const res = await request.get('/service/monitor', { params })
    tableData.value = res
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  fetchData()
}

const handleReset = () => {
  queryForm.projectId = null
  queryForm.ip = ''
  fetchData()
}

onMounted(() => {
  fetchProjects()
  fetchData()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;
}
.text-gray {
  color: #909399;
  font-size: 12px;
}
</style>
