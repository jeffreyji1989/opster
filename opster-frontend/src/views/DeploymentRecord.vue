<template>
  <div class="deployment-record-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>发版记录管理</span>
        </div>
      </template>
      <div class="card-body">
        <!-- 搜索栏 -->
        <el-form :inline="true" class="search-form">
          <el-form-item label="项目名称">
            <el-input v-model="searchForm.projectName" placeholder="请输入项目名称" clearable></el-input>
          </el-form-item>
          <el-form-item label="发版状态">
            <el-select v-model="searchForm.status" placeholder="请选择发版状态" clearable>
              <el-option label="进行中" :value="0"></el-option>
              <el-option label="完成" :value="1"></el-option>
              <el-option label="失败" :value="2"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 数据表格 -->
        <el-table :data="deploymentRecords" style="width: 100%" border>
          <el-table-column prop="id" label="记录ID" width="80"></el-table-column>
          <el-table-column prop="projectName" label="项目名称" min-width="120"></el-table-column>
          <el-table-column prop="serverIp" label="服务器IP" min-width="120"></el-table-column>
          <el-table-column prop="serverAlias" label="服务器别名" min-width="100"></el-table-column>
          <el-table-column prop="serviceName" label="服务名称" min-width="100"></el-table-column>
          <el-table-column prop="status" label="发版状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">{{ getStatusName(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="160"></el-table-column>
          <el-table-column prop="logPath" label="发版日志路径" min-width="200"></el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="scope">
              <el-button type="primary" size="small" @click="viewLogs(scope.row.id)">查看日志</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 日志弹窗 -->
        <el-dialog
          v-model="logDialogVisible"
          title="发版日志"
          width="80%"
          destroy-on-close
        >
          <pre class="log-content">{{ logContent }}</pre>
        </el-dialog>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

// 搜索表单
const searchForm = ref({
  projectName: '',
  status: ''
})

// 发版记录列表
const deploymentRecords = ref([])

// 日志弹窗
const logDialogVisible = ref(false)
const logContent = ref('')
const logSocket = ref(null)

// 初始化数据
onMounted(() => {
  fetchDeploymentRecords()
})

// 获取发版记录列表
const fetchDeploymentRecords = async () => {
  try {
    const response = await request.get('/deployment-records', {
      params: searchForm.value
    })
    deploymentRecords.value = response
  } catch (error) {
    ElMessage.error('获取发版记录失败')
    console.error('获取发版记录失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  fetchDeploymentRecords()
}

// 重置
const resetForm = () => {
  searchForm.value = {
    projectName: '',
    status: ''
  }
  fetchDeploymentRecords()
}

// 查看日志
const viewLogs = (id) => {
  logContent.value = '正在连接 WebSocket...'
  logDialogVisible.value = true

  // 使用环境变量配置的 WebSocket 地址
  const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/ws/log/deployment/${id}`
  
  try {
    const socket = new WebSocket(wsUrl)
    
    socket.onopen = () => {
      logContent.value = '>>> 连接成功，正在获取日志...\n'
    }
    
    socket.onmessage = (event) => {
      logContent.value += event.data + '\n'
      // 自动滚动到底部
      setTimeout(() => {
        const el = document.querySelector('.log-content')
        if (el) el.scrollTop = el.scrollHeight
      }, 0)
    }
    
    socket.onerror = (error) => {
      console.error('WebSocket Error:', error)
      logContent.value += '\n>>> 连接发生错误'
    }
    
    socket.onclose = () => {
      logContent.value += '\n>>> 连接已断开'
    }
    
    // 存储 socket 实例，以便在对话框关闭时关闭连接
    logSocket.value = socket
    
  } catch (e) {
    console.error(e)
    logContent.value = '无法建立连接: ' + e.message
  }
}

// 监听日志对话框可见性变化，关闭 WebSocket 连接
watch(logDialogVisible, (val) => {
  if (!val && logSocket.value) {
    logSocket.value.close()
    logSocket.value = null
  }
})

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 0:
      return 'warning'
    case 1:
      return 'success'
    case 2:
      return 'danger'
    default:
      return ''
  }
}

// 获取状态名称
const getStatusName = (status) => {
  switch (status) {
    case 0:
      return '进行中'
    case 1:
      return '完成'
    case 2:
      return '失败'
    default:
      return ''
  }
}
</script>

<style scoped>
.deployment-record-container {
  padding: 20px;
}

.card-header {
  font-size: 16px;
  font-weight: bold;
}

.search-form {
  margin-bottom: 20px;
}

.log-content {
  background: #1e1e1e;
  color: #fff;
  padding: 10px;
  border-radius: 4px;
  max-height: 500px;
  overflow: auto;
  font-family: monospace;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
