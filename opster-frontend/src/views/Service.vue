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
      <el-table-column prop="repoGitUrl" label="仓库" show-overflow-tooltip />
      <el-table-column label="服务器" width="150">
        <template #default="scope">
          {{ getServerName(scope.row.serverId) }}
        </template>
      </el-table-column>
      <el-table-column prop="env" label="环境" width="80" />
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="gitBranch" label="分支" width="100" />
      <el-table-column prop="deployPath" label="部署路径" show-overflow-tooltip />
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
            <el-dropdown @command="(cmd) => handleRollbackCommand(cmd, scope.row)">
              <el-button size="small" type="danger">
                版本回退<el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="quick">快速回退（上一版本）</el-dropdown-item>
                  <el-dropdown-item command="history">选择历史版本</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" type="default" @click="handleTerminal(scope.row)">终端</el-button>
          </el-button-group>
          <el-divider direction="vertical" />
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑服务' : '新增服务'" width="1200px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="选择项目">
          <el-select v-model="form.projectId" placeholder="请选择项目" @change="handleProjectChange" :disabled="!!form.id">
            <el-option v-for="item in projects" :key="item.id" :label="item.projectName" :value="item.id" />
          </el-select>
        </el-form-item>

        <div v-if="form.projectId" class="repo-config-list">
          <div v-for="(item, index) in form.items" :key="index" class="repo-config-card">
            <div class="repo-info">
              <el-tag size="small">{{ getRepoTypeLabel(item.repoType) }}</el-tag>
              <span class="repo-url">{{ item.repoGitUrl }}</span>
            </div>
            
            <el-row :gutter="20">
              <el-col :span="5">
                <el-form-item label="服务器" label-width="70px">
                  <el-select v-model="item.serverId" placeholder="选择服务器" style="width: 100%">
                    <el-option v-for="s in servers" :key="s.id" :label="`${s.alias} (${s.ip})`" :value="s.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item label="环境" label-width="50px">
                  <el-select v-model="item.env" style="width: 100%">
                    <el-option label="生产" value="生产" />
                    <el-option label="测试" value="测试" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="3">
                <el-form-item label="端口" label-width="50px">
                  <el-input-number v-model="item.port" :min="1" :max="65535" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item label="分支" label-width="50px">
                  <el-input v-model="item.gitBranch" />
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item label="状态" label-width="50px">
                  <el-switch v-model="item.status" :active-value="1" :inactive-value="0" active-text="启用" />
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item label="仓库类型" label-width="80px">
                  <el-select v-model="item.repositoryType" style="width: 100%">
                    <el-option label="前端" :value="0" />
                    <el-option label="后端" :value="1" />
                    <el-option label="管理后台" :value="2" />
                    <el-option label="移动端" :value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="部署路径" label-width="70px">
                  <el-input v-model="item.deployPath" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="日志路径" label-width="70px">
                  <el-input v-model="item.logPath" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="项目路径" label-width="70px">
                  <el-input v-model="item.projectPath" placeholder="例如：opster-backend、opster-frontend" clearable />
                  <span style="font-size: 12px; color: #999;">
                    相对于Git仓库的子目录路径，如果项目在仓库根目录则留空
                  </span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <!-- 后端项目显示 Maven 命令 -->
                <el-form-item v-if="item.repositoryType === 1" label="Maven命令" label-width="70px">
                  <el-input v-model="item.mavenCmd" type="textarea" :rows="1" placeholder="mvn clean package -DskipTests" />
                </el-form-item>
                <!-- 前端项目显示构建命令 -->
                <el-form-item v-else label="构建命令" label-width="70px">
                  <el-input v-model="item.buildCmd" type="textarea" :rows="1" placeholder="npm run build" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="启动脚本" label-width="70px">
                  <el-input v-model="item.startScript" type="textarea" :rows="1" placeholder="./start.sh" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="监控地址" label-width="70px">
                  <el-input v-model="item.monitorUrl" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
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
        <el-space>
          <el-tag :type="resultStatus === 'success' ? 'success' : 'danger'" size="large">
            {{ resultStatus === 'success' ? '执行成功' : '执行完成' }}
          </el-tag>
          <el-tag v-if="deployProgress > 0 && deployProgress < 100" type="info">
            进度: {{ deployProgress }}%
          </el-tag>
        </el-space>
      </div>
      <div class="result-content">
        <pre class="log-content" :class="{ 'log-with-progress': deployProgress > 0 && deployProgress < 100 }">{{ resultContent }}</pre>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="copyResultContent">复制日志</el-button>
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

    <!-- 版本历史对话框 -->
    <el-dialog v-model="versionHistoryVisible" title="版本历史" width="70%" :close-on-click-modal="false">
      <el-timeline>
        <el-timeline-item
          v-for="version in versionHistory"
          :key="version.id"
          :timestamp="version.createTime"
          placement="top"
        >
          <el-card>
            <div class="version-card">
              <div class="version-header">
                <el-tag v-if="version.versionTag" type="primary">{{ version.versionTag }}</el-tag>
                <el-tag :type="getDeploymentStatusType(version.status)">
                  {{ getDeploymentStatusText(version.status) }}
                </el-tag>
                <el-tag v-if="version.backupFileSize" type="info">
                  {{ formatFileSize(version.backupFileSize) }}
                </el-tag>
              </div>
              <div class="version-info">
                <p><strong>部署时间:</strong> {{ version.createTime }}</p>
                <p v-if="version.versionDescription"><strong>版本描述:</strong> {{ version.versionDescription }}</p>
                <p v-if="version.gitCommitHash"><strong>Git提交:</strong> <code>{{ version.gitCommitHash.substring(0, 8) }}</code></p>
                <p v-if="version.backupFilePath"><strong>备份文件:</strong> <code>{{ version.backupFilePath }}</code></p>
              </div>
              <div class="version-actions">
                <el-button size="small" type="danger" @click="handleRollbackToVersion(version)">回退到此版本</el-button>
                <el-button size="small" @click="handleEditVersion(version)">编辑描述</el-button>
                <el-button size="small" type="info" @click="handleViewVersionLog(version)">查看日志</el-button>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="versionHistory.length === 0" description="暂无版本历史" />
    </el-dialog>

    <!-- 版本描述编辑对话框 -->
    <el-dialog v-model="versionEditVisible" title="编辑版本信息" width="500px">
      <el-form :model="versionEditForm" label-width="80px">
        <el-form-item label="版本标签">
          <el-input v-model="versionEditForm.tag" placeholder="如：v1.0.0, stable, canary" />
        </el-form-item>
        <el-form-item label="版本描述">
          <el-input
            v-model="versionEditForm.description"
            type="textarea"
            :rows="4"
            placeholder="描述此版本的特性、变更内容等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="versionEditVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveVersionInfo">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
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
const deployProgress = ref(0) // 新增：部署进度

