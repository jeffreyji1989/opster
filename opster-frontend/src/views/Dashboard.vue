<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>项目总数</span>
            </div>
          </template>
          <div class="card-value">{{ stats.projectCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>服务器总数</span>
            </div>
          </template>
          <div class="card-value">{{ stats.serverCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>服务总数</span>
            </div>
          </template>
          <div class="card-value">{{ stats.serviceCount || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="chart-section" style="margin-top: 20px;">
      <el-card header="服务状态分布">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="status-box normal">
              <div class="label">正常运行</div>
              <div class="value">{{ stats.statusNormal || 0 }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="status-box error">
              <div class="label">异常状态</div>
              <div class="value">{{ stats.statusError || 0 }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="status-box unknown">
              <div class="label">未启动</div>
              <div class="value">{{ stats.statusUnknown || 0 }}</div>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../api/request'

const stats = ref({})

const fetchStats = async () => {
  try {
    const res = await request.get('/dashboard/stats')
    stats.value = res
  } catch (e) {
    console.error(e)
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.card-header {
  font-weight: bold;
}
.card-value {
  font-size: 28px;
  text-align: center;
  color: #409EFF;
  padding: 10px 0;
}
.status-box {
  text-align: center;
  padding: 20px;
  border-radius: 8px;
  color: white;
}
.status-box .label {
  font-size: 14px;
  opacity: 0.9;
}
.status-box .value {
  font-size: 24px;
  font-weight: bold;
  margin-top: 5px;
}
.status-box.normal {
  background-color: #67C23A;
}
.status-box.error {
  background-color: #F56C6C;
}
.status-box.unknown {
  background-color: #909399;
}
</style>
