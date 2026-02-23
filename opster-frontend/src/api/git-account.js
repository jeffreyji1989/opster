import request from './request'

/**
 * Git 账号管理 API
 */

/**
 * 条件查询 Git 账号列表
 * @param {Object} params - 查询参数
 * @param {string} params.accountName - 账号名称（支持模糊搜索）
 * @param {string} params.gitPlatform - Git 平台
 * @param {number} params.status - 状态
 * @returns {Promise} Git 账号列表
 */
export function getGitAccountList(params) {
  return request({
    url: '/git-account/list',
    method: 'get',
    params
  })
}

/**
 * 分页查询 Git 账号
 * @param {Object} params - 分页参数
 * @param {number} params.page - 页码（从 1 开始）
 * @param {number} params.size - 每页大小
 * @returns {Promise} 分页结果
 */
export function getGitAccountPage(params) {
  return request({
    url: '/git-account/page',
    method: 'get',
    params
  })
}

/**
 * 根据 ID 查询 Git 账号
 * @param {number} id - Git 账号 ID
 * @returns {Promise} Git 账号信息
 */
export function getGitAccountById(id) {
  return request({
    url: `/git-account/${id}`,
    method: 'get'
  })
}

/**
 * 新增 Git 账号
 * @param {Object} data - Git 账号信息
 * @returns {Promise} 是否成功
 */
export function createGitAccount(data) {
  return request({
    url: '/git-account',
    method: 'post',
    data
  })
}

/**
 * 修改 Git 账号
 * @param {Object} data - Git 账号信息
 * @returns {Promise} 是否成功
 */
export function updateGitAccount(data) {
  return request({
    url: '/git-account',
    method: 'put',
    data
  })
}

/**
 * 删除 Git 账号
 * @param {number} id - Git 账号 ID
 * @returns {Promise} 是否成功
 */
export function deleteGitAccount(id) {
  return request({
    url: `/git-account/${id}`,
    method: 'delete'
  })
}

/**
 * 获取所有支持的 Git 平台列表
 * @returns {Promise} 平台列表
 */
export function getSupportedPlatforms() {
  return request({
    url: '/git-account/platforms',
    method: 'get'
  })
}

/**
 * 获取 Git 账号统计信息
 * @returns {Promise} 统计信息
 */
export function getGitAccountStats() {
  return request({
    url: '/git-account/stats',
    method: 'get'
  })
}
