<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增服务器</el-button>
      <el-form :inline="true" :model="queryForm" style="margin-left: 20px;">
        <el-form-item label="IP">
          <el-input v-model="queryForm.ip" placeholder="请输入 IP 地址" style="width: 150px;" />
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model="queryForm.groupName" placeholder="请输入分组" style="width: 150px;" />
        </el-form-item>
        <el-form-item label="环境">
          <el-select v-model="queryForm.env" placeholder="全部" style="width: 100px;">
            <el-option label="全部" value="" />
            <el-option label="测试" value="test" />
            <el-option label="正式" value="prod" />
          </el-select>
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
      <el-table-column prop="alias" label="别名" />
      <el-table-column prop="ip" label="IP 地址" />
      <el-table-column prop="groupName" label="分组" />
      <el-table-column prop="env" label="环境" />
      <el-table-column prop="deployedCount" label="部署数" width="80" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="handleToggleStatus(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="380">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="success" @click="handleTerminal(scope.row)">终端</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          <el-button size="small" type="warning" @click="handleDeployKey(scope.row)" v-if="scope.row.password">
            生成密钥
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑服务器' : '新增服务器'" width="800px">
      <el-form :model="form" label-width="140px">
        <el-form-item label="别名">
          <el-input v-model="form.alias" />
        </el-form-item>
        <el-form-item label="IP 地址">
          <el-input v-model="form.ip" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>

        <!-- 认证方式选择 -->
        <el-form-item label="认证方式">
          <el-radio-group v-model="form.authType">
            <el-radio label="password">密码认证</el-radio>
            <el-radio label="key">密钥认证</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 密码（条件显示） -->
        <el-form-item label="密码" v-if="form.authType === 'password'">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入服务器密码" />
        </el-form-item>

        <!-- 私钥（条件显示） -->
        <el-form-item label="私钥" v-if="form.authType === 'key'">
          <el-input
            v-model="form.privateKey"
            type="textarea"
            :rows="10"
            placeholder="-----BEGIN PRIVATE KEY-----&#10;...&#10;-----END PRIVATE KEY-----"
          />
        </el-form-item>

        <!-- 私钥密码（可选） -->
        <el-form-item label="私钥密码" v-if="form.authType === 'key'">
          <el-input v-model="form.privateKeyPassphrase" type="password" show-password placeholder="（可选）私钥密码" />
        </el-form-item>

        <!-- 生成并部署密钥按钮 -->
        <el-form-item v-if="form.authType === 'key' && form.id" label=" ">
          <el-button type="warning" @click="handleDeployKey(form)" :loading="deployingKey" :disabled="!form.password">
            生成并部署密钥对
          </el-button>
          <el-button @click="handleTestKeyAuth(form)" v-if="form.privateKey">测试密钥连接</el-button>
          <div style="color: #909399; font-size: 12px; margin-top: 4px;" v-if="form.password">
            点击"生成并部署密钥对"将自动使用当前密码连接服务器并部署公钥
          </div>
          <div style="color: #f56c6c; font-size: 12px; margin-top: 4px;" v-else>
            请先在下方填写服务器密码，然后保存后再部署密钥
          </div>
        </el-form-item>

        <el-form-item label="分组">
          <el-input v-model="form.groupName" />
        </el-form-item>
        <el-form-item label="环境">
          <el-select v-model="form.env">
            <el-option label="生产" value="prod" />
            <el-option label="测试" value="test" />
          </el-select>
        </el-form-item>
        <el-form-item label="JAVA_HOME">
          <el-input v-model="form.javaHome" placeholder="例如：/usr/lib/jvm/java-17-openjdk（可选）" />
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            该服务器上的 Java 安装路径，不填则使用系统默认配置或自动检测
          </div>
        </el-form-item>
        <el-form-item label="MAVEN_HOME">
          <el-input v-model="form.mavenHome" placeholder="例如：/usr/share/maven（可选）" />
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            该服务器上的 Maven 安装路径，不填则使用系统默认配置
          </div>
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

    <!-- 密钥部署结果对话框 -->
    <el-dialog v-model="keyDeployDialogVisible" title="密钥部署结果" width="600px">
      <div v-if="deployResult.success" style="color: green;">
        <el-icon><check /></el-icon>
        <span>密钥生成并部署成功！</span>
      </div>
      <div v-else style="color: red;">
        <el-icon><close /></el-icon>
        <span>{{ deployResult.error }}</span>
      </div>
      <el-input
        v-if="deployResult.publicKey"
        type="textarea"
        :rows="4"
        v-model="deployResult.publicKey"
        readonly
        style="margin-top: 10px;"
      />
      <div v-if="deployResult.publicKey" style="margin-top: 10px; color: #909399; font-size: 12px;">
        这是已部署到服务器的公钥，请妥善保管。私钥已加密保存到数据库。
      </div>
      <template #footer>
        <el-button type="primary" @click="keyDeployDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Terminal Dialog -->
    <el-dialog v-model="terminalVisible" title="服务器终端" width="90%" height="80vh" :close-on-click-modal="false">
      <div class="terminal-container">
        <!-- Left Side: Command Chat -->
        <div class="terminal-left">
          <div class="chat-messages" ref="chatMessagesRef">
            <div v-for="(msg, index) in chatMessages" :key="index" :class="['chat-message', msg.type]">
              <strong>{{ msg.sender }}:</strong>
              <span>{{ msg.content }}</span>
            </div>
          </div>
          <div class="chat-input-container">
            <el-input
              v-model="chatInput"
              placeholder="描述您想执行的操作，例如：查看日志、重启服务..."
              @keyup.enter="sendChatMessage"
              clearable
            >
              <template #append>
                <el-button @click="sendChatMessage">发送</el-button>
              </template>
            </el-input>
          </div>
          <!-- Recommended Commands -->
          <div class="recommended-commands" v-if="recommendedCommands.length > 0">
            <div class="commands-title">推荐命令：</div>
            <el-button
              v-for="(cmd, index) in recommendedCommands"
              :key="index"
              size="small"
              @click="sendCommandToTerminal(cmd)"
              :type="isSafeCommand(cmd) ? 'success' : 'danger'"
              style="margin: 5px;"
            >
              {{ cmd }}
            </el-button>
          </div>
        </div>
        <!-- Right Side: Terminal -->
        <div class="terminal-right">
          <div class="terminal-header">
            <el-button size="small" @click="clearTerminal">清屏</el-button>
            <el-tag type="info">SSH 连接：{{ currentServerIp }}</el-tag>
          </div>
          <div class="terminal-content" ref="terminalRef"></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="terminalVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch, onUnmounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import { AttachAddon } from 'xterm-addon-attach'
