<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增服务</el-button>
      <el-button type="primary" :disabled="selectedServices.length === 0" @click="handleBatchDeploy">
        批量发版<el-text v-if="selectedServices.length > 0" style="margin-left: 5px;">({{ selectedServices.length }})</el-text>
      </el-button>
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
            <el-option label="发版中" value="3" />
            <el-option label="发版失败" value="4" />
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

    <el-table :data="tableData" style="width: 100%" v-loading="loading" @selection-change="handleSelectionChange">
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
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="gitBranch" label="分支" width="100" />
      <el-table-column prop="runStatus" label="运行状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.runStatus)">
            {{ getStatusText(scope.row.runStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastDeployTime" label="上次发版时间" width="160">
        <template #default="scope">
          {{ formatLastDeployTime(scope.row.lastDeployTime) }}
        </template>
      </el-table-column>
      <el-table-column label="启用状态" width="120">
        <template #default="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="handleToggleEnabled(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handleAction(scope.row, 'deploy')">发版</el-button>
          <el-dropdown style="margin-left: 10px;" @command="(cmd) => handleMoreCommand(cmd, scope.row)">
            <el-button size="small">
              更多<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <!-- 后端和管理后台项目显示：启动/重启、日志、终端 -->
                <template v-if="scope.row.repositoryType === 1 || scope.row.repositoryType === 2">
                  <el-dropdown-item command="restart">启动/重启</el-dropdown-item>
                  <el-dropdown-item command="log">日志</el-dropdown-item>
                  <el-dropdown-item command="terminal">终端</el-dropdown-item>
                </template>
                <!-- 发版记录 -->
                <el-dropdown-item command="deployment_records" divided>发版记录</el-dropdown-item>
                <!-- 版本回退子菜单 -->
                <el-dropdown-item command="rollback_quick">快速回退（上一版本）</el-dropdown-item>
                <el-dropdown-item command="rollback_history">选择历史版本回退</el-dropdown-item>
                <el-dropdown-item command="edit" divided>编辑</el-dropdown-item>
                <el-dropdown-item command="delete" style="color: #f56c6c;">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
              <el-col :span="24">
                <el-form-item label="部署路径" label-width="90px">
                  <el-input :value="getComputedDeployPath(item)" readonly />
                  <span style="font-size: 12px; color: #999;">
                    项目的部署根目录 + 项目编码 + 项目路径（自动计算）
                  </span>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="项目路径" label-width="90px">
                  <el-input v-model="item.projectPath" placeholder="例如：opster-backend、opster-frontend" clearable />
                  <span style="font-size: 12px; color: #999;">
                    相对于Git仓库的子目录路径，如果项目在仓库根目录则留空
                  </span>
                </el-form-item>
              </el-col>
              <!-- 仅后端项目显示日志路径 -->
              <el-col :span="12" v-if="item.repositoryType === 1">
                <el-form-item label="日志路径" label-width="90px">
                  <!-- 新增模式：显示计算后的路径（只读） -->
                  <el-input v-if="!form.id" :value="getComputedLogPath(item)" readonly />
                  <!-- 编辑模式：显示服务端返回的值（可编辑） -->
                  <el-input v-else v-model="item.logPath" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <!-- 后端项目显示 Maven 命令 -->
                <el-form-item v-if="item.repositoryType === 1" label="Maven命令" label-width="90px">
                  <el-input v-model="item.mavenCmd" type="textarea" :rows="1" placeholder="mvn clean package -DskipTests" />
                </el-form-item>
                <!-- 前端项目显示构建命令 -->
                <el-form-item v-else label="构建命令" label-width="90px">
                  <el-input v-model="item.buildCmd" type="textarea" :rows="1" placeholder="npm run build" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <!-- 前端项目显示 Node.js 版本配置 -->
                <el-form-item v-if="item.repositoryType !== 1" label="Node版本" label-width="90px">
                  <el-select
                    v-model="item.nodeVersion"
                    placeholder="选择或输入版本"
                    filterable
                    allow-create
                    default-first-option
                    style="width: 100%">
                    <el-option label="使用系统默认" value="" />
                    <el-option v-for="version in installedNodeVersions"
                               :key="version"
                               :label="version"
                               :value="version" />
                  </el-select>
                  <div style="font-size: 12px; color: #999; margin-top: 5px;">
                    格式：v18.17.0，留空使用系统默认
                  </div>
                </el-form-item>
                <!-- 后端项目显示 JDK 版本选择 -->
                <el-form-item v-else label="JDK版本" label-width="90px">
                  <el-select
                    v-model="item.nodeVersion"
                    placeholder="选择JDK版本"
                    style="width: 100%">
                    <el-option label="使用系统默认" value="" />
                    <el-option label="JDK 8" value="jdk8" />
                    <el-option label="JDK 17" value="jdk17" />
                  </el-select>
                  <div style="font-size: 12px; color: #999; margin-top: 5px;">
                    选择构建使用的 JDK 版本
                  </div>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 仅后端项目显示启动脚本和监控地址 -->
            <el-row :gutter="20" v-if="item.repositoryType === 1">
              <el-col :span="12">
                <el-form-item label="启动脚本" label-width="90px">
                  <el-input v-model="item.startScript" type="textarea" :rows="1" placeholder="./start.sh" />
                  <div style="margin-top: 5px;">
                    <el-button
                      type="primary"
                      size="small"
                      @click="handleGenerateScript(item)">
                      生成脚本
                    </el-button>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="监控地址" label-width="90px">
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

    <!-- 发版记录对话框 -->
    <el-dialog v-model="deploymentRecordsVisible" title="发版记录" width="80%" :close-on-click-modal="false">
      <el-table :data="deploymentRecords" style="width: 100%" v-loading="deploymentRecordsLoading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="createTime" label="发版时间" width="170" />
        <el-table-column label="发版状态" width="100">
          <template #default="scope">
            <el-tag :type="getRecordStatusType(scope.row.status)">
              {{ getRecordStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="scope">
            <el-tag v-if="scope.row.isRollback" type="warning" size="small">回退</el-tag>
            <el-tag v-else type="success" size="small">发版</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="versionTag" label="版本标签" width="120">
          <template #default="scope">
            {{ scope.row.versionTag || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="versionDescription" label="版本描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="gitCommitHash" label="Git提交" width="100">
          <template #default="scope">
            <code v-if="scope.row.gitCommitHash">{{ scope.row.gitCommitHash.substring(0, 8) }}</code>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleViewRecordLog(scope.row)">查看日志</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!deploymentRecordsLoading && deploymentRecords.length === 0" description="暂无发版记录" />
    </el-dialog>

    <!-- 启动脚本预览对话框 -->
    <el-dialog v-model="scriptDialogVisible" title="启动脚本预览" width="900px">
      <div class="script-header">
        <el-space>
          <el-tag type="info">标准 Spring Boot 启动脚本</el-tag>
          <el-tag type="success">自动检查端口占用</el-tag>
          <el-tag type="warning">优雅停止旧进程</el-tag>
          <el-tag type="primary">日志输出到logs/目录</el-tag>
          <el-tag type="info">内容可编辑</el-tag>
        </el-space>
      </div>

      <div class="script-content">
        <el-input
          v-model="generatedScript"
          type="textarea"
          :rows="25"
          placeholder="脚本内容"
          class="script-editor"
        />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="copyScript">复制到剪贴板</el-button>
          <el-button @click="downloadScript">下载脚本文件</el-button>
          <el-button @click="handleUploadToServer" type="primary" :loading="uploadingScript" :disabled="uploadingScript">
            上传到服务器
          </el-button>
          <el-button @click="scriptDialogVisible = false">关闭</el-button>
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
const installedNodeVersions = ref([])  // 已安装的 Node.js 版本列表
const selectedServices = ref([])  // 选中的服务列表

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

// 发版记录相关
const deploymentRecordsVisible = ref(false)
const deploymentRecords = ref([])
const deploymentRecordsLoading = ref(false)

// 脚本生成相关状态
const scriptDialogVisible = ref(false)
const generatedScript = ref('')
const currentEditingItem = ref(null)
const uploadingScript = ref(false) // 上传中的状态

// 仓库类型映射
const repoTypeMap = {
  0: '前端',
  1: '后端',
  2: '管理后台',
  3: '移动端'
}
const getRepoTypeLabel = (type) => repoTypeMap[type] || '未知'

// 仓库类型颜色映射
const getRepoTypeColor = (type) => {
  const colorMap = {
    0: 'success',    // 前端 - 绿色
    1: 'primary',    // 后端 - 蓝色
    2: 'warning',    // 管理后台 - 橙色
    3: 'info'        // 移动端 - 灰色
  }
  return colorMap[type] || 'default'
}

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
        // deployPath 已移到项目配置中
        logPath: '', // 新增模式下留空，自动计算显示
        projectPath: repo.projectPath || '', // 新增字段，项目路径
        mavenCmd: isFrontend ? '' : 'mvn clean package -DskipTests',
        buildCmd: isFrontend ? 'npm install && npm run build' : '', // 新增字段
        nodeVersion: isFrontend ? '' : 'jdk17', // 前端手动填写，后端默认 jdk17
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
  const map = {
    0: 'info',      // 未启动 - 灰色
    1: 'success',   // 正常 - 绿色
    2: 'danger',    // 异常 - 红色
    3: 'warning',   // 发版中 - 橙色
    4: 'danger'     // 发版失败 - 红色
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    0: '未启动',
    1: '正常',
    2: '异常',
    3: '发版中',
    4: '发版失败'
  }
  return map[status] || '未知'
}

// 格式化上次发版时间
const formatLastDeployTime = (time) => {
  if (!time) return '-'

  try {
    const date = new Date(time)
    const now = new Date()
    const diff = now - date

    // 小于1小时
    if (diff < 3600000) {
      const minutes = Math.floor(diff / 60000)
      return minutes < 1 ? '刚刚' : `${minutes}分钟前`
    }

    // 今天
    if (date.toDateString() === now.toDateString()) {
      return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }

    // 昨天
    const yesterday = new Date(now)
    yesterday.setDate(yesterday.getDate() - 1)
    if (date.toDateString() === yesterday.toDateString()) {
      return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }

    // 更早
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    console.error('时间格式化失败:', time, e)
    return time
  }
}

const getProjectName = (id) => {
  const p = projects.value.find(i => i.id === id)
  return p ? p.projectName : id
}

// 获取项目显示文本（项目名称-项目别名）
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

const getServerName = (id) => {
  const s = servers.value.find(i => i.id === id)
  return s ? s.alias : id
}

// 获取服务器IP
const getServerIp = (id) => {
  const s = servers.value.find(i => i.id === id)
  return s ? s.ip : id
}

// 从 Git URL 提取服务别名（仓库名）
const extractServiceAliasFromGitUrl = (gitUrl) => {
  if (!gitUrl) return 'service'

  try {
    let repoName = ''

    // 处理 SSH 格式：git@github.com:xxx/repo.git
    if (gitUrl.startsWith('git@')) {
      const colonIndex = gitUrl.indexOf(':')
      if (colonIndex > 0) {
        const pathPart = gitUrl.substring(colonIndex + 1)
        const parts = pathPart.split('/')
        repoName = parts[parts.length - 1]
      }
    }
    // 处理 HTTPS/HTTP 格式
    else {
      // 移除协议部分
      let urlWithoutProtocol = gitUrl.replace(/^https?:\/\//, '')
      // 移除认证信息（如：username:password@）
      const atIndex = urlWithoutProtocol.indexOf('@')
      if (atIndex > 0) {
        urlWithoutProtocol = urlWithoutProtocol.substring(atIndex + 1)
      }
      // 按 / 分割，获取最后一部分
      const parts = urlWithoutProtocol.split('/')
      repoName = parts[parts.length - 1]
    }

    // 移除 .git 后缀
    if (repoName.endsWith('.git')) {
      repoName = repoName.substring(0, repoName.length - 4)
    }

    // 移除其他可能的特殊字符
    repoName = repoName.replace(/[^a-zA-Z0-9_-]/g, '')

    return repoName || 'service'
  } catch (e) {
    console.error('从 Git URL 提取服务别名失败:', gitUrl, e)
    return 'service'
  }
}

// 计算完整部署路径（用于表格显示）
const getFullDeployPath = (row) => {
  const project = projects.value.find(p => p.id === row.projectId)
  if (!project) return ''

  // deployPath 现在从项目配置获取
  const deployPath = project.deployPath || ''
  const projectCode = project.projectCode || ''
  // 从 Git URL 自动提取服务别名
  const gitUrl = row.repoGitUrl || ''
  const serviceAlias = extractServiceAliasFromGitUrl(gitUrl)
  const projectPath = row.projectPath || ''

  // 构建路径：{deployPath}/{projectCode}/{serviceAlias}/{projectPath}
  let fullPath = deployPath
  if (projectCode) {
    fullPath += '/' + projectCode
    if (serviceAlias) {
      fullPath += '/' + serviceAlias
      if (projectPath) {
        fullPath += '/' + projectPath
      }
    }
  }
  return fullPath
}

// 计算完整部署路径（用于表单显示）
const getComputedDeployPath = (item) => {
  const project = projects.value.find(p => p.id === form.projectId)
  if (!project) return ''

  // deployPath 现在从项目配置获取
  const deployPath = project.deployPath || ''
  const projectCode = project.projectCode || ''
  // 从 Git URL 自动提取服务别名
  const gitUrl = item.repoGitUrl || ''
  const serviceAlias = extractServiceAliasFromGitUrl(gitUrl)
  const projectPath = item.projectPath || ''

  // 构建路径：{deployPath}/{projectCode}/{serviceAlias}/{projectPath}
  let fullPath = deployPath
  if (projectCode) {
    fullPath += '/' + projectCode
    if (serviceAlias) {
      fullPath += '/' + serviceAlias
      if (projectPath) {
        fullPath += '/' + projectPath
      }
    }
  }
  return fullPath
}

// 计算日志路径（新增模式下自动计算）
const getComputedLogPath = (item) => {
  const deployPath = getComputedDeployPath(item)
  if (!deployPath) return ''
  return deployPath + '/logs/app.log'
}

// 获取已安装的 Node.js 版本列表
const fetchInstalledNodeVersions = async () => {
  try {
    const res = await request.get('/node-version/installed')
    if (res.success) {
      installedNodeVersions.value = res.data || []
    }
  } catch (e) {
    console.error('获取 Node.js 版本列表失败:', e)
    // 不影响主流程，静默失败
  }
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
    // deployPath 已移到项目配置中
    logPath: row.logPath,
    projectPath: row.projectPath || '', // 新增字段，项目路径
    mavenCmd: row.mavenCmd,
    buildCmd: row.buildCmd, // 新增字段
    nodeVersion: row.nodeVersion || '', // Node.js 版本
    startScript: row.startScript,
    monitorUrl: row.monitorUrl,
    status: row.status,
    scriptUploaded: row.scriptUploaded || 0 // 脚本上传状态
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
      // 批量新增 - 自动计算日志路径
      const services = form.items.map(item => {
        const serviceItem = { ...item, projectId: form.projectId }
        // 如果日志路径为空，自动计算
        if (!serviceItem.logPath) {
          serviceItem.logPath = getComputedLogPath(item)
        }
        return serviceItem
      })
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

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedServices.value = selection
}

// 批量发版
const handleBatchDeploy = () => {
  if (selectedServices.value.length === 0) {
    return ElMessage.warning('请至少选择一个服务')
  }

  // 检查是否有发版中的服务
  const deployingServices = selectedServices.value.filter(s => s.runStatus === 3)
  if (deployingServices.length > 0) {
    return ElMessage.warning(`${deployingServices.length} 个服务正在发版中，请稍后再试`)
  }

  // 显示确认对话框
  const serviceNames = selectedServices.value.map(s => getProjectDisplay(s)).join('、')
  ElMessageBox.confirm(
    `确认批量发版以下 ${selectedServices.value.length} 个服务?\n\n${serviceNames}`,
    '批量发版确认',
    {
      type: 'warning',
      confirmButtonText: '确认发版',
      cancelButtonText: '取消'
    }
  ).then(async () => {
    try {
      // 调用批量发版 API
      const serviceIds = selectedServices.value.map(s => s.id)
      const res = await request.post('/service/batch-deploy', serviceIds)

      if (res.success) {
        ElMessage.success(`已提交 ${serviceIds.length} 个服务的发版任务，详细信息去发版记录查看`)

        // 清空选择
        selectedServices.value = []

        // 发版中时每5秒刷新一次状态
        const refreshTimer = setInterval(async () => {
          const hasDeploying = tableData.value.some(item => item.runStatus === 3)
          if (!hasDeploying) {
            clearInterval(refreshTimer)
          }
          await fetchData()
        }, 5000)

        fetchData()
      } else {
        ElMessage.error(res.message || '批量发版失败')
      }
    } catch (err) {
      ElMessage.error('批量发版请求失败: ' + (err.message || '未知错误'))
    }
  }).catch(() => {})
}

// Socket instances
const execSocket = ref(null)
const logSocket = ref(null)

const handleAction = (row, action) => {
  const actionNames = { 'deploy': '发版', 'restart': '启动/重启' }
  const actionName = actionNames[action] || action

  // 发版使用异步 API（不打开 WebSocket 窗口）
  if (action === 'deploy') {
    ElMessageBox.confirm(`确认执行【${actionName}】?`, '提示', { type: 'warning' }).then(() => {
      request.post(`/service/${row.id}/deploy-async`).then(res => {
        if (res.success) {
          ElMessage.success(res.message || '正在发版，详细信息去发版记录查看')

          // 发版中时每5秒刷新一次状态
          const refreshTimer = setInterval(async () => {
            const currentData = tableData.value.find(item => item.id === row.id)
            if (currentData && currentData.runStatus !== 3) {
              clearInterval(refreshTimer)
            }
            await fetchData()
          }, 5000)

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

  // 重启操作使用 WebSocket
  ElMessageBox.confirm(`确认执行【${actionName}】?`, '提示', { type: 'warning' }).then(() => {
    resultContent.value = `正在连接 WebSocket 执行 ${actionName}...\n`
    resultStatus.value = 'success'
    deployProgress.value = 0 // 重置进度
    resultVisible.value = true

    // 使用环境变量配置的 WebSocket 地址
    const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/ws/exec/${row.id}/${action}`

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
  // 使用环境变量配置的 WebSocket 地址
  const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/ws/log/${row.id}`
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
    // 使用环境变量配置的 WebSocket 地址
    const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/ws/exec/${row.id}/rollback`
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

// 处理"更多"下拉菜单的命令
const handleMoreCommand = (command, row) => {
  switch (command) {
    case 'restart':
      handleAction(row, 'restart')
      break
    case 'log':
      handleLog(row)
      break
    case 'terminal':
      handleTerminal(row)
      break
    case 'deployment_records':
      openDeploymentRecords(row)
      break
    case 'rollback_quick':
      handleRollback(row)
      break
    case 'rollback_history':
      openVersionHistory(row)
      break
    case 'edit':
      handleEdit(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

// 处理回退命令（快速回退或选择历史版本）- 保留用于兼容
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
    // 使用环境变量配置的 WebSocket 地址
    const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/ws/terminal/${row.id}`

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

// ========== 启动脚本生成相关方法 ==========

// 生成启动脚本
const handleGenerateScript = (item) => {
  currentEditingItem.value = item
  generatedScript.value = generateStartScript(item)
  scriptDialogVisible.value = true
}

// 生成脚本内容
const generateStartScript = (item) => {
  const port = item.port || 8080

  return `#!/bin/bash
# Spring Boot 应用启动脚本
# 自动生成于 Opster 平台
# 服务端口: ${port}
# 生成时间: ${new Date().toLocaleString('zh-CN')}
# 使用方式: sh start.sh (无需传递参数)

# 配置项
JAR_NAME="*.jar"
JAVA_OPTS="\${JAVA_OPTS:-}"
PORT="${port}"
LOG_DIR="logs"
PID_FILE="app.pid"

# 颜色定义
RED='\\033[0;31m'
GREEN='\\033[0;32m'
YELLOW='\\033[1;33m'
NC='\\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "\${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] INFO: \$1\${NC}"
}

log_error() {
    echo -e "\${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: \$1\${NC}"
}

log_warn() {
    echo -e "\${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARN: \$1\${NC}"
}

# 检查Java是否可用
check_java() {
    # 优先使用 JAVA_HOME 中的 java
    if [ -n "\$JAVA_HOME" ] && [ -x "\$JAVA_HOME/bin/java" ]; then
        export PATH="\$JAVA_HOME/bin:\$PATH"
        log_info "使用 JAVA_HOME 中的 Java: \$JAVA_HOME/bin/java"
        return 0
    fi

    # 如果 JAVA_HOME 未设置，尝试自动检测常见的 Java 安装路径
    local common_java_paths=(
        "/usr/lib/jvm/java-17-openjdk"
        "/usr/lib/jvm/java-17"
        "/usr/lib/jvm/java-11-openjdk"
        "/usr/lib/jvm/java-11"
        "/usr/lib/jvm/java-8-openjdk"
        "/usr/lib/jvm/java-8"
        "/usr/lib/jvm/default-java"
        "/usr/java/default"
        "/opt/java/jdk17"
        "/opt/java/jdk11"
        "/usr/local/java/jdk17"
        "/usr/local/java/jdk11"
    )

    for java_path in "\${common_java_paths[@]}"; do
        if [ -x "\$java_path/bin/java" ]; then
            export JAVA_HOME="\$java_path"
            export PATH="\$JAVA_HOME/bin:\$PATH"
            log_info "自动检测到 Java: \$JAVA_HOME/bin/java"
            return 0
        fi
    done

    # 最后尝试使用 PATH 中的 java
    if command -v java &> /dev/null; then
        log_info "使用 PATH 中的 Java: \$(which java)"
        return 0
    fi

    # 都找不到，报错
    log_error "Java 未安装或不在 PATH 中"
    log_error "请设置 JAVA_HOME 环境变量指向 Java 安装目录"
    exit 1
}

# 获取应用PID
get_pid() {
    if [ -f "\$PID_FILE" ]; then
        cat "\$PID_FILE"
    fi
}

# 检查端口是否被占用
check_port() {
    local port=\$1

    # 检查端口是否被监听
    local pid=\$(lsof -ti:\$port 2>/dev/null)

    if [ -n "\$pid" ]; then
        log_warn "检测到端口 \$port 已被占用 (PID: \$pid)"

        # 检查是否是 Java 进程
        if ps -p "\$pid" -o command= 2>/dev/null | grep -q "java"; then
            log_warn "该进程是 Java 应用，尝试优雅停止..."
            graceful_stop "\$pid"
        else
            log_error "端口 \$port 被非 Java 进程占用 (PID: \$pid, Command: \$(ps -p \$pid -o command= | xargs))"
            log_error "无法自动停止，请手动处理后重试"
            exit 1
        fi

        # 等待端口释放
        sleep 2

        # 再次检查
        pid=\$(lsof -ti:\$port 2>/dev/null)
        if [ -n "\$pid" ]; then
            log_error "端口 \$port 仍被占用，优雅停止失败"
            log_error "请手动执行: lsof -ti:\$port | xargs kill"
            exit 1
        fi

        log_info "端口 \$port 已释放"
    fi
}

# 优雅停止进程
graceful_stop() {
    local pid=\$1
    local max_wait=30  # 最大等待30秒
    local waited=0

    log_info "发送 SIGTERM 信号到进程 \$pid ..."
    kill "\$pid" 2>/dev/null || return 0

    # 等待进程退出
    while ps -p "\$pid" > /dev/null 2>&1 && [ \$waited -lt \$max_wait ]; do
        sleep 1
        waited=\$((waited + 1))
        echo -n "."
    done
    echo ""

    # 检查进程是否已退出
    if ps -p "\$pid" > /dev/null 2>&1; then
        log_warn "进程 \$pid 在 \$max_wait 秒内未响应 SIGTERM"
        log_warn "如需强制停止，请执行: kill -9 \$pid"
        return 1
    else
        log_info "进程 \$pid 已优雅停止 (耗时: \${waited}秒)"
        return 0
    fi
}

# 检查应用是否已运行
check_running() {
    local pid=\$(get_pid)
    if [ -n "\$pid" ]; then
        if ps -p "\$pid" > /dev/null 2>&1; then
            # 进程存在，进一步验证是否是 Java 进程
            if ps -p "\$pid" -o command= | grep -q "java.*jar"; then
                log_warn "检测到应用已在运行 (PID: \$pid)"

                # 询问是否重启
                log_warn "准备优雅停止旧进程并重启..."
                graceful_stop "\$pid"

                # 清理 PID 文件
                rm -f "\$PID_FILE"
            else
                # PID 存在但不是 Java 进程，可能是 PID 重用
                log_warn "检测到旧 PID 文件，但进程不是 Java 应用，清理中..."
                rm -f "\$PID_FILE"
            fi
        else
            # PID 文件存在但进程已不存在，清理旧的 PID 文件
            log_warn "检测到残留的 PID 文件，进程已停止，清理中..."
            rm -f "\$PID_FILE"
        fi
    fi
}

# 启动应用
start() {
    log_info "=========================================="
    log_info "Spring Boot 应用启动"
    log_info "=========================================="

    # 检查Java
    check_java

    # 检查端口是否被占用
    log_info "检查端口 \$PORT 是否可用..."
    check_port \$PORT

    # 检查是否已运行
    check_running

    # 创建日志目录
    mkdir -p "\$LOG_DIR"

    # 查找jar文件
    jar_file=\$(ls -t \$JAR_NAME 2>/dev/null | head -1)
    if [ -z "\$jar_file" ]; then
        log_error "未找到jar文件: \$JAR_NAME"
        exit 1
    fi

    log_info "使用jar文件: \$jar_file"
    log_info "服务端口: \$PORT"
    log_info "日志文件: \$LOG_DIR/app.log"

    # 启动应用
    nohup java \$JAVA_OPTS -jar "\$jar_file" \\
        > "\$LOG_DIR/app.log" 2>&1 &
    echo \$! > "\$PID_FILE"

    # 等待启动
    sleep 3

    # 验证启动成功
    if ps -p \$(get_pid) > /dev/null 2>&1; then
        log_info "应用启动成功 (PID: \$(get_pid))"
        log_info "应用日志: \$LOG_DIR/app.log"
        log_info "=========================================="
        log_info "启动完成"
        log_info "=========================================="
    else
        log_error "应用启动失败，请检查日志: \$LOG_DIR/app.log"
        exit 1
    fi
}

# 执行启动
start
`
}

// 复制脚本到剪贴板
const copyScript = () => {
  navigator.clipboard.writeText(generatedScript.value).then(() => {
    ElMessage.success('脚本已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 下载脚本文件
const downloadScript = () => {
  const blob = new Blob([generatedScript.value], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'start.sh'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('脚本文件已下载')
}

// 上传到服务器
const handleUploadToServer = async () => {
  if (!currentEditingItem.value) {
    ElMessage.error('未找到服务信息')
    return
  }

  // 检查是否有服务ID（新增模式下的服务还没有保存）
  if (!currentEditingItem.value.id) {
    ElMessageBox.alert('请先保存服务后再上传脚本', '提示', { type: 'warning' })
    return
  }

  try {
    // 检查是否已上传过
    const alreadyUploaded = currentEditingItem.value.scriptUploaded === 1

    if (alreadyUploaded) {
      // 已上传过，询问是否覆盖
      await ElMessageBox.confirm(
        '脚本已上传过，是否覆盖？（旧脚本会自动备份）',
        '脚本已存在',
        {
          confirmButtonText: '覆盖（自动备份旧脚本）',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).catch(() => {
        throw new Error('cancel')
      })
    } else {
      // 未上传过，直接询问是否上传
      await ElMessageBox.confirm('确认将脚本上传到服务器？', '上传确认', {
        confirmButtonText: '确认上传',
        cancelButtonText: '取消',
        type: 'info'
      }).catch(() => {
        throw new Error('cancel')
      })
    }

    // 开始上传，显示 loading
    uploadingScript.value = true

    // 执行上传
    const uploadRes = await request.post(`/service/${currentEditingItem.value.id}/upload-start-script`, {
      scriptContent: generatedScript.value
    })

    if (uploadRes.success) {
      ElMessage.success('脚本上传成功')
      // 更新本地状态
      currentEditingItem.value.scriptUploaded = 1
    } else {
      ElMessage.error('上传失败: ' + (uploadRes.message || '未知错误'))
    }
  } catch (e) {
    if (e.message !== 'cancel') {
      ElMessage.error('操作失败: ' + (e.message || '未知错误'))
    }
  } finally {
    // 结束 loading
    uploadingScript.value = false
  }
}

// ========== 发版记录相关方法 ==========

// 打开发版记录对话框
const openDeploymentRecords = async (row) => {
  currentService.value = row
  deploymentRecords.value = []
  deploymentRecordsVisible.value = true
  deploymentRecordsLoading.value = true

  try {
    const res = await request.get(`/deployment-records/service/${row.id}/records`)
    deploymentRecords.value = res || []
  } catch (e) {
    ElMessage.error('获取发版记录失败')
  } finally {
    deploymentRecordsLoading.value = false
  }
}

// 获取记录状态类型
const getRecordStatusType = (status) => {
  const statusMap = {
    0: 'warning',   // 进行中
    1: 'success',   // 完成
    2: 'danger'     // 失败
  }
  return statusMap[status] || 'info'
}

// 获取记录状态文本
const getRecordStatusText = (status) => {
  const statusMap = {
    0: '进行中',
    1: '完成',
    2: '失败'
  }
  return statusMap[status] || '未知'
}

// 查看发版记录日志
const handleViewRecordLog = (record) => {
  logContent.value = '正在获取日志...'
  logVisible.value = true
  request.get(`/deployment-records/${record.id}/logs`).then(data => {
    logContent.value = data || '无日志内容'
  }).catch(() => {
    logContent.value = '获取日志失败'
  })
}

onMounted(() => {
  fetchData()
  fetchInstalledNodeVersions()
})
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

/* 脚本预览对话框样式 */
.script-header {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.script-content {
  margin: 15px 0;
}

.script-editor {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.script-editor :deep(.el-textarea__inner) {
  background: #1e1e1e;
  color: #f8f8f2;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
}
</style>