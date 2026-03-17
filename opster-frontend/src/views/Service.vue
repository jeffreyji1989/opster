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
      <el-table-column prop="serviceName" label="服务名称" width="120" />
      <el-table-column label="类型" width="100">
        <template #default="scope">
          <el-tag :type="getServiceTypeColor(scope.row.serviceType)" size="small">
            {{ getServiceTypeLabel(scope.row.serviceType) }}
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handleAction(scope.row, 'deploy')">发版</el-button>
          <el-button size="small" type="success" @click="handleCopy(scope.row)">复制</el-button>
          <el-dropdown style="margin-left: 10px;" @command="(cmd) => handleMoreCommand(cmd, scope.row)">
            <el-button size="small">
              更多<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <!-- 仅后端项目显示：启动/重启、日志、终端 -->
                <template v-if="scope.row.serviceType === 1">
                  <el-dropdown-item command="restart">启动/重启</el-dropdown-item>
                  <el-dropdown-item command="log">日志</el-dropdown-item>
                  <el-dropdown-item command="terminal">终端</el-dropdown-item>
                </template>
                <!-- 发版记录 -->
                <el-dropdown-item command="deployment_records" divided>发版记录</el-dropdown-item>
                <!-- 版本回退子菜单 -->
                <el-dropdown-item command="rollback_quick">快速回退（上一版本）</el-dropdown-item>
                <el-dropdown-item command="rollback_history">选择历史版本回退</el-dropdown-item>
                <!-- 配置文件管理 -->
                <el-dropdown-item command="config_files" divided>配置文件管理</el-dropdown-item>
                <!-- 启动脚本管理 -->
                <el-dropdown-item command="start_scripts" divided>启动脚本管理</el-dropdown-item>
                <el-dropdown-item command="edit" divided>编辑</el-dropdown-item>
                <el-dropdown-item command="toggle_enabled">{{ scope.row.status === 1 ? '禁用' : '启用' }}</el-dropdown-item>
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

        <div v-if="form.projectId">
          <!-- 添加服务按钮（仅在新增模式显示） -->
          <el-button v-if="!form.id" type="primary" size="small" @click="handleAddService" style="margin-bottom: 15px;">
            + 添加服务
          </el-button>

          <div class="repo-config-list">
            <el-collapse v-model="activeCollapseNames">
              <el-collapse-item v-for="(item, index) in form.items" :key="index" :name="index">
                <template #title>
                  <div class="collapse-title">
                    <el-tag size="small" type="primary">{{ item.subProjectName || item.serviceName || '服务 ' + (index + 1) }}</el-tag>
                    <el-tag size="small" type="info" style="margin-left: 8px;">{{ getServiceTypeLabel(item.serviceType) }}</el-tag>
                    <span v-if="item.repoGitUrl" class="repo-url" style="margin-left: 10px;">{{ item.repoGitUrl }}</span>
                    <!-- 删除按钮（仅在新增模式且有多个服务时显示） -->
                    <el-button
                      v-if="!form.id && form.items.length > 1"
                      type="danger"
                      size="small"
                      @click.stop="handleRemoveService(index)"
                      style="margin-left: 10px;">
                      删除
                    </el-button>
                  </div>
                </template>

            <!-- 第一行：服务名称、Git 地址、分支 -->
            <el-row :gutter="20">
              <el-col :span="6">
                <el-form-item label="服务名称" label-width="80px">
                  <el-input v-model="item.serviceName" placeholder="请输入服务名称" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="Git 地址" label-width="80px">
                  <!-- 新增模式：显示下拉选择框 -->
                  <el-select
                    v-if="!form.id"
                    v-model="item.subProjectId"
                    placeholder="选择仓库地址"
                    filterable
                    allow-create
                    style="width: 100%"
                    @change="(val) => handleSubProjectChange(index, val)">
                    <el-option
                      v-for="sub in availableSubProjects"
                      :key="sub.id"
                      :label="`${sub.subProjectName} - ${sub.gitUrl}`"
                      :value="sub.id" />
                  </el-select>
                  <!-- 编辑模式：显示 Git 地址输入框（只读） -->
                  <el-input
                    v-else
                    v-model="item.repoGitUrl"
                    placeholder="Git 仓库地址"
                    clearable
                    readonly>
                    <template #append>
                      <el-button @click="copyGitUrl(item.repoGitUrl)">复制</el-button>
                    </template>
                  </el-input>
                  <span style="font-size: 12px; color: #999;">
                    <span v-if="!form.id">可选择已有仓库或手动输入</span>
                    <span v-else>编辑模式下 Git 地址不可修改</span>
                  </span>
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="分支" label-width="50px">
                  <el-input v-model="item.gitBranch" placeholder="默认 master" />
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第二行：服务类型、服务器、端口号、环境 -->
            <el-row :gutter="20">
              <el-col :span="6">
                <el-form-item label="服务类型" label-width="80px">
                  <el-select v-model="item.serviceType" style="width: 100%" @change="(val) => handleServiceTypeChange(item, val)">
                    <el-option label="后端" :value="1" />
                    <el-option label="前端" :value="0" />
                    <el-option label="管理后台" :value="2" />
                    <el-option label="移动端" :value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="服务器" label-width="70px">
                  <el-select v-model="item.serverId" placeholder="选择服务器" style="width: 100%">
                    <el-option v-for="s in servers" :key="s.id" :label="`${s.alias} (${s.ip})`" :value="s.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="5">
                <el-form-item label="端口" label-width="50px">
                  <el-input-number v-model="item.port" :min="1" :max="65535" :step="1" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="5">
                <el-form-item label="环境" label-width="50px">
                  <el-select v-model="item.env" style="width: 100%">
                    <el-option label="生产" value="生产" />
                    <el-option label="测试" value="测试" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第三行：编译版本、编译路径 -->
            <el-row :gutter="20">
              <el-col :span="12">
                <!-- 前端/移动端项目显示 Node.js 版本选择 -->
                <el-form-item v-if="isFrontendType(item.serviceType)" label="编译版本" label-width="90px">
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
                    Node.js 版本，格式：v18.17.0，留空使用系统默认
                  </div>
                </el-form-item>
                <!-- 后端/管理后台项目显示 JDK 版本配置 -->
                <el-form-item v-else-if="isBackendType(item.serviceType)" label="编译版本" label-width="90px">
                  <el-select
                    v-model="item.nodeVersion"
                    placeholder="选择 JDK 版本"
                    style="width: 100%">
                    <el-option label="使用系统默认" value="" />
                    <el-option label="JDK 8" value="jdk8" />
                    <el-option label="JDK 17" value="jdk17" />
                  </el-select>
                  <div style="font-size: 12px; color: #999; margin-top: 5px;">
                    JDK 版本，留空使用系统默认
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="编译路径" label-width="90px">
                  <el-input v-model="item.compilePath" placeholder="例如：target、dist" clearable />
                  <span style="font-size: 12px; color: #999;">
                    编译工作目录（手动填写）
                  </span>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第四行：编译脚本 -->
            <el-row :gutter="20">
              <el-col :span="24">
                <el-form-item label="编译脚本" label-width="90px">
                  <el-input v-model="item.buildScript" type="textarea" :rows="2" placeholder="后端：mvn clean package -DskipTests；前端：npm install && npm run build" />
                  <span style="font-size: 12px; color: #999;">
                    统一 Maven 命令和构建命令
                  </span>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第五行：部署路径 -->
            <el-row :gutter="20">
              <el-col :span="24">
                <el-form-item label="部署路径" label-width="80px">
                  <el-input v-model="item.deployPath" placeholder="请输入部署路径，例如：/var/opster/eip" clearable />
                  <div style="margin-top: 5px;">
                    <span style="font-size: 12px; color: #999;">
                      服务器上的部署路径（手动填写）
                    </span>
                    <el-button
                      v-if="!item.deployPath"
                      type="primary"
                      size="small"
                      link
                      @click="handleAutoFillDeployPath(item)">
                      自动填充
                    </el-button>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 仅后端/管理后台项目显示启动脚本 -->
            <el-row :gutter="20" v-if="isBackendType(item.serviceType)">
              <el-col :span="24">
                <el-form-item label="启动脚本" label-width="80px">
                  <el-select
                    v-model="item.startScriptId"
                    placeholder="选择启动脚本（可选）"
                    filterable
                    clearable
                    style="width: 100%"
                    @change="(val) => handleStartScriptChange(item, val)">
                    <el-option
                      v-for="script in startScripts"
                      :key="script.id"
                      :label="script.name + (script.isDefault === 1 ? '（默认）' : '')"
                      :value="script.id" />
                  </el-select>
                  <div style="margin-top: 5px;">
                    <el-button
                      type="primary"
                      size="small"
                      @click="handleGoToScriptManagement">
                      脚本管理
                    </el-button>
                    <el-button
                      v-if="item.startScriptId"
                      type="info"
                      size="small"
                      @click="handleViewScriptDetail(item)">
                      查看脚本
                    </el-button>
                  </div>
                  <span style="font-size: 12px; color: #999;">
                    选择已创建的启动脚本，发版时会自动上传到服务器
                  </span>
                </el-form-item>
              </el-col>
            </el-row>
              </el-collapse-item>
            </el-collapse>
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
            进度：{{ deployProgress }}%
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
              <el-tag type="info">AI 命令生成器</el-tag>
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
            <el-tag type="success">SSH 终端</el-tag>
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
                <p v-if="version.gitCommitHash"><strong>Git 提交:</strong> <code>{{ version.gitCommitHash.substring(0, 8) }}</code></p>
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
        <el-table-column prop="gitCommitHash" label="Git 提交" width="100">
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
          <el-tag type="primary">日志输出到 logs/ 目录</el-tag>
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

    <!-- 配置文件管理对话框 -->
    <el-dialog v-model="configFilesVisible" title="配置文件管理" width="90%">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 15px;">
        <template #title>
          配置文件在发版时会自动上传到服务器部署目录下（与 jar 包同级），平台配置优先于 Git 仓库配置
        </template>
      </el-alert>

      <div class="config-header" style="margin-bottom: 15px;">
        <el-button type="primary" @click="handleAddConfigFile">
          + 新增配置文件
        </el-button>
      </div>

      <el-table :data="configFileList" style="width: 100%">
        <el-table-column prop="filename" label="文件名" width="250" />
        <el-table-column prop="lastModified" label="最后修改时间" width="180">
          <template #default="scope">
            {{ scope.row.lastModified ? scope.row.lastModified.replace('T', ' ').substring(0, 19) : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="versionTag" label="版本标签" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isDeployed ? 'success' : 'warning'">
              {{ scope.row.isDeployed ? '已部署' : '未部署' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleEditConfigFile(scope.row)">
              编辑
            </el-button>
            <el-button size="small" @click="handleViewConfigHistory(scope.row)">
              历史版本
            </el-button>
            <el-button size="small" type="danger" @click="handleDeleteConfigFile(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="configFilesVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 配置文件编辑对话框 -->
    <el-dialog v-model="configFileEditVisible" title="编辑配置文件" width="80%">
      <el-form :model="configEditForm" label-width="100px">
        <el-form-item label="文件名">
          <el-input v-model="configEditForm.filename" placeholder="如：application-dev.yml" :disabled="!!configEditForm.id" />
        </el-form-item>
        <el-form-item label="版本标签">
          <el-input v-model="configEditForm.versionTag" placeholder="如：v1.0.0" />
        </el-form-item>
        <el-form-item label="版本描述">
          <el-input
            v-model="configEditForm.versionDescription"
            type="textarea"
            :rows="2"
            placeholder="描述此版本的变更内容"
          />
        </el-form-item>
        <el-form-item label="配置内容">
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 10px;">
            <template #title>
              YAML 格式，注意缩进（2 空格）
            </template>
          </el-alert>
          <el-input
            v-model="configEditForm.content"
            type="textarea"
            :rows="20"
            style="font-family: monospace;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configFileEditVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveConfigFile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 配置文件版本历史对话框 -->
    <el-dialog v-model="configFileHistoryVisible" title="版本历史" width="80%">
      <el-timeline>
        <el-timeline-item
          v-for="version in configFileVersions"
          :key="version.id"
          :timestamp="version.createTime ? version.createTime.replace('T', ' ').substring(0, 19) : ''"
          placement="top">
          <el-card>
            <div class="version-card">
              <el-tag>{{ version.versionTag || '未标记' }}</el-tag>
              <el-tag :type="version.isDeployed ? 'success' : 'info'" style="margin-left: 10px;">
                {{ version.isDeployed ? '已部署' : '未部署' }}
              </el-tag>
              <p style="margin-top: 10px;">{{ version.versionDescription || '无描述' }}</p>
              <div class="version-actions" style="margin-top: 10px;">
                <el-button size="small" @click="handleViewVersionContent(version)">
                  查看内容
                </el-button>
                <el-button size="small" type="primary" @click="handleRollbackToConfigVersion(version)">
                  回退到此版本
                </el-button>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <template #footer>
        <el-button @click="configFileHistoryVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 启动脚本管理对话框 -->
    <el-dialog v-model="startScriptsVisible" title="启动脚本管理" width="90%">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 15px;">
        <template #title>
          配置启动脚本后，发版时会自动上传到服务器部署目录下（与 jar 包同级），平台配置的脚本优先于服务器上的脚本
        </template>
      </el-alert>

      <div class="start-script-header" style="margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center;">
        <el-space>
          <el-select
            v-model="selectedScriptId"
            placeholder="选择已创建的脚本"
            filterable
            clearable
            style="width: 300px"
            @change="handleSelectScript">
            <el-option
              v-for="script in allScripts"
              :key="script.id"
              :label="script.name + (script.isDefault === 1 ? '（默认）' : '')"
              :value="script.id" />
          </el-select>
          <el-button type="primary" @click="handleViewScriptDetail">
            查看脚本详情
          </el-button>
        </el-space>
        <el-space>
          <el-button type="primary" @click="handleCreateScript">
            <el-icon><Plus /></el-icon>
            新增脚本
          </el-button>
          <el-button @click="handleGoToScriptManagement">
            管理脚本
          </el-button>
        </el-space>
      </div>

      <el-table :data="scriptVersions" style="width: 100%">
        <el-table-column prop="versionNo" label="版本号" width="120" />
        <el-table-column prop="versionDescription" label="描述" width="200" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isActive === 1 ? 'success' : 'info'">
              {{ scope.row.isActive === 1 ? '激活' : '历史' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button
              v-if="scope.row.isActive !== 1"
              size="small"
              @click="handleActivateScriptVersion(scope.row)">
              激活
            </el-button>
            <el-button size="small" @click="handleViewScriptVersion(scope.row)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="startScriptsVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 启动脚本详情/编辑对话框 -->
    <el-dialog v-model="scriptDetailVisible" :title="scriptEditMode ? '编辑脚本' : '脚本详情'" width="900px">
      <div v-if="currentScriptDetail">
        <el-form :model="currentScriptDetail" label-width="100px">
          <el-form-item label="脚本名称">
            <el-input
              v-model="currentScriptDetail.name"
              :readonly="!scriptEditMode"
              placeholder="请输入脚本名称" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="currentScriptDetail.description"
              :readonly="!scriptEditMode"
              type="textarea"
              :rows="2"
              placeholder="请输入脚本描述" />
          </el-form-item>
          <el-form-item label="服务端口">
            <el-input-number
              v-model="currentScriptDetail.port"
              :readonly="!scriptEditMode"
              :min="1"
              :max="65535"
              style="width: 100%" />
          </el-form-item>
          <el-form-item label="JVM 参数">
            <el-input
              v-model="currentScriptDetail.jvmArgs"
              :readonly="!scriptEditMode"
              type="textarea"
              :rows="3"
              placeholder="例如：-Xms256m -Xmx512m -Dspring.profiles.active=dev" />
          </el-form-item>
          <el-form-item label="前置命令">
            <el-input
              v-model="currentScriptDetail.preJavaCmd"
              :readonly="!scriptEditMode"
              type="textarea"
              :rows="2"
              placeholder="在 Java 命令执行前运行的命令，例如：fuser -k 8080/tcp" />
          </el-form-item>
          <el-form-item label="后置命令">
            <el-input
              v-model="currentScriptDetail.postJavaCmd"
              :readonly="!scriptEditMode"
              type="textarea"
              :rows="2"
              placeholder="在 Java 命令执行后运行的命令，例如：curl http://localhost:8080/health" />
          </el-form-item>
          <el-form-item label="设为默认">
            <el-switch v-model="currentScriptDetail.isDefault" :disabled="!scriptEditMode" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <el-divider />
        <div style="margin-top: 20px;">
          <div style="margin-bottom: 10px; font-weight: bold;">
            脚本内容预览：
            <el-tag v-if="scriptEditMode" type="warning" size="small" style="margin-left: 10px;">
              保存后自动生成
            </el-tag>
          </div>
          <pre style="background: #f5f7fa; padding: 15px; border-radius: 4px; max-height: 500px; overflow: auto; font-size: 12px;">{{ currentScriptDetail.scriptContent }}</pre>
        </div>
      </div>
      <template #footer>
        <template v-if="scriptEditMode">
          <el-button @click="scriptDetailVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveScript">保存</el-button>
        </template>
        <template v-else>
          <el-button @click="scriptDetailVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleEditScript">编辑</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { ArrowDown, Plus } from '@element-plus/icons-vue'
import 'xterm/css/xterm.css'
import { Terminal } from 'xterm'
import { AttachAddon } from 'xterm-addon-attach'
import { FitAddon } from 'xterm-addon-fit'
import { getServiceConfigFiles, saveServiceConfigFile, deleteServiceConfigFile, getConfigFileVersions, rollbackConfigFileVersion } from '../api/service-config'
import { listScripts, getScriptDetail, createScript, updateScript, getDefaultScript } from '../api/service-start-script'

const loading = ref(false)
const tableData = ref([])
const projects = ref([])
const servers = ref([])
const businessLines = ref([])
const installedNodeVersions = ref([])  // 已安装的 Node.js 版本列表
const selectedServices = ref([])  // 选中的服务列表
const availableSubProjects = ref([])  // 项目可用的子项目列表

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

// 配置文件管理相关
const configFilesVisible = ref(false)
const configFileList = ref([])
const configFileEditVisible = ref(false)
const configFileHistoryVisible = ref(false)
const currentConfigFile = ref(null)
const configEditForm = reactive({
  id: null,
  filename: '',
  content: '',
  versionTag: '',
  versionDescription: ''
})
const configFileVersions = ref([])

// 启动脚本管理相关
const startScriptsVisible = ref(false)
const allScripts = ref([]) // 所有启动脚本列表
const selectedScriptId = ref(null) // 选中的脚本 ID
const scriptVersions = ref([]) // 当前脚本的版本列表
const currentScriptDetail = ref(null) // 当前脚本详情
const scriptDetailVisible = ref(false) // 脚本详情对话框
const scriptEditMode = ref(false) // 是否为编辑模式
const currentEditingService = ref(null) // 当前编辑的服务

// 发版记录相关
const deploymentRecordsVisible = ref(false)
const deploymentRecords = ref([])
const deploymentRecordsLoading = ref(false)

// 脚本生成相关状态
const scriptDialogVisible = ref(false)
const generatedScript = ref('')
const currentEditingItem = ref(null)
const uploadingScript = ref(false) // 上传中的状态

// 启动脚本管理相关
const startScripts = ref([]) // 启动脚本列表

// 服务类型映射（支持 4 种类型）
const serviceTypeMap = {
  0: '前端',
  1: '后端',
  2: '管理后台',
  3: '移动端'
}
const getServiceTypeLabel = (type) => {
  if (type === undefined || type === null) return '未知'
  return serviceTypeMap[type] || '后端'
}

// 服务类型颜色映射（支持 4 种类型）
const getServiceTypeColor = (type) => {
  const colorMap = {
    0: 'success',    // 前端 - 绿色
    1: 'primary',    // 后端 - 蓝色
    2: 'warning',    // 管理后台 - 橙色
    3: 'info'        // 移动端 - 灰色
  }
  return colorMap[type] || 'primary'
}

// 判断是否为后端类型（后端/管理后台）
const isBackendType = (type) => [1, 2].includes(type)

// 判断是否为前端类型（前端/移动端）
const isFrontendType = (type) => [0, 3].includes(type)

const form = reactive({
  id: null,
  projectId: null,
  items: []
})

// 折叠面板状态
const activeCollapseNames = ref([])

const handleProjectChange = async (projectId) => {
  if (!projectId) {
    form.items = []
    availableSubProjects.value = []
    return
  }

  try {
    // 从 SubProject 表获取该项目的代码仓库列表
    const subProjects = await request.get(`/sub-project/project/${projectId}`)

    // 保存可用的子项目列表
    availableSubProjects.value = Array.isArray(subProjects) ? subProjects : []

    // 如果有 SubProject 配置，添加第一个空白服务（默认选择第一个仓库）
    if (availableSubProjects.value.length > 0) {
      const firstSubProject = availableSubProjects.value[0]
      const isFrontend = ['frontend', 'mobile'].includes(firstSubProject.projectType)

      // 获取项目信息以计算部署路径
      const project = projects.value.find(p => p.id === projectId)
      const deployRootPath = project?.deployRootPath || project?.deployPath || ''
      const projectCode = project?.projectCode || ''
      const projectPath = firstSubProject.projectPath || ''

      // 计算默认部署路径
      const defaultDeployPath = deployRootPath && projectCode
        ? `${deployRootPath}/${projectCode}${projectPath ? '/' + projectPath : ''}`
        : ''

      form.items = [{
        // 基本信息
        serviceName: '',

        // 关联 SubProject
        subProjectId: firstSubProject.id,
        subProjectName: firstSubProject.subProjectName,

        // Git 信息
        repoGitUrl: firstSubProject.gitUrl,
        gitAccountId: firstSubProject.gitAccountId,
        gitBranch: firstSubProject.gitBranch || 'master',
        projectPath: firstSubProject.projectPath || '',
        deployPath: defaultDeployPath,  // 自动填充默认路径

        // 项目类型
        projectType: firstSubProject.projectType,
        serviceType: getServiceTypeValue(firstSubProject.projectType),

        // 部署配置
        serverId: null,
        env: '测试',
        port: isFrontend ? 80 : 8080,

        // 日志路径
        logPath: '',
        compilePath: '',

        // 构建配置
        buildScript: isFrontend ? 'npm install && npm run build' : 'mvn clean package -DskipTests',
        nodeVersion: isFrontend ? 'v14.18.2' : 'jdk8',  // 前端默认 v14.18.2，后端默认 jdk8

        // 启动脚本
        startScript: isFrontend ? '' : './start.sh',

        // 状态
        status: 1
      }]
    } else {
      // 如果没有 SubProject 配置，提供空白表单
      form.items = [{
        serviceName: '',
        subProjectId: null,
        subProjectName: '',
        repoGitUrl: '',
        gitAccountId: null,
        gitBranch: 'master',
        projectPath: '',
        deployPath: '',
        projectType: '',
        serviceType: 1,
        serverId: null,
        env: '测试',
        port: 8080,
        logPath: '',
        compilePath: '',
        buildScript: 'mvn clean package -DskipTests',
        nodeVersion: 'jdk8',  // 后端默认 JDK 版本
        startScript: './start.sh',
        status: 1
      }]
    }

    // 默认展开第一个面板
    activeCollapseNames.value = [0]
  } catch (error) {
    console.error('获取代码仓库列表失败:', error)
    availableSubProjects.value = []
    // 出错时也提供空白表单
    form.items = [{
      serviceName: '',
      subProjectId: null,
      subProjectName: '',
      repoGitUrl: '',
      gitAccountId: null,
      gitBranch: 'master',
      projectPath: '',
      deployPath: '',
      projectType: '',
      serviceType: 1,
      serverId: null,
      env: '测试',
      port: 8080,
      logPath: '',
      compilePath: '',
      buildScript: 'mvn clean package -DskipTests',
      nodeVersion: 'jdk8',  // 后端默认 JDK 版本
      startScript: './start.sh',
      status: 1
    }]
    activeCollapseNames.value = [0]
  }
}

// 处理子项目选择变化
const handleSubProjectChange = (itemIndex, subProjectId) => {
  const item = form.items[itemIndex]

  if (subProjectId && availableSubProjects.value.length > 0) {
    // 从下拉框选择
    const selectedSubProject = availableSubProjects.value.find(sub => sub.id === subProjectId)
    if (selectedSubProject) {
      item.subProjectName = selectedSubProject.subProjectName
      item.repoGitUrl = selectedSubProject.gitUrl
      item.gitAccountId = selectedSubProject.gitAccountId
      item.gitBranch = selectedSubProject.gitBranch || 'master'
      item.projectPath = selectedSubProject.projectPath || ''
      item.projectType = selectedSubProject.projectType
      item.serviceType = getServiceTypeValue(selectedSubProject.projectType)

      // 根据项目类型调整默认值（只有 frontend 和 mobile 是前端项目）
      const isFrontend = ['frontend', 'mobile'].includes(selectedSubProject.projectType)
      item.port = isFrontend ? 80 : 8080
      item.buildScript = isFrontend ? 'npm install && npm run build' : 'mvn clean package -DskipTests'
      item.nodeVersion = isFrontend ? 'v14.18.2' : 'jdk8'  // 前端默认 v14.18.2，后端默认 jdk8
    }
  } else {
    // 手动输入或清空
    item.subProjectName = ''
    item.repoGitUrl = ''
    item.gitAccountId = null
    item.projectPath = ''
    item.deployPath = ''
  }
}

// 复制 Git URL 到剪贴板
const copyGitUrl = (url) => {
  if (!url) {
    return ElMessage.warning('Git 地址为空')
  }
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('Git 地址已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 处理服务类型变化时自动更新编译脚本
const handleServiceTypeChange = (item, serviceType) => {
  // 类型配置映射
  const typeConfig = {
    0: { // 前端
      port: 80,
      buildScript: 'npm install && npm run build',
      showStartScript: false,
      defaultVersion: 'v14.18.2'  // 前端默认 Node.js 版本
    },
    1: { // 后端
      port: 8080,
      buildScript: 'mvn clean package -DskipTests',
      showStartScript: true,
      defaultVersion: 'jdk8'  // 后端默认 JDK 版本
    },
    2: { // 管理后台
      port: 8080,
      buildScript: 'mvn clean package -DskipTests',
      showStartScript: true,
      defaultVersion: 'jdk8'  // 管理后台默认 JDK 版本
    },
    3: { // 移动端
      port: 80,
      buildScript: 'npm install && npm run build',
      showStartScript: false,
      defaultVersion: 'v14.18.2'  // 移动端默认 Node.js 版本
    }
  }

  const config = typeConfig[serviceType] || typeConfig[1]

  // 更新编译脚本
  item.buildScript = config.buildScript

  // 更新默认端口
  item.port = config.port

  // 更新默认编译版本
  item.nodeVersion = config.defaultVersion

  // 更新启动脚本（只有后端/管理后台需要）
  if (config.showStartScript) {
    item.startScript = item.startScript || './start.sh'
  } else {
    item.startScript = ''
  }

  ElMessage.success('已根据服务类型自动更新配置')
}

// 自动填充部署路径
const handleAutoFillDeployPath = (item) => {
  const project = projects.value.find(p => p.id === form.projectId)
  if (!project) {
    return ElMessage.warning('请先选择项目')
  }

  // 使用项目配置的 deployRootPath 作为基础路径
  const deployRootPath = project.deployRootPath || project.deployPath || ''
  const projectCode = project.projectCode || ''
  const projectPath = item.projectPath || ''

  // 构建路径：{deployRootPath}/{projectCode}/{projectPath}
  let fullPath = deployRootPath
  if (projectCode) {
    fullPath += '/' + projectCode
    if (projectPath) {
      fullPath += '/' + projectPath
    }
  }

  if (fullPath) {
    item.deployPath = fullPath
    ElMessage.success('已自动填充部署路径')
  } else {
    ElMessage.warning('无法自动填充部署路径，请检查项目配置')
  }
}

// 将项目类型字符串转换为服务类型数字值
const getServiceTypeValue = (projectType) => {
  const typeMap = {
    'frontend': 0,   // 前端项目
    'backend': 1,    // 后端项目
    'admin': 2,      // 管理后台
    'mobile': 3      // 移动端
  }
  return typeMap[projectType] ?? 1
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

    // 小于 1 小时
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

// 获取项目显示文本（项目名称 - 项目别名）
const getProjectDisplay = (row) => {
  const p = projects.value.find(i => i.id === row.projectId)
  if (!p) return row.projectId

  const projectName = p.projectName

  // 从项目的 repositories 中找到对应的仓库别名
  // 通过 git 仓库地址 + 项目路径来区分
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

  // 组合显示：项目名称 - 别名
  if (repoAlias) {
    return `${projectName}-${repoAlias}`
  }
  return projectName
}

const getServerName = (id) => {
  const s = servers.value.find(i => i.id === id)
  return s ? s.alias : id
}

// 获取服务器 IP
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

  // 使用项目配置的 deployRootPath 作为基础路径
  const deployRootPath = project.deployRootPath || project.deployPath || ''
  const projectCode = project.projectCode || ''
  const projectPath = row.projectPath || ''

  // 根据最新的目录结构构建路径：{deployRootPath}/{projectCode}/{projectPath}
  // 注意：新目录结构移除了 {serviceAlias} 层级
  let fullPath = deployRootPath
  if (projectCode) {
    fullPath += '/' + projectCode
    // 如果有 projectPath，追加到路径末尾
    if (projectPath) {
      fullPath += '/' + projectPath
    }
  }
  return fullPath
}

// 计算完整部署路径（用于表单显示）
const getComputedDeployPath = (item) => {
  const project = projects.value.find(p => p.id === form.projectId)
  if (!project) return ''

  // 使用项目配置的 deployRootPath 作为基础路径
  const deployRootPath = project.deployRootPath || project.deployPath || ''
  const projectCode = project.projectCode || ''
  const projectPath = item.projectPath || ''

  // 根据最新的目录结构构建路径：{deployRootPath}/{projectCode}/{projectPath}
  // 注意：新目录结构移除了 {serviceAlias} 层级
  let fullPath = deployRootPath
  if (projectCode) {
    fullPath += '/' + projectCode
    // 如果有 projectPath，追加到路径末尾
    if (projectPath) {
      fullPath += '/' + projectPath
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

// 计算编辑模式下的完整部署路径
const getFullDeployPathForEdit = (item) => {
  const project = projects.value.find(p => p.id === form.projectId)
  if (!project) return ''

  // 使用项目配置的 deployRootPath 作为基础路径
  const deployRootPath = project.deployRootPath || project.deployPath || ''
  const projectCode = project.projectCode || ''

  // 从 Git URL 自动提取服务别名
  const gitUrl = item.repoGitUrl || ''
  const serviceAlias = extractServiceAliasFromGitUrl(gitUrl)
  const projectPath = item.projectPath || ''

  // 根据最新的目录结构构建路径：{deployRootPath}/{projectCode}
  // 注意：新目录结构移除了 {serviceAlias} 层级
  let fullPath = deployRootPath
  if (projectCode) {
    fullPath += '/' + projectCode
    // 如果有 projectPath，追加到路径末尾
    if (projectPath) {
      fullPath += '/' + projectPath
    }
  }
  return fullPath
}

// 计算源码目录（系统自动计算，只读）
const getComputedSourcePath = (item) => {
  const project = projects.value.find(p => p.id === form.projectId)
  if (!project) return ''

  // 从 application.yml 配置中获取 opster.deploy-path（这里需要从后端获取）
  // 暂时使用项目配置中的 deployRootPath
  const deployPath = project.deployRootPath || ''
  const projectCode = project.projectCode || ''

  // 构建路径：{deployPath}/{projectCode}/source
  let fullPath = deployPath
  if (projectCode) {
    fullPath += '/' + projectCode + '/source'
  }
  return fullPath
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

// 加载启动脚本列表
const loadStartScripts = async () => {
  try {
    const res = await listScripts()
    startScripts.value = res || []
  } catch (error) {
    console.error('加载启动脚本列表失败:', error)
  }
}

// 处理启动脚本选择变化
const handleStartScriptChange = (item, scriptId) => {
  if (scriptId) {
    // 选中的脚本，更新脚本内容
    const selectedScript = startScripts.value.find(s => s.id === scriptId)
    if (selectedScript) {
      item.startScript = selectedScript.scriptContent || `./start.sh`
    }
  } else {
    // 清空选择
    item.startScript = ''
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

// 添加服务（支持同一个 Git 地址配置多个部署服务）
const handleAddService = () => {
  // 获取当前项目信息
  const project = projects.value.find(p => p.id === form.projectId)
  if (!project) return

  // 如果有可用的子项目，默认选择第一个
  let defaultSubProjectId = null
  let defaultSubProjectName = ''
  let defaultRepoGitUrl = ''
  let defaultGitAccountId = null
  let defaultProjectPath = ''
  let defaultDeployPath = ''
  let defaultProjectType = ''
  let defaultServiceType = 1
  let defaultPort = 8080
  let defaultBuildScript = 'mvn clean package -DskipTests'
  let defaultNodeVersion = 'jdk8'  // 默认后端 JDK 版本

  if (availableSubProjects.value.length > 0) {
    const firstSubProject = availableSubProjects.value[0]
    defaultSubProjectId = firstSubProject.id
    defaultSubProjectName = firstSubProject.subProjectName
    defaultRepoGitUrl = firstSubProject.gitUrl
    defaultGitAccountId = firstSubProject.gitAccountId
    defaultProjectPath = firstSubProject.projectPath || ''
    defaultProjectType = firstSubProject.projectType
    defaultServiceType = getServiceTypeValue(firstSubProject.projectType)

    const isFrontend = ['frontend', 'mobile'].includes(firstSubProject.projectType)
    defaultPort = isFrontend ? 80 : 8080
    defaultBuildScript = isFrontend ? 'npm install && npm run build' : 'mvn clean package -DskipTests'
    defaultNodeVersion = isFrontend ? 'v14.18.2' : 'jdk8'  // 前端默认 v14.18.2，后端默认 jdk8

    // 计算默认部署路径
    const deployRootPath = project.deployRootPath || project.deployPath || ''
    const projectCode = project.projectCode || ''
    if (deployRootPath && projectCode) {
      defaultDeployPath = `${deployRootPath}/${projectCode}${defaultProjectPath ? '/' + defaultProjectPath : ''}`
    }
  }

  // 添加一个新的服务配置
  form.items.push({
    serviceName: '',
    subProjectId: defaultSubProjectId,
    subProjectName: defaultSubProjectName,
    repoGitUrl: defaultRepoGitUrl,
    gitAccountId: defaultGitAccountId,
    gitBranch: 'master',
    projectPath: defaultProjectPath,
    deployPath: defaultDeployPath,  // 自动填充默认路径
    projectType: defaultProjectType,
    serviceType: defaultServiceType,
    serverId: null,
    env: '测试',
    port: defaultPort,
    logPath: '',
    compilePath: '',
    buildScript: defaultBuildScript,
    nodeVersion: defaultNodeVersion,  // 根据类型设置默认版本
    // 启动脚本
    startScriptId: null,
    startScriptVersionId: null,
    startScript: defaultProjectType === 'backend' || defaultProjectType === 'admin' ? './start.sh' : '',
    status: 1
  })
  // 自动展开新添加的面板
  activeCollapseNames.value.push(form.items.length - 1)
}

// 删除服务
const handleRemoveService = (index) => {
  form.items.splice(index, 1)
  // 更新折叠面板状态
  activeCollapseNames.value = activeCollapseNames.value.filter(name => name !== index)
}

const handleEdit = async (row) => {
  form.id = row.id
  form.projectId = row.projectId

  // 先加载项目的子项目列表
  try {
    const subProjects = await request.get(`/sub-project/project/${row.projectId}`)
    availableSubProjects.value = Array.isArray(subProjects) ? subProjects : []
  } catch (error) {
    console.error('获取子项目列表失败:', error)
    availableSubProjects.value = []
  }

  // 尝试通过 repoGitUrl 匹配对应的子项目
  let matchedSubProjectId = row.subProjectId || null
  let matchedSubProjectName = row.subProjectName || ''
  let matchedProjectType = row.projectType || ''

  // 如果没有 subProjectId 但有 repoGitUrl，尝试匹配
  if (!matchedSubProjectId && row.repoGitUrl && availableSubProjects.value.length > 0) {
    const matched = availableSubProjects.value.find(sub => sub.gitUrl === row.repoGitUrl)
    if (matched) {
      matchedSubProjectId = matched.id
      matchedSubProjectName = matched.subProjectName
      matchedProjectType = matched.projectType
    }
  }

  // 编辑模式只编辑当前选中的一个服务
  form.items = [{
    id: row.id,
    serviceName: row.serviceName || '',
    // 关联子项目信息
    subProjectId: matchedSubProjectId,
    subProjectName: matchedSubProjectName,
    // Git 信息
    repoGitUrl: row.repoGitUrl || '',
    gitAccountId: row.gitAccountId || null,
    gitBranch: row.gitBranch || 'master',
    projectPath: row.projectPath || '',
    deployPath: row.deployPath || '',
    // 项目类型
    projectType: matchedProjectType,
    serviceType: row.serviceType ?? row.repositoryType ?? 1,
    // 服务器配置
    serverId: row.serverId,
    env: row.env,
    port: row.port,
    // 路径配置
    logPath: row.logPath,
    compilePath: row.compilePath || '',
    // 构建配置
    buildScript: row.buildScript || row.mavenCmd || row.buildCmd || '',
    nodeVersion: row.nodeVersion || '',
    // 启动脚本
    startScriptId: row.startScriptId || null,
    startScriptVersionId: row.startScriptVersionId || null,
    startScript: row.startScript || '',
    // 状态
    status: row.status,
    scriptUploaded: row.scriptUploaded || 0
  }]

  // 默认展开第一个面板
  activeCollapseNames.value = [0]
  dialogVisible.value = true
}

// 复制服务（复用 handleEdit 的逻辑，但设置为新增模式）
const handleCopy = async (row) => {
  // 重置表单为新增模式
  form.id = null
  form.projectId = row.projectId

  // 先加载项目的子项目列表
  try {
    const subProjects = await request.get(`/sub-project/project/${row.projectId}`)
    availableSubProjects.value = Array.isArray(subProjects) ? subProjects : []
  } catch (error) {
    console.error('获取子项目列表失败:', error)
    availableSubProjects.value = []
  }

  // 尝试通过 repoGitUrl 匹配对应的子项目
  let matchedSubProjectId = row.subProjectId || null
  let matchedSubProjectName = row.subProjectName || ''
  let matchedProjectType = row.projectType || ''

  if (!matchedSubProjectId && row.repoGitUrl && availableSubProjects.value.length > 0) {
    const matched = availableSubProjects.value.find(sub => sub.gitUrl === row.repoGitUrl)
    if (matched) {
      matchedSubProjectId = matched.id
      matchedSubProjectName = matched.subProjectName
      matchedProjectType = matched.projectType
    }
  }

  // 填充表单数据（复制当前服务的所有配置）
  form.items = [{
    serviceName: (row.serviceName || '') + '_copy',  // 服务名称添加后缀
    // 关联子项目信息
    subProjectId: matchedSubProjectId,
    subProjectName: matchedSubProjectName,
    // Git 信息
    repoGitUrl: row.repoGitUrl || '',
    gitAccountId: row.gitAccountId || null,
    gitBranch: row.gitBranch || 'master',
    projectPath: row.projectPath || '',
    deployPath: row.deployPath || '',
    // 项目类型
    projectType: matchedProjectType,
    serviceType: row.serviceType ?? row.repositoryType ?? 1,
    // 服务器配置
    serverId: row.serverId,
    env: row.env,
    port: row.port,
    // 路径配置
    logPath: row.logPath,
    compilePath: row.compilePath || '',
    // 构建配置
    buildScript: row.buildScript || row.mavenCmd || row.buildCmd || '',
    nodeVersion: row.nodeVersion || '',
    // 启动脚本
    startScriptId: row.startScriptId || null,
    startScriptVersionId: row.startScriptVersionId || null,
    startScript: row.startScript || '',
    // 状态
    status: 1,
    scriptUploaded: 0  // 复制后重置脚本上传状态
  }]

  // 默认展开第一个面板
  activeCollapseNames.value = [0]
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
    return ElMessage.warning(`代码仓库【${invalid.subProjectName || invalid.repoGitUrl}】未选择服务器`)
  }

  try {
    // 统一处理新增和编辑 - 自动计算日志路径
    const services = form.items.map(item => {
      const serviceItem = { ...item, projectId: form.projectId }
      // 如果日志路径为空，自动计算
      if (!serviceItem.logPath) {
        serviceItem.logPath = getComputedLogPath(item)
      }
      return serviceItem
    })

    if (form.id) {
      // 编辑模式 - 只更新当前服务
      await request.put('/service', services[0])
      ElMessage.success('更新成功')
    } else {
      // 批量新增
      await request.post('/service/batch', services)
      ElMessage.success('批量创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error('保存失败:', e)
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该服务？', '警告', { type: 'warning' }).then(async () => {
    await request.delete(`/service/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  })
}

// 处理启用/禁用
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

        // 发版中时每 5 秒刷新一次状态
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
      ElMessage.error('批量发版请求失败：' + (err.message || '未知错误'))
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

          // 发版中时每 5 秒刷新一次状态
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
        ElMessage.error('发版请求失败：' + (err.message || '未知错误'))
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
      resultContent.value += '\n无法建立连接：' + e.message
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
    logContent.value = '无法建立连接：' + e.message
  }
}

watch(logVisible, (val) => {
  if (!val && logSocket.value) {
    logSocket.value.close()
    logSocket.value = null
  }
})

const handleRollback = (row) => {
  ElMessageBox.confirm('确认执行版本回退？', '警告', { type: 'warning' }).then(() => {
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
    case 'config_files':
      openConfigFiles(row)
      break
    case 'start_scripts':
      openStartScripts(row)
      break
    case 'edit':
      handleEdit(row)
      break
    case 'toggle_enabled':
      handleToggleEnabled(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

// ==================== 配置文件管理相关函数 ====================

// 打开配置文件管理对话框
const openConfigFiles = async (row) => {
  currentService.value = row
  configFileList.value = []
  configFilesVisible.value = true

  try {
    const res = await getServiceConfigFiles(row.id)
    configFileList.value = res || []
  } catch (error) {
    console.error('获取配置文件列表失败:', error)
    ElMessage.error('获取配置文件列表失败')
  }
}

// ========== 启动脚本管理相关函数 ==========

// 打开启动脚本管理对话框
const openStartScripts = async (row) => {
  currentEditingService.value = row
  startScriptsVisible.value = true
  selectedScriptId.value = row.startScriptId

  // 加载所有脚本列表
  await loadAllScripts()

  // 加载当前脚本的版本列表
  if (selectedScriptId.value) {
    await loadScriptVersions(selectedScriptId.value)
  }
}

// 加载所有启动脚本列表
const loadAllScripts = async () => {
  try {
    const res = await listScripts()
    allScripts.value = res || []
  } catch (error) {
    console.error('加载启动脚本列表失败:', error)
  }
}

// 加载脚本版本列表
const loadScriptVersions = async (scriptId) => {
  try {
    const res = await request.get(`/service-start-script/${scriptId}/versions`)
    scriptVersions.value = res || []
  } catch (error) {
    console.error('加载脚本版本列表失败:', error)
  }
}

// 选择脚本
const handleSelectScript = async (scriptId) => {
  if (scriptId) {
    await loadScriptVersions(scriptId)
  } else {
    scriptVersions.value = []
  }
}

// 查看脚本详情
const handleViewScriptDetail = async () => {
  if (!selectedScriptId.value) {
    ElMessage.warning('请先选择脚本')
    return
  }
  try {
    const detail = await getScriptDetail(selectedScriptId.value)
    currentScriptDetail.value = detail
    scriptDetailVisible.value = true
  } catch (error) {
    ElMessage.error('加载脚本详情失败')
  }
}

// 查看脚本版本
const handleViewScriptVersion = (version) => {
  currentScriptDetail.value = {
    ...version,
    name: allScripts.value.find(s => s.id === selectedScriptId.value)?.name || ''
  }
  scriptDetailVisible.value = true
}

// 激活脚本版本
const handleActivateScriptVersion = async (version) => {
  try {
    await request.post(`/service-start-script/${selectedScriptId.value}/activate/${version.id}`)
    ElMessage.success('版本激活成功')
    await loadScriptVersions(selectedScriptId.value)
    await loadAllScripts()
  } catch (error) {
    ElMessage.error('激活版本失败')
  }
}

// 跳转到脚本管理页面（如果需要独立页面的话）
const handleGoToScriptManagement = () => {
  ElMessage.info('请在编辑服务时选择和配置启动脚本')
}

// 创建脚本
const handleCreateScript = async () => {
  try {
    // 获取默认脚本模板
    const defaultScript = await getDefaultScript()

    // 初始化脚本详情
    currentScriptDetail.value = {
      name: '新启动脚本',
      description: '',
      port: 8080,
      jvmArgs: '',
      preJavaCmd: '',
      postJavaCmd: '',
      isDefault: 0,
      scriptContent: defaultScript?.scriptContent || ''
    }

    scriptEditMode.value = true
    scriptDetailVisible.value = true
  } catch (error) {
    ElMessage.error('加载默认脚本模板失败')
  }
}

// 编辑脚本
const handleEditScript = () => {
  scriptEditMode.value = true
}

// 保存脚本（创建或更新）
const handleSaveScript = async () => {
  if (!currentScriptDetail.value?.name) {
    ElMessage.warning('请输入脚本名称')
    return
  }

  try {
    const data = {
      name: currentScriptDetail.value.name,
      description: currentScriptDetail.value.description || '',
      port: currentScriptDetail.value.port,
      jvmArgs: currentScriptDetail.value.jvmArgs || '',
      preJavaCmd: currentScriptDetail.value.preJavaCmd || '',
      postJavaCmd: currentScriptDetail.value.postJavaCmd || '',
      isDefault: currentScriptDetail.value.isDefault || 0
    }

    // 判断是创建还是更新
    if (currentScriptDetail.value.id) {
      // 更新现有脚本
      data.id = currentScriptDetail.value.id
      await updateScript(data)
      ElMessage.success('脚本更新成功')
    } else {
      // 创建新脚本
      await createScript(data)
      ElMessage.success('脚本创建成功')
    }

    // 关闭对话框并刷新列表
    scriptDetailVisible.value = false
    scriptEditMode.value = false
    await loadAllScripts()
  } catch (error) {
    ElMessage.error('保存脚本失败')
  }
}

// 更新服务的启动脚本配置
const updateServiceScript = async () => {
  if (!currentEditingService.value) return

  try {
    const service = {
      ...currentEditingService.value,
      startScriptId: selectedScriptId.value,
      startScriptVersionId: scriptVersions.value.find(v => v.isActive === 1)?.id || null
    }
    await request.put('/service', service)
    ElMessage.success('启动脚本配置已更新')
    fetchData()
  } catch (error) {
    ElMessage.error('更新启动脚本配置失败')
  }
}

// 添加配置文件
const handleAddConfigFile = () => {
  configEditForm.id = null
  configEditForm.filename = ''
  configEditForm.content = ''
  configEditForm.versionTag = ''
  configEditForm.versionDescription = ''
  configFileEditVisible.value = true
}

// 编辑配置文件
const handleEditConfigFile = (row) => {
  configEditForm.id = row.id
  configEditForm.filename = row.filename
  configEditForm.content = row.content
  configEditForm.versionTag = row.versionTag || ''
  configEditForm.versionDescription = ''
  configFileEditVisible.value = true
}

// 保存配置文件
const handleSaveConfigFile = async () => {
  if (!configEditForm.filename) {
    return ElMessage.warning('请输入文件名')
  }
  if (!configEditForm.content) {
    return ElMessage.warning('请输入配置内容')
  }

  try {
    const res = await saveServiceConfigFile(currentService.value.id, {
      id: configEditForm.id,
      filename: configEditForm.filename,
      content: configEditForm.content,
      versionTag: configEditForm.versionTag,
      versionDescription: configEditForm.versionDescription
    })

    if (res.success) {
      ElMessage.success('配置文件保存成功')
      configFileEditVisible.value = false
      // 重新加载配置文件列表
      openConfigFiles(currentService.value)
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    console.error('保存配置文件失败:', error)
    ElMessage.error('保存配置文件失败')
  }
}

// 删除配置文件
const handleDeleteConfigFile = async (row) => {
  ElMessageBox.confirm(`确认删除配置文件 "${row.filename}"？`, '删除确认', {
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteServiceConfigFile(currentService.value.id, row.id)
      if (res.success) {
        ElMessage.success('配置文件删除成功')
        // 重新加载配置文件列表
        openConfigFiles(currentService.value)
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除配置文件失败:', error)
      ElMessage.error('删除配置文件失败')
    }
  }).catch(() => {})
}

// 查看配置文件版本历史
const handleViewConfigHistory = async (row) => {
  currentConfigFile.value = row
  configFileVersions.value = []
  configFileHistoryVisible.value = true

  try {
    const versions = await getConfigFileVersions(currentService.value.id, row.filename)
    configFileVersions.value = versions || []
  } catch (error) {
    console.error('获取版本历史失败:', error)
    ElMessage.error('获取版本历史失败')
  }
}

// 查看版本内容
const handleViewVersionContent = (version) => {
  configEditForm.id = null
  configEditForm.filename = currentConfigFile.value?.filename || ''
  configEditForm.content = version.content
  configEditForm.versionTag = version.versionTag || ''
  configEditForm.versionDescription = version.versionDescription || ''
  configFileEditVisible.value = true
}

// 回退到配置文件的某个版本
const handleRollbackToConfigVersion = async (version) => {
  ElMessageBox.confirm(`确认回退到版本 "${version.versionTag || '未标记'}"？`, '回退确认', {
    type: 'warning'
  }).then(async () => {
    try {
      const res = await rollbackConfigFileVersion(currentService.value.id, version.id)
      if (res.success) {
        ElMessage.success('版本回退成功')
        configFileHistoryVisible.value = false
        // 重新加载配置文件列表
        openConfigFiles(currentService.value)
      } else {
        ElMessage.error(res.message || '回退失败')
      }
    } catch (error) {
      console.error('回退版本失败:', error)
      ElMessage.error('回退版本失败')
    }
  }).catch(() => {})
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
      ElMessage.error('回退请求失败：' + (err.message || '未知错误'))
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
# 服务端口：${port}
# 生成时间：${new Date().toLocaleString('zh-CN')}
# 使用方式：sh start.sh (无需传递参数)

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
log_info() => {
    echo -e "\${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] INFO: \$1\${NC}"
}

log_error() => {
    echo -e "\${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: \$1\${NC}"
}

log_warn() => {
    echo -e "\${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARN: \$1\${NC}"
}

# 检查 Java 是否可用
check_java() => {
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

# 获取应用 PID
get_pid() => {
    if [ -f "\$PID_FILE" ]; then
        cat "\$PID_FILE"
    fi
}

# 检查端口是否被占用
check_port() => {
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
            log_error "请手动执行：lsof -ti:\$port | xargs kill"
            exit 1
        fi

        log_info "端口 \$port 已释放"
    fi
}

# 优雅停止进程
graceful_stop() => {
    local pid=\$1
    local max_wait=30  # 最大等待 30 秒
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
        log_warn "如需强制停止，请执行：kill -9 \$pid"
        return 1
    else
        log_info "进程 \$pid 已优雅停止 (耗时：\${waited}秒)"
        return 0
    fi
}

# 检查应用是否已运行
check_running() => {
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
start() => {
    log_info "=========================================="
    log_info "Spring Boot 应用启动"
    log_info "=========================================="

    # 检查 Java
    check_java

    # 检查端口是否被占用
    log_info "检查端口 \$PORT 是否可用..."
    check_port \$PORT

    # 检查是否已运行
    check_running

    # 创建日志目录
    mkdir -p "\$LOG_DIR"

    # 查找 jar 文件
    jar_file=\$(ls -t \$JAR_NAME 2>/dev/null | head -1)
    if [ -z "\$jar_file" ]; then
        log_error "未找到 jar 文件：\$JAR_NAME"
        exit 1
    fi

    log_info "使用 jar 文件：\$jar_file"
    log_info "服务端口：\$PORT"
    log_info "日志文件：\$LOG_DIR/app.log"

    # 启动应用
    nohup java \$JAVA_OPTS -jar "\$jar_file" \\
        > "\$LOG_DIR/app.log" 2>&1 &
    echo \$! > "\$PID_FILE"

    # 等待启动
    sleep 3

    # 验证启动成功
    if ps -p \$(get_pid) > /dev/null 2>&1; then
        log_info "应用启动成功 (PID: \$(get_pid))"
        log_info "应用日志：\$LOG_DIR/app.log"
        log_info "=========================================="
        log_info "启动完成"
        log_info "=========================================="
    else
        log_error "应用启动失败，请检查日志：\$LOG_DIR/app.log"
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

  // 检查是否有服务 ID（新增模式下的服务还没有保存）
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
      ElMessage.error('上传失败：' + (uploadRes.message || '未知错误'))
    }
  } catch (e) {
    if (e.message !== 'cancel') {
      ElMessage.error('操作失败：' + (e.message || '未知错误'))
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
  loadStartScripts() // 加载启动脚本列表
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
.log-content :deep(>>>) {
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

/* 折叠面板标题样式 */
.collapse-title {
  display: flex;
  align-items: center;
  width: 100%;
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
