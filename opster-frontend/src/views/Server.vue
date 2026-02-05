<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增服务器</el-button>
      <el-form :inline="true" :model="queryForm" style="margin-left: 20px;">
        <el-form-item label="IP">
          <el-input v-model="queryForm.ip" placeholder="请输入IP地址" style="width: 150px;" />
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
      <el-table-column prop="ip" label="IP地址" />
      <el-table-column prop="groupName" label="分组" />
      <el-table-column prop="env" label="环境" />
      <el-table-column prop="deployedCount" label="部署数" width="80" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="handleToggleStatus(scope.row)" />
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑服务器' : '新增服务器'">
      <el-form :model="form" label-width="140px">
        <el-form-item label="别名">
          <el-input v-model="form.alias" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="form.ip" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
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
          <el-input v-model="form.javaHome" placeholder="例如: /usr/lib/jvm/java-17-openjdk（可选）" />
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            该服务器上的 Java 安装路径，不填则使用系统默认配置或自动检测
          </div>
        </el-form-item>
        <el-form-item label="MAVEN_HOME">
          <el-input v-model="form.mavenHome" placeholder="例如: /usr/share/maven（可选）" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const form = reactive({
  id: null,
  alias: '',
  ip: '',
  username: '',
  password: '',
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
    groupName: '',
    env: 'test',
    javaHome: '',
    mavenHome: '',
    status: 1
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
  ElMessageBox.confirm('确认删除该服务器?', '警告', {
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
