import request from './request'

/**
 * 获取所有启动脚本列表
 */
export function listScripts() {
  return request.get('/service-start-script/list')
}

/**
 * 获取脚本详情
 */
export function getScriptDetail(scriptId) {
  return request.get(`/service-start-script/${scriptId}`)
}

/**
 * 创建脚本
 */
export function createScript(data) {
  return request.post('/service-start-script/create', data)
}

/**
 * 更新脚本
 */
export function updateScript(data) {
  return request.post('/service-start-script/update', data)
}

/**
 * 获取默认脚本模板
 */
export function getDefaultScript() {
  return request.get('/service-start-script/default')
}
