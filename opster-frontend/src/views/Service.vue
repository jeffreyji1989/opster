<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增服务</el-button>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="项目" width="120">
        <template #default="scope">
          {{ getProjectName(scope.row.projectId) }}
        </template>
      </el-table-column>
      <el-table-column label="服务器" width="150">
        <template #default="scope">
          {{ getServerName(scope.row.serverId) }}
        </template>
      </el-table-column>
      <el-table-column prop="env" label="环境" width="80" />
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="gitBranch" label="分支" width="100" />
      <el-table-column prop="deployPath" label="部署路径" show-overflow-tooltip />
      <el-table-column prop="monitorUrl" label="监控地址" show-overflow-tooltip />
      <el-table-column prop="status" label="运行状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用状态" width="120">
        <template #default="scope">
          <el-switch v-model="scope.row.enabled" active-value="1" inactive-value="0" @change="handleToggleEnabled(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="350">
        <template #default="scope">
          <el-button-group>
            <el-button size="small" type="primary" @click="handleAction(scope.row, 'compile-restart')">编译重启</el-button>
            <el-button size="small" type="warning" @click="handleAction(scope.row, 'restart')">重启</el-button>
            <el-button size="small" type="success" @click="handleAction(scope.row, 'start')">启动</el-button>
            <el-button size="small" type="info" @click="handleLog(scope.row)">日志</el-button>
          </el-button-group>
          <el-divider direction="vertical" />
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑服务' : '新增服务'">
      <el-form :model="form" label-width="120px">
        <el-form-item label="选择项目">
          <el-select v-model="form.projectId" placeholder="请选择项目">
            <el-option v-for="item in projects" :key="item.id" :label="item.projectName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择服务器">
          <el-select v-model="form.serverId" placeholder="请选择服务器">
            <el-option v-for="item in servers" :key="item.id" :label="`${item.alias} (${item.ip})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="环境">
          <el-select v-model="form.env" placeholder="请选择环境">
            <el-option label="生产" value="生产" />
            <el-option label="测试" value="测试" />
          </el-select>
        </el-form-item>
        <el-form-item label="端口号">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="Git分支">
          <el-input v-model="form.gitBranch" />
        </el-form-item>
        <el-form-item label="部署路径">
          <el-input v-model="form.deployPath" />
        </el-form-item>
        <el-form-item label="日志路径">
          <el-input v-model="form.logPath" />
        </el-form-item>
        <el-form-item label="Maven命令">
          <el-input v-model="form.mavenCmd" type="textarea" />
        </el-form-item>
        <el-form-item label="启动脚本">
          <el-input v-model="form.startScript" type="textarea" />
        </el-form-item>
        <el-form-item label="监控地址">
          <el-input v-model="form.monitorUrl" placeholder="请输入监控地址" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" active-value="1" inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Log Dialog -->
    <el-dialog v-model="logVisible" title="服务日志" width="70%">
      <pre class="log-content">{{ logContent }}</pre>
    </el-dialog>
    
    <!-- Action Result Dialog -->
    <el-dialog v-model="resultVisible" title="执行结果" width="80%" :close-on-click-modal="false">
      <div class="result-header">
        <el-tag :type="resultStatus === 'success' ? 'success' : 'danger'" size="large">
          {{ resultStatus === 'success' ? '执行成功' : '执行完成' }}
        </el-tag>
      </div>
      <div class="result-content">
        <pre class="log-content">{{ resultContent }}</pre>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="resultVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const projects = ref([])
const servers = ref([])

const dialogVisible = ref(false)
const logVisible = ref(false)
const logContent = ref('')

const resultVisible = ref(false)
const resultContent = ref('')
const resultStatus = ref('success')

const form = reactive({
  id: null,
  projectId: null,
  serverId: null,
  env: '测试',
  port: 8080,
  gitBranch: 'master',
  deployPath: '',
  logPath: '',
  mavenCmd: 'mvn clean package -DskipTests',
  startScript: './start.sh',
  monitorUrl: '',
  status: 0,
  enabled: 1
})

const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 0: '未启动', 1: '正常', 2: '异常' }
  return map[status] || '未知'
}

const getProjectName = (id) => {
  const p = projects.value.find(i => i.id === id)
  return p ? p.projectName : id
}

const getServerName = (id) => {
  const s = servers.value.find(i => i.id === id)
  return s ? s.alias : id
}

