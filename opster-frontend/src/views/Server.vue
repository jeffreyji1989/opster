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
      <el-table-column label="操作" width="280">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'

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

onMounted(fetchData)
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
}
</style>
