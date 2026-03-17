import { Page, Locator } from '@playwright/test'

/**
 * 服务管理页面 Page Object
 */
export class ServicePage {
  readonly page: Page
  readonly addServiceButton: Locator
  readonly batchDeployButton: Locator
  readonly searchForm: {
    projectSelect: Locator
    businessLineSelect: Locator
    envSelect: Locator
    runStatusSelect: Locator
    statusSelect: Locator
  }
  readonly searchButton: Locator
  readonly resetButton: Locator
  readonly table: Locator
  readonly tableRows: Locator
  readonly serviceDialog: Locator
  readonly projectSelect: Locator
  readonly addServiceBtn: Locator
  readonly submitButton: Locator
  readonly cancelButton: Locator
  readonly logDialog: Locator
  readonly resultDialog: Locator
  readonly versionHistoryDialog: Locator
  readonly deploymentRecordsDialog: Locator

  constructor(page: Page) {
    this.page = page
    this.addServiceButton = page.getByRole('button', { name: '新增服务' })
    this.batchDeployButton = page.getByRole('button', { name: '批量发版' })

    // 搜索表单
    this.searchForm = {
      projectSelect: page.locator('.el-form-item:has-text("项目名称") .el-select'),
      businessLineSelect: page.locator('.el-form-item:has-text("业务线") .el-select'),
      envSelect: page.locator('.el-form-item:has-text("环境") .el-select'),
      runStatusSelect: page.locator('.el-form-item:has-text("运行状态") .el-select'),
      statusSelect: page.locator('.el-form-item:has-text("启用状态") .el-select')
    }

    this.searchButton = page.getByRole('button', { name: '查询' })
    this.resetButton = page.getByRole('button', { name: '重置' })

    // 表格
    this.table = page.locator('.el-table')
    this.tableRows = page.locator('.el-table__row')

    // 弹窗
    this.serviceDialog = page.locator('.el-dialog:has-text("编辑服务"), .el-dialog:has-text("新增服务")')
    this.projectSelect = this.serviceDialog.locator('el-select[placeholder="请选择项目"]')
    this.addServiceBtn = this.serviceDialog.getByRole('button', { name: '+ 添加服务' })
    this.submitButton = this.serviceDialog.getByRole('button', { name: '确认' })
    this.cancelButton = this.serviceDialog.getByRole('button', { name: '取消' })

    this.logDialog = page.locator('.el-dialog:has-text("服务日志")')
    // 执行结果弹窗 - 使用更精确的标题定位器
    this.resultDialog = page.locator('.el-dialog__title:has-text("执行结果")').locator('..')
    this.versionHistoryDialog = page.locator('.el-dialog:has-text("版本历史")')
    this.deploymentRecordsDialog = page.locator('.el-dialog:has-text("发版记录")')
  }

  /**
   * 导航到服务管理页面
   */
  async goto() {
    await this.page.goto('/service')
    // 使用 domcontentloaded 避免 Firefox 浏览器 networkidle 超时问题
    await this.page.waitForLoadState('domcontentloaded')
    // 等待页面主要元素可见，增加超时时间
    await this.page.waitForTimeout(2000)
  }