const fetchData = async () => {
  loading.value = true
  try {
    const [serviceRes, projectRes, serverRes] = await Promise.all([
      request.get('/service/list'),
      request.get('/project/list'),
      request.get('/server/list')
    ])
    tableData.value = serviceRes
    projects.value = projectRes
    servers.value = serverRes
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  Object.assign(form, {
    id: null,
    projectId: null,
    serverId: null,
    env: '测试',
    port: 8080,
    gitBranch: 'master',
    deployPath: '',
    logPath: '',
    mavenCmd: 'mvn clean package -DskipTests',
    startScript: './start.sh',
    monitorUrl: '',
    status: 0,
    enabled: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.id) {
      await request.put('/service', form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/service', form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该服务?', '警告', {
    type: 'warning'
  }).then(async () => {
    await request.delete(`/service/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  })
}

const handleToggleEnabled = async (row) => {
  try {
    await request.put('/service', row)
    ElMessage.success('状态更新成功')
  } catch (e) {
    ElMessage.error('状态更新失败')
    // 恢复原状态
    row.enabled = row.enabled === 1 ? 0 : 1
  }
}

const handleAction = (row, action) => {
  const actionNames = {
    'compile-restart': '编译并重启',
    'restart': '重启',
    'start': '启动'
  }
  const actionName = actionNames[action] || action
  
  ElMessageBox.confirm(`确认要执行【${actionName}】操作吗?`, '提示', {
    confirmButtonText: '确认执行',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // Open Result Dialog immediately
    resultContent.value = `正在连接 WebSocket 执行 ${actionName}...\n`
    resultStatus.value = 'success'
    resultVisible.value = true
    
    // Connect WebSocket for Execution
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.hostname
    const port = '8080' // Backend port
    const wsUrl = `${protocol}//${host}:${port}/ws/exec/${row.id}/${action}`
    
    let socket = null
    try {
      socket = new WebSocket(wsUrl)
      
      socket.onopen = () => {
        resultContent.value += '>>> 连接成功，开始执行...\n'
      }
      
      socket.onmessage = (event) => {
        resultContent.value += event.data + '\n'
        // Auto scroll
        setTimeout(() => {
          const els = document.querySelectorAll('.log-content')
          // The second one is result dialog content (usually)
          // Safer to find by parent dialog
          if (els.length > 0) {
             els.forEach(el => el.scrollTop = el.scrollHeight)
          }
        }, 0)
      }
      
      socket.onerror = (error) => {
        console.error('WebSocket Error:', error)
        resultContent.value += '\n>>> 连接发生错误'
        resultStatus.value = 'error'
      }
      
      socket.onclose = () => {
        resultContent.value += '\n>>> 执行结束 (连接已断开)'
        // Refresh status after execution
        fetchData()
      }
      
      // Store socket instance
      execSocket.value = socket
      
    } catch (e) {
      console.error(e)
      resultContent.value += '\n无法建立连接: ' + e.message
      resultStatus.value = 'error'
    }

  }).catch(() => {})
}

// Add a ref to hold exec socket
const execSocket = ref(null)

// Watch result dialog visible change to close socket
watch(resultVisible, (val) => {
  if (!val && execSocket.value) {
    execSocket.value.close()
    execSocket.value = null
  }
})

const handleLog = (row) => {
  logContent.value = '正在连接 WebSocket...'
  logVisible.value = true
  
  // Use current host for WebSocket connection
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.hostname
  const port = '8080' // Backend port
  const wsUrl = `${protocol}//${host}:${port}/ws/log/${row.id}`
  
  let socket = null
  try {
    socket = new WebSocket(wsUrl)
    
    socket.onopen = () => {
      logContent.value = '>>> 连接成功，正在获取日志...\n'
    }
    
    socket.onmessage = (event) => {
      logContent.value += event.data + '\n'
      // Auto scroll to bottom
      // Need nextTick or setTimeout if dom not updated
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
    
    // Store socket instance to close it when dialog closed
    logSocket.value = socket
    
  } catch (e) {
    console.error(e)
    logContent.value = '无法建立连接: ' + e.message
  }
}

// Add a ref to hold socket
const logSocket = ref(null)

// Watch dialog visible change to close socket
import { watch } from 'vue'
watch(logVisible, (val) => {
  if (!val && logSocket.value) {
    logSocket.value.close()
    logSocket.value = null
  }
})

onMounted(fetchData)
</script>

<style scoped>
.toolbar {
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
.result-header {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
}
</style>
