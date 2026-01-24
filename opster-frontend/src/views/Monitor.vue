<template>
  <div class="monitor-container">
    <div class="sidebar">
      <div class="sidebar-header">
        <el-input v-model="searchText" placeholder="搜索服务器IP/别名" prefix-icon="Search" clearable />
      </div>
      <div class="server-list">
        <div 
          v-for="server in filteredServers" 
          :key="server.id" 
          class="server-item"
          :class="{ active: currentServerId === server.id }"
          @click="selectServer(server)"
        >
          <div class="server-name">{{ server.alias || server.ip }}</div>
          <div class="server-ip">{{ server.ip }}</div>
          <el-tag size="small" :type="server.status === 1 ? 'success' : 'info'">
            {{ server.status === 1 ? '在线' : '离线' }}
          </el-tag>
        </div>
      </div>
    </div>

    <div class="main-content">
      <div v-if="!currentServerId" class="empty-state">
        <el-empty description="请选择一台服务器开始监控" />
      </div>
      
      <div v-else class="dashboard">
        <div class="header">
          <h2>{{ currentServer?.alias || currentServer?.ip }} 监控面板</h2>
          <div class="status-badge" :class="{ active: connected }">
            {{ connected ? '实时监控中' : '连接断开' }}
          </div>
        </div>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-card class="chart-card">
              <template #header>CPU 使用率</template>
              <div ref="cpuChartRef" class="chart-container"></div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="chart-card">
              <template #header>内存使用情况</template>
              <div ref="memChartRef" class="chart-container"></div>
              <div class="mem-info" v-if="metric">
                {{ formatSize(metric.memoryUsed) }} / {{ formatSize(metric.memoryTotal) }} ({{ metric.memoryUsage }}%)
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="chart-card">
              <template #header>磁盘使用情况</template>
              <div class="disk-list" v-if="metric && metric.diskInfos">
                <div v-for="(disk, index) in metric.diskInfos" :key="index" class="disk-item">
                  <div class="disk-label">
                    <span>{{ disk.mountedOn }}</span>
                    <span>{{ disk.usePercent }}</span>
                  </div>
                  <el-progress 
                    :percentage="parseInt(disk.usePercent)" 
                    :status="getDiskStatus(disk.usePercent)"
                  />
                  <div class="disk-detail">{{ disk.used }} / {{ disk.size }}</div>
                </div>
              </div>
              <div v-else class="no-data">暂无数据</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- Placeholder for History (Future) -->
        <!-- <el-row class="mt-20">
          <el-col :span="24">
            <el-card>
              <template #header>历史趋势 (近1小时)</template>
              <div style="height: 300px; display: flex; align-items: center; justify-content: center; color: #999;">
                历史数据功能开发中...
              </div>
            </el-card>
          </el-col>
        </el-row> -->
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import request from '../api/request'
import * as echarts from 'echarts'

// Data
const servers = ref([])
const searchText = ref('')
const currentServerId = ref(null)
const currentServer = ref(null)
const metric = ref(null)
const connected = ref(false)
let ws = null
let cpuChart = null
let memChart = null

// Computed
const filteredServers = computed(() => {
  if (!searchText.value) return servers.value
  const lowText = searchText.value.toLowerCase()
  return servers.value.filter(s => 
    s.ip.includes(lowText) || (s.alias && s.alias.toLowerCase().includes(lowText))
  )
})

// Formatting
const formatSize = (mb) => {
  if (!mb) return '0 MB'
  if (mb > 1024) return (mb / 1024).toFixed(2) + ' GB'
  return mb + ' MB'
}

const getDiskStatus = (percentStr) => {
  const p = parseInt(percentStr)
  if (p > 90) return 'exception'
  if (p > 75) return 'warning'
  return 'success'
}

// Actions
const fetchServers = async () => {
  try {
    const res = await request.get('/server/list')
    servers.value = res
  } catch (e) {
    console.error(e)
  }
}

const selectServer = (server) => {
  if (currentServerId.value === server.id) return
  currentServerId.value = server.id
  currentServer.value = server
  metric.value = null // clear old data
  startMonitoring(server.id)
  
  nextTick(() => {
    initCharts()
  })
}