import 'xterm/css/xterm.css'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const deployingKey = ref(false)
const keyDeployDialogVisible = ref(false)
const deployResult = reactive({
  success: false,
  error: '',
  publicKey: ''
})

const form = reactive({
  id: null,
  alias: '',
  ip: '',
  username: '',
  password: '',
  privateKey: '',
  privateKeyPassphrase: '',
  authType: 'password',
  groupName: '',
  env: 'test',
  javaHome: '',
  mavenHome: '',
  status: 1
})

const queryForm = reactive({
  ip: '',
  groupName: '',
  env: '',
  status: ''
})

// Terminal related
const terminalVisible = ref(false)
const terminalSocket = ref(null)
const terminalRef = ref(null)
const terminal = ref(null)
const fitAddon = ref(null)
const attachAddon = ref(null)
const chatMessages = ref([])
const chatInput = ref('')
const recommendedCommands = ref([])
const currentServerIp = ref('')
const currentServerId = ref(null)

const fetchData = async () => {
  loading.value = true
  try {
    const params = {}
    if (queryForm.ip) params.ip = queryForm.ip
    if (queryForm.groupName) params.groupName = queryForm.groupName
    if (queryForm.env !== '') params.env = queryForm.env
    if (queryForm.status !== '') params.status = queryForm.status

    const res = await request.get('/server/list', { params })
    // 处理状态值，确保是数字类型，避免菜单切换时触发 el-switch 的 change 事件
    tableData.value = res.map(item => {
      let statusValue = 0
      if (item.status === 'ENABLED' || item.status === 1 || item.status === '1') {
        statusValue = 1
      } else if (item.status === 'DISABLED' || item.status === 0 || item.status === '0') {
        statusValue = 0
      }
      // 根据是否有私钥判断认证方式
      item.authType = item.privateKey ? 'key' : 'password'
      return {
        ...item,
        status: statusValue
      }
    })
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  Object.assign(form, {
    id: null,
    alias: '',
    ip: '',
    username: '',
    password: '',
    privateKey: '',
    privateKeyPassphrase: '',
    authType: 'password',
    groupName: '',
    env: 'test',
    javaHome: '',
    mavenHome: '',
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, {
    ...row,
    authType: row.privateKey ? 'key' : 'password'
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.id) {
      await request.put('/server', form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/server', form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该服务器？', '警告', {
    type: 'warning'
  }).then(async () => {
    await request.delete(`/server/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  })
}

const handleToggleStatus = async (row) => {
  try {
    await request.put('/server', row)
    ElMessage.success('状态更新成功')
  } catch (e) {
    ElMessage.error('状态更新失败')
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

/**
 * 生成并部署密钥对
 */
const handleDeployKey = async (row) => {
  ElMessageBox.confirm(
    `将为服务器 ${row.ip} 生成并部署 SSH 密钥对。\n\n系统将：\n1. 生成 ED25519 密钥对\n2. 使用密码登录服务器\n3. 自动部署公钥到 ~/.ssh/authorized_keys\n4. 保存私钥到数据库\n\n确认继续？`,
    '部署密钥',
    {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }
  )

  deployingKey.value = true

  try {
    const res = await request.post(`/server/generate-and-deploy-key/${row.id}`)

    if (res.success) {
      deployResult.success = true
      deployResult.error = ''
      deployResult.publicKey = res.publicKey

      ElMessage.success('密钥生成并部署成功！')

      // 刷新数据
      fetchData()
    } else {
      deployResult.success = false
      deployResult.error = res.error || '未知错误'
      deployResult.publicKey = ''
      ElMessage.error('部署失败：' + res.error)
    }

    keyDeployDialogVisible.value = true
  } catch (e) {
    ElMessage.error('部署失败：' + (e.response?.data?.error || e.message))
    deployResult.success = false
    deployResult.error = e.response?.data?.error || e.message
    keyDeployDialogVisible.value = true
  } finally {
    deployingKey.value = false
  }
}

/**
 * 测试密钥连接
 */
const handleTestKeyAuth = async (row) => {
  try {
    const res = await request.post(`/server/test-key-auth/${row.id}`)

    if (res.success) {
      ElMessage.success('密钥认证成功！')
    } else {
      ElMessage.error('密钥认证失败：' + res.error)
    }
  } catch (e) {
    ElMessage.error('测试失败：' + (e.response?.data?.error || e.message))
  }
}

const handleSearch = () => {
  fetchData()
}

const handleReset = () => {
  Object.assign(queryForm, {
    ip: '',
    groupName: '',
    env: '',
    status: ''
  })
  fetchData()
}

// Terminal related functions
const handleTerminal = (row) => {
  // 重置所有状态
  chatMessages.value = []
  recommendedCommands.value = []
  chatInput.value = ''
  currentServerIp.value = row.ip
  currentServerId.value = row.id

  // 清理旧的 WebSocket 连接
  if (terminalSocket.value) {
    try {
      terminalSocket.value.close()
    } catch (e) {
      console.warn('关闭旧 WebSocket 连接失败:', e)
    }
    terminalSocket.value = null
  }

  terminalVisible.value = true

  nextTick(() => {
    // 先清理旧的 terminal 实例
    if (terminal.value) {
      try {
        terminal.value.dispose()
      } catch (e) {
        console.warn('清理旧 terminal 实例失败:', e)
      }
      terminal.value = null
    }

    // 清理 addon 引用
    fitAddon.value = null
    attachAddon.value = null

    // 初始化新的 terminal
    initTerminal()

    // 创建 WebSocket 连接
    const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/ws/server-terminal/${row.id}`

    try {
      terminalSocket.value = new WebSocket(wsUrl)

      terminalSocket.value.onopen = () => {
        attachAddon.value = new AttachAddon(terminalSocket.value)
        terminal.value.loadAddon(attachAddon.value)
        terminal.value.write('>>> 服务器终端连接成功：' + row.ip + '\\r\\n')
        terminal.value.write('>>> 已建立 SSH 连接，可以执行命令\\r\\n\\r\\n')
      }

      terminalSocket.value.onerror = (error) => {
        console.error('WebSocket 连接错误:', error)
        terminal.value.write('\\r\\n>>> 连接失败，请检查后端服务是否启动\\r\\n')
      }

      terminalSocket.value.onclose = (event) => {
        console.log('WebSocket 连接关闭:', event.code, event.reason)
        if (!event.wasClean) {
          terminal.value.write('\\r\\n>>> 连接异常关闭\\r\\n')
        }
      }
    } catch (e) {
      console.error('创建 WebSocket 失败:', e)
      terminal.value.write('\\r\\n>>> 创建连接失败，请检查网络或后端服务\\r\\n')
    }
  })
}

const initTerminal = () => {
  // 检查 DOM 元素是否存在
  if (!terminalRef.value) {
    console.error('Terminal 容器元素不存在')
    return
  }

  // 安全地清理旧的 terminal 实例（如果存在）
  if (terminal.value) {
    try {
      terminal.value.dispose()
    } catch (e) {
      console.warn('Terminal dispose error:', e)
    }
    terminal.value = null
  }

  try {
    // 创建新的 terminal 实例
    terminal.value = new Terminal({
      cursorBlink: true,
      fontSize: 14,
      theme: { background: '#282a36', foreground: '#f8f8f2' }
    })

    fitAddon.value = new FitAddon()
    terminal.value.loadAddon(fitAddon.value)
    terminal.value.open(terminalRef.value)
    fitAddon.value.fit()
  } catch (e) {
    console.error('初始化 terminal 失败:', e)
  }
}

const sendChatMessage = async () => {
  if (!chatInput.value.trim()) return
  chatMessages.value = [{ type: 'user', sender: '您', content: chatInput.value }]
  try {
    const response = await request.post('/terminal/generate-command', chatInput.value)
    recommendedCommands.value = response
    chatMessages.value.push({ type: 'ai', sender: 'AI', content: '推荐命令已生成' })
  } catch (e) {
    chatMessages.value.push({ type: 'ai', sender: 'AI', content: '生成命令失败' })
  }
  chatInput.value = ''
}

const isSafeCommand = (cmd) => !/rm\s+-rf|mkfs|shutdown|reboot/i.test(cmd)

const sendCommandToTerminal = (cmd) => {
  if (terminalSocket.value) {
    terminalSocket.value.send(cmd + '\\r')
  }
}

const clearTerminal = () => {
  if (terminal.value) {
    terminal.value.clear()
  }
}

watch(terminalVisible, (val) => {
  if (!val) {
    // 清理 WebSocket 连接
    if (terminalSocket.value) {
      try {
        terminalSocket.value.close()
      } catch (e) {
        console.warn('关闭 WebSocket 失败:', e)
      }
      terminalSocket.value = null
    }

    // 清理 terminal 实例
    if (terminal.value) {
      try {
        terminal.value.dispose()
      } catch (e) {
        console.warn('销毁 terminal 实例失败:', e)
      }
      terminal.value = null
    }

    // 清理 addon 引用
    fitAddon.value = null
    attachAddon.value = null
  }
})

// 组件卸载时清理终端资源
onUnmounted(() => {
  if (terminalSocket.value) {
    terminalSocket.value.close()
    terminalSocket.value = null
  }
  if (terminal.value) {
    try {
      terminal.value.dispose()
    } catch (e) {
      console.warn('Terminal dispose error on unmount:', e)
    }
    terminal.value = null
  }
  fitAddon.value = null
  attachAddon.value = null
})

onMounted(fetchData)
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
}

.terminal-container {
  display: flex;
  height: 70vh;
  gap: 10px;
}

.terminal-left {
  width: 300px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-right: 1px solid #e0e0e0;
  padding-right: 10px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 10px;
  background: #f5f5f5;
}

.chat-message {
  margin-bottom: 10px;
  padding: 8px;
  border-radius: 4px;
}

.chat-message.user {
  background: #e3f2fd;
}

.chat-message.ai {
  background: #e8f5e9;
}

.chat-input-container {
  margin-top: 10px;
}

.recommended-commands {
  margin-top: 10px;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fafafa;
}

.commands-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #666;
}

.terminal-right {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.terminal-header {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
  padding: 8px;
  background: #f5f5f5;
  border-radius: 4px;
}

.terminal-content {
  flex: 1;
  border: 1px solid #333;
  border-radius: 4px;
  overflow: hidden;
}
</style>
