import { test as base, expect } from '@playwright/test'

/**
 * 测试夹具（Fixtures）
 */

// 扩展测试 fixtures
export const test = base.extend<{
  // 空 fixtures，用于 future 扩展
}>({})

// 重新导出 expect
export { expect }

/**
 * 认证相关的 fixtures
 */
export const authFixture = {
  /**
   * 执行登录操作
   * @param page Playwright Page 实例
   */
  async login(page: any) {
    // 如果系统不需要登录，此方法可以留空
    // 如果需要登录，可以在这里实现登录逻辑
    await page.goto('/')
    // 使用 domcontentloaded 避免 Firefox 浏览器 networkidle 超时问题
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(500)
  }
}

/**
 * 测试数据工厂
 */
export const testdata = {
  /**
   * 生成随机的项目名称
   */
  generateProjectName() {
    return `TestProject_${Date.now()}`
  },

  /**
   * 生成随机的服务名称
   */
  generateServiceName() {
    return `TestService_${Date.now()}`
  },

  /**
   * 生成随机的服务器 IP
   */
  generateServerIp() {
    return `192.168.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`
  },

  /**
   * 生成随机的 Git 仓库地址
   */
  generateGitUrl() {
    const repos = ['eip', 'opster', 'platform', 'gateway', 'auth-service']
    const repo = repos[Math.floor(Math.random() * repos.length)]
    return `git@github.com:example/${repo}.git`
  },

  /**
   * 服务类型
   */
  serviceTypes: {
    FRONTEND: 0,
    BACKEND: 1,
    ADMIN: 2,
    MOBILE: 3
  },

  /**
   * 运行状态
   */
  runStatus: {
    NOT_STARTED: 0,
    NORMAL: 1,
    ABNORMAL: 2,
    DEPLOYING: 3,
    DEPLOY_FAILED: 4
  },

  /**
   * 发版状态
   */
  deploymentStatus: {
    IN_PROGRESS: 0,
    SUCCESS: 1,
    FAILED: 2
  }
}

/**
 * 常用的断言辅助函数
 */
export const assertions = {
  /**
   * 断言元素可见
   */
  async toBeVisible(locator: any, timeout = 5000) {
    await locator.waitFor({ state: 'visible', timeout })
  },

  /**
   * 断言元素隐藏
   */
  async toBeHidden(locator: any, timeout = 5000) {
    await locator.waitFor({ state: 'hidden', timeout })
  },

  /**
   * 断言元素存在
   */
  async toBeAttached(locator: any, timeout = 5000) {
    await locator.waitFor({ state: 'attached', timeout })
  },

  /**
   * 断言表格有指定行数
   */
  async toHaveRowCount(tableLocator: any, count: number, timeout = 5000) {
    await tableLocator.locator('.el-table__row').waitFor({ state: 'attached', timeout })
    const actualCount = await tableLocator.locator('.el-table__row').count()
    expect(actualCount).toBeGreaterThanOrEqual(count)
  },

  /**
   * 断言弹窗可见
   */
  async dialogToBeVisible(page: any, dialogText: string) {
    const dialog = page.locator(`.el-dialog:has-text("${dialogText}")`)
    await dialog.waitFor({ state: 'visible' })
  },

  /**
   * 断言消息提示
   */
  async toHaveMessage(page: any, messageText: string, type: 'success' | 'error' | 'warning' = 'success') {
    const message = page.locator(`.el-message--${type}`)
    await message.waitFor({ state: 'visible' })
    await expect(message).toContainText(messageText)
  }
}
