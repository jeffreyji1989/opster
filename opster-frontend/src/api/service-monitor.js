import request from './request'

/**
 * 获取服务监控统计列表（包含当前和上次状态）
 */
export function getMonitorStatistics() {
  return request.get('/service/monitor/statistics')
}

/**
 * 获取服务监控列表
 */
export function getMonitorList() {
  return request.get('/service/monitor/list')
}

/**
 * 获取指定服务的最新监控记录
 */
export function getLatestRecord(serviceId) {
  return request.get(`/service/monitor/${serviceId}/latest`)
}

/**
 * 获取指定服务的监控历史记录
 */
export function getMonitorHistory(serviceId, days = 7) {
  return request.get(`/service/monitor/${serviceId}/history`, { params: { days } })
}

/**
 * 手动触发指定服务的监控检查
 */
export function manualCheck(serviceId) {
  return request.post(`/service/monitor/${serviceId}/check`)
}

/**
 * 手动触发所有服务的监控检查
 */
export function checkAll() {
  return request.post('/service/monitor/check-all')
}

/**
 * 清理历史数据
 */
export function cleanupHistory(days = 7) {
  return request.post('/service/monitor/cleanup', { params: { days } })
}

/**
 * 获取所有可用于监控的服务列表
 */
export function getAvailableServices() {
  return request.get('/service/monitor/available-services')
}

/**
 * 更新服务的监控URL
 */
export function updateMonitorUrl(serviceId, monitorUrl) {
  return request.put(`/service/monitor/${serviceId}/monitor-url`, { monitorUrl })
}