// 版本历史相关
const versionHistoryVisible = ref(false)
const versionHistory = ref([])
const currentService = ref(null)
const versionEditVisible = ref(false)
const versionEditForm = reactive({
  id: null,
  description: '',
  tag: ''
})

// 仓库类型映射
const repoTypeMap = {
  0: '前端',
  1: '后端',
  2: '管理后台',
  3: '移动端'
}
const getRepoTypeLabel = (type) => repoTypeMap[type] || '未知'

const form = reactive({
  id: null,
  projectId: null,
  items: []
})

const handleProjectChange = (projectId) => {
  const project = projects.value.find(p => p.id === projectId)
  if (!project) {
    form.items = []
    return
  }

  // 解析 repositories 字段，确保它是数组对象
  let repos = project.repositories
  if (typeof repos === 'string') {
    try {
      repos = JSON.parse(repos)
    } catch (e) {
      console.error('解析仓库列表失败:', e)
      repos = []
    }
  }

  if (Array.isArray(repos)) {
    form.items = repos.map(repo => {
      // 根据仓库类型预设默认值
      const isFrontend = repo.type === 0 || repo.type === 2 || repo.type === 3
      return {
        repoGitUrl: repo.gitUrl,
        repoType: repo.type,
        repositoryType: repo.type, // 新增字段
        serverId: null,
        env: '测试',
        port: isFrontend ? 80 : 8080,
        gitBranch: 'master',
        deployPath: repo.projectPath || '/var/www/' + (project.projectName || 'app'),
        logPath: isFrontend ? '' : '/var/log/' + (project.projectName || 'app') + '.log',
        projectPath: repo.projectPath || '', // 新增字段，项目路径
        mavenCmd: isFrontend ? '' : 'mvn clean package -DskipTests',
        buildCmd: isFrontend ? 'npm install && npm run build' : '', // 新增字段
        startScript: isFrontend ? '' : './start.sh',
        monitorUrl: '',
        status: 1
      }
    })
  } else {
    form.items = []
  }
}

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
    const params = {}
    if (queryForm.projectId !== '') params.projectId = queryForm.projectId
    if (queryForm.businessLine) params.businessLine = queryForm.businessLine
    if (queryForm.env !== '') params.env = queryForm.env
    if (queryForm.runStatus !== '') params.runStatus = queryForm.runStatus
    if (queryForm.status !== '') params.status = queryForm.status
    
    const [serviceRes, projectRes, serverRes] = await Promise.all([
      request.get('/service/list', { params }),
      request.get('/project/list'),
      request.get('/server/list')
    ])
    
    tableData.value = serviceRes.map(item => ({
      ...item,
      status: (item.status === 'ENABLED' || item.status === 1 || item.status === '1') ? 1 : 0
    }))
    
    projects.value = projectRes.map(item => ({
      ...item,
      status: (item.status === 'ENABLED' || item.status === 1 || item.status === '1') ? 1 : 0
    }))
    
    servers.value = serverRes.map(item => ({
      ...item,
      status: (item.status === 'ENABLED' || item.status === 1 || item.status === '1') ? 1 : 0
    }))
    
    const lines = new Set()
    projects.value.forEach(p => p.businessLine && lines.add(p.businessLine))
    businessLines.value = Array.from(lines)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  form.id = null
  form.projectId = null
  form.items = []
  dialogVisible.value = true
}

