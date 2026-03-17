import request from './request'

/**
 * 服务配置文件管理 API
 */

/**
 * 获取服务配置文件列表
 * @param {number} serviceId - 服务 ID
 * @returns {Promise} 配置文件列表
 */
export function getServiceConfigFiles(serviceId) {
  return request({
    url: `/service/${serviceId}/config-files`,
    method: 'get'
  })
}

/**
 * 保存配置文件
 * @param {number} serviceId - 服务 ID
 * @param {Object} data - 配置文件信息
 * @returns {Promise} 保存结果
 */
export function saveServiceConfigFile(serviceId, data) {
  return request({
    url: `/service/${serviceId}/config-files`,
    method: 'post',
    data
  })
}

/**
 * 删除配置文件
 * @param {number} serviceId - 服务 ID
 * @param {number} configId - 配置文件 ID
 * @returns {Promise} 删除结果
 */
export function deleteServiceConfigFile(serviceId, configId) {
  return request({
    url: `/service/${serviceId}/config-files/${configId}`,
    method: 'delete'
  })
}

/**
 * 获取配置文件版本历史
 * @param {number} serviceId - 服务 ID
 * @param {string} filename - 文件名
 * @returns {Promise} 版本历史列表
 */
export function getConfigFileVersions(serviceId, filename) {
  return request({
    url: `/service/${serviceId}/config-files/${filename}/versions`,
    method: 'get'
  })
}

/**
 * 回退到历史版本
 * @param {number} serviceId - 服务 ID
 * @param {number} versionId - 版本 ID
 * @returns {Promise} 回退结果
 */
export function rollbackConfigFileVersion(serviceId, versionId) {
  return request({
    url: `/service/${serviceId}/config-versions/${versionId}/rollback`,
    method: 'post'
  })
}

/**
 * 从 Git 同步配置文件
 * @param {number} serviceId - 服务 ID
 * @returns {Promise} 同步结果
 */
export function syncConfigFromGit(serviceId) {
  return request({
    url: `/service/${serviceId}/config-files/sync-git`,
    method: 'post'
  })
}
