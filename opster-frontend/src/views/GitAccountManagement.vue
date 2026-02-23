<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增 Git 账号</el-button>
      <el-form :inline="true" :model="queryForm" style="margin-left: 20px;">
        <el-form-item label="账号名称">
          <el-input v-model="queryForm.accountName" placeholder="请输入账号名称" style="width: 150px;" />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="queryForm.gitPlatform" placeholder="全部" style="width: 120px;">
            <el-option label="全部" value="" />
            <el-option label="Gitee" value="gitee" />
            <el-option label="GitLab" value="gitlab" />
            <el-option label="GitHub" value="github" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" style="width: 100px;">
            <el-option label="全部" value="" />
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
      <el-table-column prop="accountName" label="账号名称" width="180" />
      <el-table-column prop="gitPlatform" label="Git 平台" width="120">
        <template #default="scope">
          <el-tag :type="getPlatformType(scope.row.gitPlatform)">
            {{ getPlatformLabel(scope.row.gitPlatform) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="gitUsername" label="Git 用户名" width="180" />
      <el-table-column prop="authType" label="认证方式" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.authType === 0 ? 'success' : 'warning'">
            {{ scope.row.authType === 0 ? 'HTTPS' : 'SSH' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleToggleStatus(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑 Git 账号' : '新增 Git 账号'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="140px">
        <el-form-item label="账号名称" prop="accountName">
          <el-input v-model="form.accountName" placeholder="请输入账号名称" />
        </el-form-item>
        <el-form-item label="Git 平台" prop="gitPlatform">
          <el-select v-model="form.gitPlatform" placeholder="请选择 Git 平台">
            <el-option label="Gitee" value="gitee" />
            <el-option label="GitLab" value="gitlab" />
            <el-option label="GitHub" value="github" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证方式" prop="authType">
          <el-radio-group v-model="form.authType">
            <el-radio :value="0">HTTPS</el-radio>
            <el-radio :value="1">SSH</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- HTTPS 认证 -->
        <template v-if="form.authType === 0">
          <el-form-item label="Git 用户名" prop="gitUsername">
            <el-input v-model="form.gitUsername" placeholder="请输入 Git 用户名" />
          </el-form-item>
          <el-form-item label="Git 密码" prop="gitPassword">
            <el-input
              v-model="form.gitPassword"
              type="password"
              show-password
              placeholder="请输入 Git 密码"
            />
          </el-form-item>
        </template>

        <!-- SSH 认证 -->
        <template v-if="form.authType === 1">
          <el-form-item label="SSH 私钥路径" prop="sshKeyPath">
            <el-input v-model="form.sshKeyPath" placeholder="例如: ~/.ssh/id_rsa" />
          </el-form-item>
          <el-form-item label="SSH 私钥密码">
            <el-input
              v-model="form.sshKeyPassphrase"
              type="password"
              show-password
              placeholder="如果私钥有密码，请输入（可选）"
            />
          </el-form-item>
        </template>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as gitAccountApi from '../api/git-account'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

// 查询表单
const queryForm = reactive({
  accountName: '',
  gitPlatform: '',
  status: ''
})

// 编辑表单
const form = reactive({
  id: null,
  accountName: '',
  gitPlatform: 'gitee',
  description: '',
  gitUsername: '',
  gitPassword: '',
  authType: 0,
  sshKeyPath: '',
  sshKeyPassphrase: '',
  status: 1
})

// 表单验证规则
const rules = {
  accountName: [
    { required: true, message: '请输入账号名称', trigger: 'blur' }
  ],
  gitPlatform: [
    { required: true, message: '请选择 Git 平台', trigger: 'change' }
  ],
  authType: [
    { required: true, message: '请选择认证方式', trigger: 'change' }
  ],
  gitUsername: [
    { required: true, message: '请输入 Git 用户名', trigger: 'blur' }
  ],
  gitPassword: [
    { required: true, message: '请输入 Git 密码', trigger: 'blur' }
  ],
  sshKeyPath: [
    { required: true, message: '请输入 SSH 私钥路径', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 获取平台标签
const getPlatformLabel = (platform) => {
  const map = {
    gitee: 'Gitee',
    gitlab: 'GitLab',
    github: 'GitHub'
  }
  return map[platform] || platform
}

// 获取平台类型
const getPlatformType = (platform) => {
  const map = {
    gitee: 'danger',
    gitlab: 'primary',
    github: 'success'
  }
  return map[platform] || ''
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {}
    if (queryForm.accountName) params.accountName = queryForm.accountName
    if (queryForm.gitPlatform) params.gitPlatform = queryForm.gitPlatform
    if (queryForm.status !== '') params.status = queryForm.status

    const res = await gitAccountApi.getGitAccountList(params)
    tableData.value = res.map(item => {
      // 确保状态值是数字类型
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
  } catch (error) {
    console.error('获取 Git 账号列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => {
  Object.assign(form, {
    id: null,
    accountName: '',
    gitPlatform: 'gitee',
    description: '',
    gitUsername: '',
    gitPassword: '',
    authType: 0,
    sshKeyPath: '',
    sshKeyPassphrase: '',
    status: 1
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  Object.assign(form, {
    ...row,
    password: '' // 密码不回显
  })
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该 Git 账号吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await gitAccountApi.deleteGitAccount(row.id)
    ElMessage.success('删除成功')
    await fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 切换状态
const handleToggleStatus = async (row) => {
  try {
    await gitAccountApi.updateGitAccount(row)
    ElMessage.success('状态更新成功')
  } catch (error) {
    console.error('状态更新失败:', error)
    // 更新失败，恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    // HTTPS 认证方式需要验证用户名和密码
    if (form.authType === 0 && (!form.gitUsername || !form.gitPassword)) {
      ElMessage.error('HTTPS 认证方式需要填写用户名和密码')
      return
    }

    // SSH 认证方式需要验证私钥路径
    if (form.authType === 1 && !form.sshKeyPath) {
      ElMessage.error('SSH 认证方式需要填写私钥路径')
      return
    }

    if (form.id) {
      await gitAccountApi.updateGitAccount(form)
      ElMessage.success('修改成功')
    } else {
      await gitAccountApi.createGitAccount(form)
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    await fetchData()
  } catch (error) {
    console.error('提交失败:', error)
  }
}

// 查询
const handleSearch = () => {
  fetchData()
}

// 重置
const handleReset = () => {
  Object.assign(queryForm, {
    accountName: '',
    gitPlatform: '',
    status: ''
  })
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}
</style>
