<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增项目</el-button>
      <el-form :inline="true" :model="queryForm" style="margin-left: 20px;">
        <el-form-item label="项目名称">
          <el-input v-model="queryForm.projectName" placeholder="请输入项目名称" style="width: 150px;" />
        </el-form-item>
        <el-form-item label="业务线">
          <el-input v-model="queryForm.businessLine" placeholder="请输入业务线" style="width: 150px;" />
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
      <el-table-column prop="projectName" label="项目名称" />
      <el-table-column prop="projectOwner" label="负责人" />
      <el-table-column prop="gitUrl" label="Git地址" show-overflow-tooltip />
      <el-table-column prop="businessLine" label="业务线" />
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑项目' : '新增项目'">
      <el-form :model="form" label-width="100px">
        <el-form-item label="项目名称">
          <el-input v-model="form.projectName" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.projectOwner" />
        </el-form-item>
        <el-form-item label="Git地址">
          <el-input v-model="form.gitUrl" />
        </el-form-item>
        <el-form-item label="监控地址">
          <el-input v-model="form.monitorUrl" />
        </el-form-item>
        <el-form-item label="业务线">
          <el-input v-model="form.businessLine" />
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
  projectName: '',
  projectOwner: '',
  gitUrl: '',
  monitorUrl: '',
  businessLine: '',
  status: 1
})

const queryForm = reactive({
  projectName: '',
  businessLine: '',
  status: ''
})

const fetchData = async () => {
  loading.value = true
  try {
    const params = {}
    if (queryForm.projectName) params.projectName = queryForm.projectName
    if (queryForm.businessLine) params.businessLine = queryForm.businessLine
    if (queryForm.status !== '') params.status = queryForm.status
    
    const res = await request.get('/project/list', { params })
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
  form.id = null
  form.projectName = ''
  form.projectOwner = ''
  form.gitUrl = ''
  form.monitorUrl = ''
  form.businessLine = ''
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.id) {
      await request.put('/project', form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/project', form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    // handled in interceptor
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该项目?', '警告', {
    type: 'warning'
  }).then(async () => {
    await request.delete(`/project/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  })
}

const handleToggleStatus = async (row) => {
  try {
    // 确保发送的是数字类型的状态值
    const updatedRow = {
      ...row,
      status: row.status === 1 ? 1 : 0
    }
    await request.put('/project', updatedRow)
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
    projectName: '',
    businessLine: '',
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