  /**
   * 搜索服务
   */
  async search(options: {
    projectId?: string
    businessLine?: string
    env?: string
    runStatus?: string
    status?: string
  }) {
    if (options.projectId) {
      await this.searchForm.projectSelect.click()
      await this.page.waitForTimeout(500)
      await this.page.locator('.el-select-dropdown__item span:has-text("' + options.projectId + '")').first().click()
      await this.page.waitForTimeout(300)
    }
    if (options.businessLine) {
      await this.searchForm.businessLineSelect.click()
      await this.page.waitForTimeout(500)
      await this.page.locator('.el-select-dropdown__item span:has-text("' + options.businessLine + '")').first().click()
      await this.page.waitForTimeout(300)
    }
    if (options.env) {
      await this.searchForm.envSelect.click()
      await this.page.waitForTimeout(500)
      await this.page.locator('.el-select-dropdown__item span:has-text("' + options.env + '")').first().click()
      await this.page.waitForTimeout(300)
    }
    if (options.runStatus) {
      await this.searchForm.runStatusSelect.click()
      await this.page.waitForTimeout(500)
      await this.page.locator('.el-select-dropdown__item span:has-text("' + options.runStatus + '")').first().click()
      await this.page.waitForTimeout(300)
    }
    if (options.status) {
      await this.searchForm.statusSelect.click()
      await this.page.waitForTimeout(500)
      await this.page.locator('.el-select-dropdown__item span:has-text("' + options.status + '")').first().click()
      await this.page.waitForTimeout(300)
    }
    // 直接点击搜索按钮，Playwright 会自动等待元素可交互
    await this.searchButton.click()
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
   * 点击"发版"按钮
   */
  async deployService(rowIndex: number) {
    await this.tableRows.nth(rowIndex).getByRole('button', { name: '发版' }).click()
    // 等待确认弹窗完全显示
    const confirmButton = this.page.locator('.el-message-box__btns .el-button--primary')
    await confirmButton.waitFor({ state: 'visible' })
    await confirmButton.click()
  }

  /**
   * 点击"更多"下拉菜单
   */
  async openMoreDropdown(rowIndex: number) {
    // 先关闭所有已打开的下拉菜单
    await this.page.keyboard.press('Escape')
    await this.page.waitForTimeout(200)

    // 点击当前行的"更多"按钮
    await this.tableRows.nth(rowIndex).getByRole('button', { name: '更多' }).click()
    // 等待下拉菜单完全展开
    await this.page.waitForSelector('.el-dropdown-menu', { state: 'visible', timeout: 5000 })
  }

  /**
   * 点击"发版记录"
   */
  async viewDeploymentRecords(rowIndex: number) {
    await this.openMoreDropdown(rowIndex)
    // 等待菜单项可见并点击 - 使用 .first() 精确定位
    const menuItem = this.page.locator('.el-dropdown-menu__item:has-text("发版记录")')
    await menuItem.first().waitFor({ state: 'visible', timeout: 10000 })
    await menuItem.first().click()
    // 等待弹窗可见，增加超时时间，并等待网络请求完成
    await this.page.waitForLoadState('domcontentloaded')
    await this.deploymentRecordsDialog.waitFor({ state: 'visible', timeout: 20000 })
  }

  /**
   * 点击"版本回退"
   */
  async openVersionHistory(rowIndex: number) {
    await this.openMoreDropdown(rowIndex)
    // 等待菜单项可见并点击 - 使用 .first() 精确定位
    const menuItem = this.page.locator('.el-dropdown-menu__item:has-text("选择历史版本回退")')
    await menuItem.first().waitFor({ state: 'visible', timeout: 10000 })
    await menuItem.first().click()
    // 等待弹窗可见
    await this.versionHistoryDialog.waitFor({ state: 'visible', timeout: 20000 })
  }

  /**
   * 选择表格行
   */
  async selectRow(rowIndex: number) {
    await this.tableRows.nth(rowIndex).locator('.el-checkbox').click()
  }

  /**
   * 获取选中的服务数量
   */
  async getSelectedCount(): Promise<number> {
    const selectedCheckbox = this.table.locator('.el-checkbox.is-checked')
    return await selectedCheckbox.count()
  }

  /**
   * 点击批量发版
   */
  async batchDeploy() {
    await this.batchDeployButton.click()
    // 等待确认弹窗完全显示
    const confirmButton = this.page.locator('.el-message-box__btns .el-button--primary')
    await confirmButton.waitFor({ state: 'visible' })
    await confirmButton.click()
  }

  /**
   * 等待发版完成
   */
  async waitForDeployComplete() {
    // 使用 domcontentloaded 避免 Firefox 浏览器 networkidle 超时问题
    await this.page.waitForLoadState('domcontentloaded')
    await this.page.waitForTimeout(1000)
  }

  /**
   * 获取服务运行状态
   */
  async getServiceStatus(rowIndex: number): Promise<string> {
    // 运行状态标签在表格中是第二个 el-tag（第一个是服务类型标签）
    const statusTag = this.tableRows.nth(rowIndex).locator('.el-tag').nth(1)
    return await statusTag.textContent() || ''
  }
}
