<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增服务</el-button>
      <el-form :inline="true" :model="queryForm" style="margin-left: 20px;">
        <el-form-item label="项目名称">
          <el-select v-model="queryForm.projectId" placeholder="全部" style="width: 150px;">
            <el-option label="全部" value="" />
            <el-option v-for="item in projects" :key="item.id" :label="item.projectName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务线">
          <el-select v-model="queryForm.businessLine" placeholder="全部" style="width: 150px;">
            <el-option label="全部" value="" />
            <el-option v-for="item in businessLines" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="环境">
          <el-select v-model="queryForm.env" placeholder="全部" style="width: 100px;">
            <el-option label="全部" value="" />
            <el-option label="测试" value="测试" />
            <el-option label="正式" value="生产" />
          </el-select>
        </el-form-item>
        <el-form-item label="运行状态">
          <el-select v-model="queryForm.runStatus" placeholder="全部" style="width: 120px;">
            <el-option label="全部" value="" />
            <el-option label="未启动" value="0" />
            <el-option label="正常" value="1" />
            <el-option label="异常" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
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
      <el-table-column prop="runStatus" label="运行状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.runStatus)">
            {{ getStatusText(scope.row.runStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用状态" width="120">
        <template #default="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="handleToggleEnabled(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="400">
        <template #default="scope">
          <el-button-group>
            <el-button size="small" type="primary" @click="handleAction(scope.row, 'deploy')">发版</el-button>
            <el-button size="small" type="warning" @click="handleAction(scope.row, 'restart')">重启</el-button>
            <el-button size="small" type="success" @click="handleAction(scope.row, 'start')">启动</el-button>
            <el-button size="small" type="info" @click="handleLog(scope.row)">日志</el-button>
            <el-button size="small" type="danger" @click="handleRollback(scope.row)">版本回退</el-button>
            <el-button size="small" type="default" @click="handleTerminal(scope.row)">终端</el-button>
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
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
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

    <!-- Terminal Dialog -->
    <el-dialog v-model="terminalVisible" title="服务终端" width="90%" height="80vh" :close-on-click-modal="false">
      <div class="terminal-container">
        <!-- Left Side: AI Command Generator -->
        <div class="terminal-left">
          <!-- Chat Window -->
          <div class="chat-window">
            <div class="chat-header">
              <el-tag type="info">AI命令生成器</el-tag>
            </div>
            <div class="chat-content" ref="chatContent">
              <div v-for="(msg, index) in chatMessages" :key="index" :class="['chat-message', msg.type]">
                <div class="message-sender">{{ msg.sender }}:</div>
                <div class="message-content">{{ msg.content }}</div>
              </div>
            </div>
            <div class="chat-input">
              <el-input
                v-model="chatInput"
                placeholder="描述你要执行的操作，例如：查看进程状态"
                @keyup.enter="sendChatMessage"
                clearable
              />
              <el-button type="primary" @click="sendChatMessage">发送</el-button>
            </div>
          </div>
          
          <!-- Command List -->
          <div class="command-list">
            <div class="list-header">
              <el-tag type="warning">推荐命令</el-tag>
            </div>
            <div class="list-content">
              <el-empty v-if="recommendedCommands.length === 0" description="暂无推荐命令" />
              <div v-else v-for="(cmd, index) in recommendedCommands" :key="index" class="command-item">
                <el-input
                  :value="cmd"
                  size="small"
                  readonly
                  :class="{ 'danger-command': !isSafeCommand(cmd) }"
                />
                <el-button 
                  v-if="isSafeCommand(cmd)"
                  size="small" 
                  type="primary" 
                  @click="sendCommandToTerminal(cmd)"
                >
                  执行
                </el-button>
                <span v-else class="danger-warning">危险命令谨慎操作</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Right Side: Terminal -->
        <div class="terminal-right">
          <div class="terminal-header">
            <el-tag type="success">SSH终端</el-tag>
          </div>
          <div class="terminal-content" ref="terminalRef"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import 'xterm/css/xterm.css'
import { Terminal } from 'xterm'
import { AttachAddon } from 'xterm-addon-attach'
import { FitAddon } from 'xterm-addon-fit'
import { SearchAddon } from 'xterm-addon-search'

const loading = ref(false)
const tableData = ref([])
const projects = ref([])
const servers = ref([])
const businessLines = ref([])

const queryForm = reactive({
  projectId: '',
  businessLine: '',
  env: '',
  runStatus: '',
  status: ''
})

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
  runStatus: 0,
  status: 1
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
    // 构建查询参数
    const params = {}
    if (queryForm.projectId !== '') params.projectId = queryForm.projectId
    if (queryForm.businessLine) params.businessLine = queryForm.businessLine
    if (queryForm.env !== '') params.env = queryForm.env
    if (queryForm.runStatus !== '') params.runStatus = queryForm.runStatus
    if (queryForm.status !== '') params.status = queryForm.status
    
    // 并行请求数据
    const [serviceRes, projectRes, serverRes] = await Promise.all([
      request.get('/service/list', { params }),
      request.get('/project/list'),
      request.get('/server/list')
    ])
    
    // 处理服务数据的状态值，确保是数字类型，避免菜单切换时触发 el-switch 的 change 事件
    tableData.value = serviceRes.map(item => {
      let statusValue = 0
      if (item.status === 'ENABLED' || item.status === 1 || item.status === '1') {
        statusValue = 1
      } else if (item.status === 'DISABLED' || item.status === 0 || item.status === '0') {
        statusValue = 0
      }
      return {
        ...item,
        status: statusValue
      }
    })
    
    // 处理项目数据的状态值
    projects.value = projectRes.map(item => {
      let statusValue = 0
      if (item.status === 'ENABLED' || item.status === 1 || item.status === '1') {
        statusValue = 1
      } else if (item.status === 'DISABLED' || item.status === 0 || item.status === '0') {
        statusValue = 0
      }
      return {
        ...item,
        status: statusValue
      }
    })
    
    // 处理服务器数据的状态值
    servers.value = serverRes.map(item => {
      let statusValue = 0
      if (item.status === 'ENABLED' || item.status === 1 || item.status === '1') {
        statusValue = 1
      } else if (item.status === 'DISABLED' || item.status === 0 || item.status === '0') {
        statusValue = 0
      }
      return {
        ...item,
        status: statusValue
      }
    })
    
    // 提取业务线列表（去重）
    const lines = new Set()
    projects.value.forEach(project => {
      if (project.businessLine) {
        lines.add(project.businessLine)
      }
    })
    businessLines.value = Array.from(lines)
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
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleSearch = () => {
  fetchData()
}

const handleReset = () => {
  Object.assign(queryForm, {
    projectId: '',
    businessLine: '',
    env: '',
    runStatus: '',
    status: ''
  })
  fetchData()
}

const handleAction = (row, action) => {
  const actionNames = {
    'deploy': '发版',
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

// 版本回退处理函数
const handleRollback = (row) => {
  ElMessageBox.confirm('确认要执行版本回退操作吗?', '警告', {
    confirmButtonText: '确认回退',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // Open Result Dialog immediately
    resultContent.value = `正在连接 WebSocket 执行版本回退...\n`
    resultStatus.value = 'success'
    resultVisible.value = true
    
    // Connect WebSocket for Execution
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.hostname
    const port = '8080' // Backend port
    const wsUrl = `${protocol}//${host}:${port}/ws/exec/${row.id}/rollback`
    
    let socket = null
    try {
      socket = new WebSocket(wsUrl)
      
      socket.onopen = () => {
        resultContent.value += '>>> 连接成功，开始执行版本回退...\n'
      }
      
      socket.onmessage = (event) => {
        resultContent.value += event.data + '\n'
        // Auto scroll
        setTimeout(() => {
          const els = document.querySelectorAll('.log-content')
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
        resultContent.value += '\n>>> 版本回退执行结束 (连接已断开)'
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

// Terminal related data
const terminalVisible = ref(false)
const terminalOutput = ref('')
const terminalInput = ref('')
const terminalSocket = ref(null)
const terminalRef = ref(null)
const terminal = ref(null)
const fitAddon = ref(null)
const attachAddon = ref(null)

// Chat related data
const chatMessages = ref([])
const chatInput = ref('')
const recommendedCommands = ref([])

// Terminal handler
const handleTerminal = (row) => {
  // Reset terminal data
  chatMessages.value = []
  recommendedCommands.value = []
  chatInput.value = ''
  
  terminalVisible.value = true
  
  // Wait for DOM to update
  nextTick(() => {
    // Initialize terminal
    initTerminal()
    
    // Connect to terminal WebSocket
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.hostname
    const port = '8080' // Backend port
    const wsUrl = `${protocol}//${host}:${port}/ws/terminal/${row.id}`
    
    try {
      terminalSocket.value = new WebSocket(wsUrl)
      
      terminalSocket.value.onopen = () => {
        // Attach WebSocket to terminal
        attachAddon.value = new AttachAddon(terminalSocket.value)
        terminal.value.loadAddon(attachAddon.value)
        terminal.value.write('>>> 终端连接成功\r\n')
      }
      
      terminalSocket.value.onerror = (error) => {
        console.error('Terminal WebSocket Error:', error)
        terminal.value.write('\r\n>>> 终端连接发生错误\r\n')
      }
      
      terminalSocket.value.onclose = () => {
        terminal.value.write('\r\n>>> 终端连接已断开\r\n')
      }
      
    } catch (e) {
      console.error(e)
      terminal.value.write('\r\n无法建立终端连接: ' + e.message + '\r\n')
    }
  })
}

// Initialize terminal
const initTerminal = () => {
  // Clean up existing terminal
  if (terminal.value) {
    terminal.value.dispose()
  }
  
  // Create new terminal
  terminal.value = new Terminal({
    cursorBlink: true,
    cursorStyle: 'block',
    scrollback: 1000,
    fontSize: 14,
    fontFamily: 'Consolas, Monaco, "Courier New", monospace',
    theme: {
      foreground: '#f8f8f2',
      background: '#282a36',
      cursor: '#f8f8f2',
      cursorAccent: '#282a36',
      selection: 'rgba(255, 255, 255, 0.1)',
      ansi: {
        black: '#21222c',
        red: '#ff5555',
        green: '#50fa7b',
        yellow: '#f1fa8c',
        blue: '#bd93f9',
        magenta: '#ff79c6',
        cyan: '#8be9fd',
        white: '#f8f8f2',
        brightBlack: '#6272a4',
        brightRed: '#ff6e6e',
        brightGreen: '#69ff94',
        brightYellow: '#ffffa5',
        brightBlue: '#d6acff',
        brightMagenta: '#ff92df',
        brightCyan: '#a4ffff',
        brightWhite: '#ffffff'
      }
    }
  })
  
  // Create and load fit addon
  fitAddon.value = new FitAddon()
  terminal.value.loadAddon(fitAddon.value)
  
  // Create and load search addon
  const searchAddon = new SearchAddon()
  terminal.value.loadAddon(searchAddon)
  
  // Attach terminal to DOM
  terminal.value.open(terminalRef.value)
  
  // Fit terminal to container
  fitAddon.value.fit()
  
  // Add resize listener
  window.addEventListener('resize', () => {
    if (fitAddon.value && terminal.value) {
      fitAddon.value.fit()
    }
  })
  
  // Add tab completion support
  terminal.value.onKey((e) => {
    const printable = !e.domEvent.altKey && !e.domEvent.ctrlKey && !e.domEvent.metaKey
    
    if (e.key === 'Tab') {
      // Handle tab completion
      e.domEvent.preventDefault()
      terminal.value.write('  ')
    } else if (printable) {
      // Pass through printable characters
      if (attachAddon.value) {
        // WebSocket is connected, let the attach addon handle it
      }
    }
  })
}

// Send chat message to generate commands
const sendChatMessage = async () => {
  if (!chatInput.value.trim()) return
  
  // Clear history chat messages before sending new one
  chatMessages.value = []
  
  // Add user message to chat
  chatMessages.value.push({
    type: 'user',
    sender: '您',
    content: chatInput.value
  })
  
  // Scroll to bottom
  setTimeout(() => {
    const chatContent = document.querySelector('.chat-content')
    if (chatContent) {
      chatContent.scrollTop = chatContent.scrollHeight
    }
  }, 0)
  
  // Generate commands
  try {
    const response = await request.post('/terminal/generate-command', chatInput.value)
    recommendedCommands.value = response
    
    // Add AI response to chat
    chatMessages.value.push({
      type: 'ai',
      sender: 'AI',
      content: '已为您生成以下命令：\n' + recommendedCommands.value.map(cmd => `- ${cmd}`).join('\n')
    })
    
    // Scroll to bottom
    setTimeout(() => {
      const chatContent = document.querySelector('.chat-content')
      if (chatContent) {
        chatContent.scrollTop = chatContent.scrollHeight
      }
    }, 0)
  } catch (e) {
    console.error(e)
    chatMessages.value.push({
      type: 'ai',
      sender: 'AI',
      content: '生成命令失败，请重试'
    })
  }
  
  chatInput.value = ''
}

// 安全命令白名单（只读/无害命令）
const SAFE_COMMANDS = /^(ls|pwd|whoami|date|echo|cat|head|tail|grep|ps|df|du|free|top|uname|hostname|id|which|man)$/

function isSafeCommand(command) {
  const simpleCmd = command.trim().split(/\s+/)[0]
  return SAFE_COMMANDS.test(simpleCmd)
}

// Send command to terminal
const sendCommandToTerminal = (command) => {
  if (!terminalSocket.value || terminalSocket.value.readyState !== WebSocket.OPEN) {
    terminal.value.write('>>> 终端未连接\r\n')
    return
  }
  
  terminalSocket.value.send(command + '\r')
}

// Watch terminal dialog visible change to close socket
watch(terminalVisible, (val) => {
  if (!val) {
    // Close WebSocket
    if (terminalSocket.value) {
      terminalSocket.value.close()
      terminalSocket.value = null
    }
    
    // Clean up terminal
    if (terminal.value) {
      terminal.value.dispose()
      terminal.value = null
    }
    
    // Remove resize listener
    window.removeEventListener('resize', () => {
      if (fitAddon.value && terminal.value) {
        fitAddon.value.fit()
      }
    })
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

/* Terminal Styles */
.terminal-container {
  display: flex;
  height: 60vh;
  gap: 20px;
}

.terminal-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.terminal-right {
  flex: 2;
  display: flex;
  flex-direction: column;
}

/* Chat Window */
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.chat-header {
  padding: 10px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.chat-content {
  flex: 1;
  padding: 10px;
  overflow-y: auto;
  background-color: #fff;
}

.chat-message {
  margin-bottom: 10px;
  padding: 8px;
  border-radius: 4px;
}

.chat-message.user {
  background-color: #ecf5ff;
  align-self: flex-end;
}

.chat-message.ai {
  background-color: #f0f9eb;
  align-self: flex-start;
}

.message-sender {
  font-weight: bold;
  margin-bottom: 4px;
}

.message-content {
  font-size: 14px;
}

.chat-input {
  padding: 10px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  gap: 10px;
}

.chat-input .el-input {
  flex: 1;
}

/* Command List */
.command-list {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.list-header {
  padding: 10px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.list-content {
  padding: 10px;
  overflow-y: auto;
  background-color: #fff;
}

.command-item {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  align-items: center;
}

.command-item .el-input {
  flex: 1;
}

.danger-command :deep(.el-input__wrapper) {
  border-color: #f56c6c;
  background-color: #fef0f0;
}

.danger-warning {
  color: #f56c6c;
  font-size: 12px;
  font-weight: bold;
}

/* Terminal */
.terminal-header {
  padding: 10px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  border-radius: 4px 4px 0 0;
}

.terminal-content {
  flex: 1;
  overflow: hidden;
  border-radius: 0 0 4px 4px;
}

/* XTerm.js specific styles */
:deep(.xterm) {
  width: 100%;
  height: 100%;
}

:deep(.xterm-viewport) {
  background-color: #282a36;
}

:deep(.xterm-cursor) {
  background-color: #f8f8f2;
}

:deep(.xterm-selection) {
  background-color: rgba(255, 255, 255, 0.1);
}

/* Responsive Adjustments */
@media (max-width: 1200px) {
  .terminal-container {
    flex-direction: column;
  }
  
  .terminal-left,
  .terminal-right {
    flex: none;
    height: 40vh;
  }
}
</style>
