<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增服务器</el-button>
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
          <el-switch v-model="scope.row.status" active-value="1" inactive-value="0" @change="handleToggleStatus(scope.row)" />
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
      <el-form :model="form" label-width="100px">
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
  status: 1
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/server/list')
    tableData.value = res
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

onMounted(fetchData)
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
}
</style>
