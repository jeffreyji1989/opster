import request from './request'

/**
 * 子项目管理 API
 */

/**
 * 条件查询子项目列表
 * @param {Object} params - 查询参数
 * @param {number} params.projectId - 项目 ID
 * @param {number} params.serverId - 服务器 ID
 * @param {string} params.projectType - 项目类型
 * @param {number} params.status - 状态
 * @returns {Promise} 子项目列表
 */
export function getSubProjectList(params) {
  return request({
    url: '/sub-project/list',
    method: 'get',
    params
  })
}

/**
 * 分页查询子项目
 * @param {Object} params - 分页参数
 * @param {number} params.page - 页码（从 1 开始）
 * @param {number} params.size - 每页大小
 * @returns {Promise} 分页结果
 */
export function getSubProjectPage(params) {
  return request({
    url: '/sub-project/page',
    method: 'get',
    params
  })
}

/**
 * 根据 ID 查询子项目
 * @param {number} id - 子项目 ID
 * @returns {Promise} 子项目信息
 */
export function getSubProjectById(id) {
  return request({
    url: `/sub-project/${id}`,
    method: 'get'
  })
}

/**
 * 根据项目 ID 查询子项目列表
 * @param {number} projectId - 项目 ID
 * @returns {Promise} 子项目列表
 */
export function getSubProjectsByProjectId(projectId) {
  return request({
    url: `/sub-project/project/${projectId}`,
    method: 'get'
  })
}

/**
 * 新增子项目
 * @param {Object} data - 子项目信息
 * @returns {Promise} 是否成功
 */
export function createSubProject(data) {
  return request({
    url: '/sub-project',
    method: 'post',
    data
  })
}

/**
 * 修改子项目
 * @param {Object} data - 子项目信息
 * @returns {Promise} 是否成功
 */
export function updateSubProject(data) {
  return request({
    url: '/sub-project',
    method: 'put',
    data
  })
}

/**
 * 删除子项目
 * @param {number} id - 子项目 ID
 * @returns {Promise} 是否成功
 */
export function deleteSubProject(id) {
  return request({
    url: `/sub-project/${id}`,
    method: 'delete'
  })
}

/**
 * 触发子项目部署
 * @param {number} id - 子项目 ID
 * @returns {Promise} 部署结果
 */
export function deploySubProject(id) {
  return request({
    url: `/sub-project/${id}/deploy`,
    method: 'post'
  })
}

/**
 * 获取子项目的配置文件
 * @param {number} id - 子项目 ID
 * @returns {Promise} 配置文件 JSON 字符串
 */
export function getSubProjectConfigFiles(id) {
  return request({
    url: `/sub-project/${id}/config-files`,
    method: 'get'
  })
}

/**
 * 保存子项目的配置文件
 * @param {number} id - 子项目 ID
 * @param {string} configFiles - 配置文件 JSON 字符串
 * @returns {Promise} 是否成功
 */
export function saveSubProjectConfigFiles(id, configFiles) {
  return request({
    url: `/sub-project/${id}/config-files`,
    method: 'post',
    data: { configFiles }
  })
}

/**
 * 统计项目下的子项目数量
 * @param {number} projectId - 项目 ID
 * @returns {Promise} 统计信息
 */
export function countSubProjectsByProjectId(projectId) {
  return request({
    url: `/sub-project/count/project/${projectId}`,
    method: 'get'
  })
}
