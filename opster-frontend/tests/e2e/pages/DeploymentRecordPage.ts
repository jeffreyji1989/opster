import { Page, Locator } from '@playwright/test'

/**
 * 发版记录管理页面 Page Object
 */
export class DeploymentRecordPage {
  readonly page: Page
  readonly pageHeader: Locator
  readonly searchInputs: {
    projectName: Locator
    serviceName: Locator
    serverIp: Locator
    status: Locator
  }
  readonly searchButton: Locator
  readonly resetButton: Locator
  readonly batchDeleteButton: Locator
  readonly table: Locator
  readonly tableRows: Locator
  readonly logDialog: Locator
  readonly logContent: Locator

  constructor(page: Page) {
    this.page = page
    this.pageHeader = page.locator('.card-header span')

    // 搜索栏 - 使用 .el-form-item 定位，在 Firefox 上更稳定
    this.searchInputs = {
      projectName: page.locator('.el-form-item:has-text("项目名称") input'),
      serviceName: page.locator('.el-form-item:has-text("服务名称") input'),
      // 服务器 IP 搜索框可能在小视口下被折叠，使用 el-form-item 定位
      serverIp: page.locator('.el-form-item:has-text("服务器 IP") input'),
      // 发版状态下拉框 - 使用 .el-select 定位，避免 placeholder 遮挡
      status: page.locator('.el-form-item:has-text("发版状态") .el-select')
    }

    this.searchButton = page.getByRole('button', { name: '搜索' })
    this.resetButton = page.getByRole('button', { name: '重置' })
    this.batchDeleteButton = page.getByRole('button', { name: /批量删除/ })

    // 表格
    this.table = page.locator('.el-table')
    this.tableRows = page.locator('.el-table__row')

    // 日志弹窗
    this.logDialog = page.locator('.el-dialog:has-text("发版日志")')
    this.logContent = page.locator('.log-content')
  }

  /**
   * 导航到发版记录页面
   */
  async goto() {
    await this.page.goto('/deployment-record')
    // 使用 domcontentloaded 避免 Firefox 浏览器 networkidle 超时问题
    await this.page.waitForLoadState('domcontentloaded')
    // 等待页面主要元素可见，增加超时时间
    await this.page.waitForTimeout(2000)
  }

  /**
   * 搜索发版记录
   */
  async search(options: {
    projectName?: string
    serviceName?: string
    serverIp?: string
    status?: string
  }) {
    if (options.projectName) {
      await this.searchInputs.projectName.fill(options.projectName)
    }
    if (options.serviceName) {
      await this.searchInputs.serviceName.fill(options.serviceName)
    }
    if (options.serverIp) {
      // 服务器 IP 搜索框可能被折叠，先检查是否存在且可见
      try {
        const serverIpVisible = await this.searchInputs.serverIp.isVisible({ timeout: 2000 })
        if (serverIpVisible) {
          await this.searchInputs.serverIp.fill(options.serverIp)
        } else {
          console.log('服务器 IP 搜索框不可见，跳过填充')
        }
      } catch (e) {
        // 元素不存在于 DOM 中
        console.log('服务器 IP 搜索框不存在于 DOM 中，跳过填充')
      }
    }
    if (options.status) {
      // 等待下拉框可点击，增加超时时间
      await this.searchInputs.status.waitFor({ state: 'visible', timeout: 15000 })
      await this.searchInputs.status.click()
      // 等待下拉菜单展开
      await this.page.waitForTimeout(500)
      await this.page.locator('.el-select-dropdown__item span:has-text("' + options.status + '")').first().click()
      // 等待下拉菜单关闭
      await this.page.waitForTimeout(300)
    }
    // 直接点击搜索按钮，Playwright 会自动等待元素可交互
    await this.searchButton.click()
    // 使用 domcontentloaded 避免 Firefox 浏览器 networkidle 超时问题
    await this.page.waitForLoadState('domcontentloaded')
    await this.page.waitForTimeout(500)
  }

  /**
   * 重置搜索表单
   */
  async resetSearch() {
    await this.resetButton.click()
    // 使用 domcontentloaded 避免 Firefox 浏览器 networkidle 超时问题
    await this.page.waitForLoadState('domcontentloaded')
    await this.page.waitForTimeout(500)
  }

  /**
   * 获取表格行数
   */
  async getRowCount(): Promise<number> {
    return await this.tableRows.count()
  }

  /**
   * 获取指定行的文本内容
   */
  async getRowText(rowIndex: number): Promise<string> {
    return await this.tableRows.nth(rowIndex).textContent() || ''
  }

  /**
   * 点击"查看日志"按钮
   */
  async viewLogs(rowIndex: number) {
    await this.tableRows.nth(rowIndex).getByRole('button', { name: '查看日志' }).click()
    await this.logDialog.waitFor({ state: 'visible' })
  }

  /**
   * 关闭日志弹窗
   */
  async closeLogDialog() {
    // 使用 Dialog 右上角的关闭按钮 (X 图标)
    await this.logDialog.locator('.el-dialog__headerbtn').click()
    await this.logDialog.waitFor({ state: 'hidden' })
  }

  /**
   * 批量删除选中的记录
   */
  async batchDelete() {
    await this.batchDeleteButton.click()
    // 确认删除弹窗
    await this.page.locator('.el-message-box__btns .el-button--primary').click()
    // 使用 domcontentloaded 避免 Firefox 浏览器 networkidle 超时问题
    await this.page.waitForLoadState('domcontentloaded')
    await this.page.waitForTimeout(500)
  }

  /**
   * 选择表格第一行
   */
  async selectRow(rowIndex: number) {
    await this.tableRows.nth(rowIndex).locator('.el-checkbox').click()
  }
}