const startMonitoring = (serverId) => {
  if (ws) {
    ws.close()
  }

  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const host = window.location.host
  // Note: Backend is on 8080, Frontend 5173. Assuming proxy or direct call.
  // Using direct port 8080 for dev if proxy not set up for WS, but let's try relative first if proxy exists.
  // If Vite proxy handles /ws, good.
  
  // Hardcoding 8080 for WS in dev environment usually required if vite proxy doesn't upgrade WS
  // Let's assume typical setup:
  const wsUrl = `ws://localhost:8080/ws/monitor`
  
  ws = new WebSocket(wsUrl)
  
  ws.onopen = () => {
    connected.value = true
    ws.send(JSON.stringify({ serverId: String(serverId) }))
  }
  
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.error) {
        console.error(data.error)
      } else {
        metric.value = data
        updateCharts(data)
      }
    } catch (e) {
      console.error('Parse error', e)
    }
  }
  
  ws.onclose = () => {
    connected.value = false
  }
  
  ws.onerror = (e) => {
    console.error('WS Error', e)
    connected.value = false
  }
}

// Charts
const cpuChartRef = ref(null)
const memChartRef = ref(null)

const initCharts = () => {
  if (cpuChartRef.value) {
    cpuChart = echarts.init(cpuChartRef.value)
    cpuChart.setOption({
      series: [{
        type: 'gauge',
        startAngle: 180,
        endAngle: 0,
        min: 0,
        max: 100,
        splitNumber: 5,
        itemStyle: { color: '#58D68D' },
        progress: { show: true, width: 18 },
        pointer: { show: false },
        axisLine: { lineStyle: { width: 18 } },
        axisTick: { show: false },
        splitLine: { length: 15, lineStyle: { width: 2, color: '#999' } },
        axisLabel: { distance: 25, color: '#999', fontSize: 14 },
        anchor: { show: false },
        title: { show: false },
        detail: {
          valueAnimation: true,
          fontSize: 30,
          offsetCenter: [0, '30%'],
          formatter: '{value}%'
        },
        data: [{ value: 0 }]
      }]
    })
  }
  
  if (memChartRef.value) {
    memChart = echarts.init(memChartRef.value)
    memChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { top: '5%', left: 'center' },
      series: [{
        name: 'Memory',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
        labelLine: { show: false },
        data: [
          { value: 0, name: 'Used' },
          { value: 0, name: 'Free' }
        ]
      }]
    })
  }
}

const updateCharts = (data) => {
  if (cpuChart) {
    cpuChart.setOption({
      series: [{
        data: [{ value: data.cpuUsage }]
      }]
    })
  }
  
  if (memChart && data.memoryTotal) {
    memChart.setOption({
      series: [{
        data: [
          { value: data.memoryUsed, name: 'Used', itemStyle: { color: '#E6A23C' } },
          { value: data.memoryTotal - data.memoryUsed, name: 'Free', itemStyle: { color: '#67C23A' } }
        ]
      }]
    })
  }
}

onMounted(() => {
  fetchServers()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (ws) ws.close()
  window.removeEventListener('resize', handleResize)
  if (cpuChart) cpuChart.dispose()
  if (memChart) memChart.dispose()
})

const handleResize = () => {
  if (cpuChart) cpuChart.resize()
  if (memChart) memChart.resize()
}

</script>

<style scoped>
.monitor-container {
  display: flex;
  height: calc(100vh - 84px); /* Adjust based on layout header */
  background: #f5f7fa;
}

.sidebar {
  width: 280px;
  background: #fff;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 15px;
  border-bottom: 1px solid #eee;
}

.server-list {
  flex: 1;
  overflow-y: auto;
}

.server-item {
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.server-item:hover {
  background: #f9f9f9;
}

.server-item.active {
  background: #ecf5ff;
  border-right: 3px solid #409eff;
}

.server-name {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 5px;
}

.server-ip {
  color: #666;
  font-size: 12px;
  margin-bottom: 5px;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.status-badge {
  padding: 5px 10px;
  border-radius: 12px;
  background: #f56c6c;
  color: white;
  font-size: 12px;
}

.status-badge.active {
  background: #67c23a;
}

.chart-card {
  height: 320px;
  display: flex;
  flex-direction: column;
}

.chart-container {
  height: 220px;
  width: 100%;
}

.mem-info {
  text-align: center;
  font-size: 14px;
  color: #666;
  margin-top: 10px;
}

.disk-list {
  height: 220px;
  overflow-y: auto;
  padding-right: 10px;
}

.disk-item {
  margin-bottom: 15px;
}

.disk-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 4px;
}

.disk-detail {
  font-size: 11px;
  color: #999;
  text-align: right;
  margin-top: 2px;
}

.no-data {
  text-align: center;
  color: #999;
  line-height: 200px;
}

.mt-20 {
  margin-top: 20px;
}
</style>