const handleEdit = (row) => {
  form.id = row.id
  form.projectId = row.projectId
  // 编辑模式只编辑当前选中的一个服务
  form.items = [{
    id: row.id,
    repoGitUrl: row.repoGitUrl,
    repositoryType: row.repositoryType ?? 1, // 新增字段，默认为后端
    serverId: row.serverId,
    env: row.env,
    port: row.port,
    gitBranch: row.gitBranch,
    deployPath: row.deployPath,
    logPath: row.logPath,
    projectPath: row.projectPath || '', // 新增字段，项目路径
    mavenCmd: row.mavenCmd,
    buildCmd: row.buildCmd, // 新增字段
    startScript: row.startScript,
    monitorUrl: row.monitorUrl,
    status: row.status
  }]
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.projectId) {
    return ElMessage.warning('请选择项目')
  }
  if (form.items.length === 0) {
    return ElMessage.warning('项目暂无仓库配置')
  }

  // 校验每项是否选择了服务器
  const invalid = form.items.find(item => !item.serverId)
  if (invalid) {
    return ElMessage.warning(`仓库 ${invalid.repoGitUrl} 未选择服务器`)
  }

  try {
    if (form.id) {
      // 编辑
      const data = { ...form.items[0], projectId: form.projectId }
      await request.put('/service', data)
      ElMessage.success('更新成功')
    } else {
      // 批量新增
      const services = form.items.map(item => ({ ...item, projectId: form.projectId }))
      await request.post('/service/batch', services)
      ElMessage.success('批量创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该服务?', '警告', { type: 'warning' }).then(async () => {
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
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleSearch = () => fetchData()
const handleReset = () => {
  Object.assign(queryForm, { projectId: '', businessLine: '', env: '', runStatus: '', status: '' })
  fetchData()
}

// Socket instances
const execSocket = ref(null)
const logSocket = ref(null)

const handleAction = (row, action) => {
  const actionNames = { 'deploy': '发版', 'restart': '重启', 'start': '启动' }
  const actionName = actionNames[action] || action

  // 发版使用异步 API（不打开 WebSocket 窗口）
  if (action === 'deploy') {
    ElMessageBox.confirm(`确认执行【${actionName}】?`, '提示', { type: 'warning' }).then(() => {
      request.post(`/service/${row.id}/deploy-async`).then(res => {
        if (res.success) {
          ElMessage.success(res.message || '正在发版，详细信息去发版记录查看')
          fetchData()
        } else {
          ElMessage.error(res.message || '发版失败')
        }
      }).catch(err => {
        ElMessage.error('发版请求失败: ' + (err.message || '未知错误'))
      })
    }).catch(() => {})
    return
  }

  // 其他操作（重启、启动）继续使用 WebSocket
  ElMessageBox.confirm(`确认执行【${actionName}】?`, '提示', { type: 'warning' }).then(() => {
    resultContent.value = `正在连接 WebSocket 执行 ${actionName}...\n`
    resultStatus.value = 'success'
    deployProgress.value = 0 // 重置进度
    resultVisible.value = true

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.hostname
    const port = '8080'
    const wsUrl = `${protocol}//${host}:${port}/ws/exec/${row.id}/${action}`

    try {
      const socket = new WebSocket(wsUrl)
      socket.onopen = () => {
        resultContent.value += '>>> 连接成功，开始执行...\n'
      }
      socket.onmessage = (e) => {
        const message = e.data

        // 解析进度信息
        if (message.includes('上传进度:')) {
          const match = message.match(/上传进度:\s*(\d+)%/)
          if (match) {
            deployProgress.value = parseInt(match[1])
          }
        } else if (message.includes('>>>')) {
          // 根据步骤更新进度
          if (message.includes('本地打包')) {
            deployProgress.value = 10
          } else if (message.includes('上传')) {
            deployProgress.value = 50
          } else if (message.includes('部署')) {
            deployProgress.value = 80
          } else if (message.includes('重启')) {
            deployProgress.value = 90
          } else if (message.includes('完成') || message.includes('Done')) {
            deployProgress.value = 100
          }
        }

        resultContent.value += message + '\n'
        nextTick(() => {
          const els = document.querySelectorAll('.log-content')
          els.forEach(el => el.scrollTop = el.scrollHeight)
        })
      }
      socket.onerror = () => {
        resultContent.value += '\n>>> 连接发生错误'
        resultStatus.value = 'error'
      }
      socket.onclose = () => {
        resultContent.value += '\n>>> 执行结束'
        deployProgress.value = 100
        fetchData()
      }
      execSocket.value = socket
    } catch (e) {
      resultContent.value += '\n无法建立连接: ' + e.message
      resultStatus.value = 'error'
    }
  }).catch(() => {})
}

// 复制日志内容
const copyResultContent = () => {
  navigator.clipboard.writeText(resultContent.value).then(() => {
    ElMessage.success('日志已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

watch(resultVisible, (val) => {
  if (!val && execSocket.value) {
    execSocket.value.close()
    execSocket.value = null
  }
})

const handleLog = (row) => {
  logContent.value = '正在连接 WebSocket...'
  logVisible.value = true
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.hostname
  const port = '8080'
  const wsUrl = `${protocol}//${host}:${port}/ws/log/${row.id}`
  try {
    const socket = new WebSocket(wsUrl)
    socket.onopen = () => logContent.value = '>>> 连接成功，正在获取日志...\n'
    socket.onmessage = (e) => {
      logContent.value += e.data + '\n'
      nextTick(() => {
        const el = document.querySelector('.log-content')
        if (el) el.scrollTop = el.scrollHeight
      })
    }
    socket.onclose = () => logContent.value += '\n>>> 连接已断开'
    logSocket.value = socket
  } catch (e) {
    logContent.value = '无法建立连接: ' + e.message
  }
}

watch(logVisible, (val) => {
  if (!val && logSocket.value) {
    logSocket.value.close()
    logSocket.value = null
  }
})

const handleRollback = (row) => {
  ElMessageBox.confirm('确认执行版本回退?', '警告', { type: 'warning' }).then(() => {
    resultContent.value = `正在连接 WebSocket 执行版本回退...\n`
    resultStatus.value = 'success'
    resultVisible.value = true
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.hostname
    const port = '8080'
    const wsUrl = `${protocol}//${host}:${port}/ws/exec/${row.id}/rollback`
    try {
      const socket = new WebSocket(wsUrl)
      socket.onopen = () => resultContent.value += '>>> 连接成功，开始执行...\n'
      socket.onmessage = (e) => {
        resultContent.value += e.data + '\n'
        nextTick(() => {
          const els = document.querySelectorAll('.log-content')
          els.forEach(el => el.scrollTop = el.scrollHeight)
        })
      }
      socket.onclose = () => {
        fetchData()
      }
      execSocket.value = socket
    } catch (e) {}
  }).catch(() => {})
}

// 处理回退命令（快速回退或选择历史版本）
const handleRollbackCommand = (command, row) => {
  if (command === 'quick') {
    handleRollback(row)
  } else if (command === 'history') {
    openVersionHistory(row)
  }
}

// 打开版本历史对话框
const openVersionHistory = async (row) => {
  currentService.value = row
  versionHistory.value = []
  versionHistoryVisible.value = true

  try {
    const res = await request.get(`/deployment-records/service/${row.id}/versions`)
    versionHistory.value = res || []
  } catch (e) {
    ElMessage.error('获取版本历史失败')
  }
}

// 获取部署状态类型
const getDeploymentStatusType = (status) => {
  const statusMap = { 0: '', 1: 'warning', 2: 'success' }
  return statusMap[status] || 'info'
}

// 获取部署状态文本
const getDeploymentStatusText = (status) => {
  const statusMap = { 0: '进行中', 1: '失败', 2: '成功' }
  return statusMap[status] || '未知'
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return `${size.toFixed(1)} ${units[unitIndex]}`
}

// 回退到指定版本
const handleRollbackToVersion = (version) => {
  ElMessageBox.confirm(`确认回退到版本 ${version.createTime}?`, '警告', { type: 'warning' }).then(() => {
    // 使用异步 API（不打开 WebSocket 窗口）
    request.post(`/service/deployment/${version.id}/rollback-async`).then(res => {
      if (res.success) {
        ElMessage.success(res.message || '正在回退，详细信息去发版记录查看')
        versionHistoryVisible.value = false
        fetchData()
      } else {
        ElMessage.error(res.message || '回退失败')
      }
    }).catch(err => {
      ElMessage.error('回退请求失败: ' + (err.message || '未知错误'))
    })
  }).catch(() => {})
}

// 编辑版本描述
const handleEditVersion = (version) => {
  versionEditForm.id = version.id
  versionEditForm.description = version.versionDescription || ''
  versionEditForm.tag = version.versionTag || ''
  versionEditVisible.value = true
}

// 保存版本信息
const handleSaveVersionInfo = async () => {
  try {
    await request.put(`/deployment-records/${versionEditForm.id}/version-info`, {
      description: versionEditForm.description,
      tag: versionEditForm.tag
    })
    ElMessage.success('保存成功')
    versionEditVisible.value = false
    // 刷新版本历史
    if (currentService.value) {
      openVersionHistory(currentService.value)
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

// 查看版本日志
const handleViewVersionLog = (version) => {
  logContent.value = '正在获取日志...'
  logVisible.value = true
  request.get(`/deployment-records/${version.id}/logs`).then(data => {
    logContent.value = data || '无日志内容'
  }).catch(() => {
    logContent.value = '获取日志失败'
  })
}

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

const handleTerminal = (row) => {
  // 重置所有状态
  chatMessages.value = []
  recommendedCommands.value = []
  chatInput.value = ''

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
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.hostname
    const port = '8080'
    const wsUrl = `${protocol}//${host}:${port}/ws/terminal/${row.id}`

    try {
      terminalSocket.value = new WebSocket(wsUrl)

      terminalSocket.value.onopen = () => {
        attachAddon.value = new AttachAddon(terminalSocket.value)
        terminal.value.loadAddon(attachAddon.value)
        terminal.value.write('>>> 终端连接成功\r\n')
      }

      terminalSocket.value.onerror = (error) => {
        console.error('WebSocket 连接错误:', error)
        terminal.value.write('\r\n>>> 连接失败，请检查后端服务是否启动\r\n')
      }

      terminalSocket.value.onclose = (event) => {
        console.log('WebSocket 连接关闭:', event.code, event.reason)
        if (!event.wasClean) {
          terminal.value.write('\r\n>>> 连接异常关闭\r\n')
        }
      }
    } catch (e) {
      console.error('创建 WebSocket 失败:', e)
      terminal.value.write('\r\n>>> 创建连接失败，请检查网络或后端服务\r\n')
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
  } catch (e) {}
  chatInput.value = ''
}

const isSafeCommand = (cmd) => !/rm\s+-rf|mkfs|shutdown|reboot/i.test(cmd)
const sendCommandToTerminal = (cmd) => {
  if (terminalSocket.value) terminalSocket.value.send(cmd + '\r')
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
.log-content {
  background: #1e1e1e;
  color: #fff;
  padding: 15px;
  border-radius: 4px;
  max-height: 500px;
  overflow: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  white-space: pre-wrap;
  font-size: 13px;
  line-height: 1.5;
}
.log-with-progress {
  max-height: 450px;
}
/* 日志中不同步骤的颜色标识 */
.log-content:deep('>>>') {
  color: #409eff;
  font-weight: bold;
}
/* 进度标签样式 */
.result-header {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}
.repo-config-list {
  margin-top: 20px;
  max-height: 60vh;
  overflow-y: auto;
}
.repo-config-card {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 15px;
  background-color: #f9fafc;
}
.repo-info {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px dashed #dcdfe6;
  padding-bottom: 10px;
}
.repo-url {
  font-family: monospace;
  font-size: 13px;
  color: #606266;
}
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
.chat-window, .command-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}
.chat-content, .list-content {
  flex: 1;
  padding: 10px;
  overflow-y: auto;
}
.chat-input, .chat-header, .list-header {
  padding: 10px;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
}
.chat-header, .list-header {
  border-top: none;
  border-bottom: 1px solid #e4e7ed;
}
.command-item {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}
.terminal-content {
  flex: 1;
  background: #282a36;
}

/* 版本历史样式 */
.version-card {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.version-header {
  display: flex;
  gap: 10px;
  align-items: center;
}
.version-info {
  color: #606266;
  font-size: 14px;
}
.version-info p {
  margin: 5px 0;
}
.version-info code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  color: #e83e8c;
}
.version-actions {
  display: flex;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}
</style>