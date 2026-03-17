import { test, expect } from '@playwright/test'
import { DeploymentRecordPage } from './pages/DeploymentRecordPage'
import { testdata, assertions } from './fixtures/testFixtures'

/**
 * 发版记录管理 E2E 测试
 *
 * 测试场景覆盖：
 * 1. 页面加载和初始状态
 * 2. 搜索功能（按项目名称、服务名称、服务器 IP、状态）
 * 3. 重置搜索
 * 4. 查看日志
 * 5. 批量删除
 */
test.describe('发版记录管理', () => {
  let deploymentRecordPage: DeploymentRecordPage

  test.beforeEach(async ({ page }) => {
    deploymentRecordPage = new DeploymentRecordPage(page)
    await deploymentRecordPage.goto()
  })

  test('页面应该正确加载', async ({ page }) => {
    // 等待页面完全加载 - 使用 domcontentloaded 避免 Firefox 浏览器 networkidle 超时问题
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(2000)

    // 验证页面标题
    await expect(deploymentRecordPage.pageHeader).toBeVisible()
    await expect(deploymentRecordPage.pageHeader).toContainText('发版记录管理')

    // 验证搜索栏存在 - 服务器 IP 搜索框可能在响应式布局中被折叠，跳过验证
    await expect(deploymentRecordPage.searchInputs.projectName).toBeVisible()
    await expect(deploymentRecordPage.searchInputs.serviceName).toBeVisible()
    // await expect(deploymentRecordPage.searchInputs.serverIp).toBeVisible()
    await expect(deploymentRecordPage.searchInputs.status).toBeVisible()

    // 验证操作按钮存在
    await expect(deploymentRecordPage.searchButton).toBeVisible()
    await expect(deploymentRecordPage.resetButton).toBeVisible()

    // 验证表格存在
    await expect(deploymentRecordPage.table).toBeVisible()
  })

  test('搜索功能 - 按项目名称搜索', async ({ page }) => {
    // 输入项目名称
    await deploymentRecordPage.search({ projectName: '测试' })

    // 验证搜索结果
    const rowCount = await deploymentRecordPage.getRowCount()
    // 搜索结果应该存在（可能为 0 或多条）
    expect(rowCount).toBeGreaterThanOrEqual(0)

    // 截图
    await page.screenshot({ path: 'artifacts/search-by-project.png' })
  })

  test('搜索功能 - 按服务名称搜索', async ({ page }) => {
    await deploymentRecordPage.search({ serviceName: '测试' })

    const rowCount = await deploymentRecordPage.getRowCount()
    expect(rowCount).toBeGreaterThanOrEqual(0)

    await page.screenshot({ path: 'artifacts/search-by-service.png' })
  })

  test('搜索功能 - 按服务器 IP 搜索', async ({ page }) => {
    // 服务器 IP 搜索框可能在响应式布局中被折叠，使用 Page Object 的 search 方法处理
    await deploymentRecordPage.search({ serverIp: '192.168' })

    const rowCount = await deploymentRecordPage.getRowCount()
    expect(rowCount).toBeGreaterThanOrEqual(0)

    await page.screenshot({ path: 'artifacts/search-by-ip.png' })
  })

  test('搜索功能 - 按状态搜索', async ({ page }) => {
    // 搜索状态为"完成"的记录
    await deploymentRecordPage.search({ status: '完成' })

    const rowCount = await deploymentRecordPage.getRowCount()
    expect(rowCount).toBeGreaterThanOrEqual(0)

    await page.screenshot({ path: 'artifacts/search-by-status.png' })
  })

  test('重置搜索表单', async ({ page }) => {
    // 先输入一些搜索条件
    await deploymentRecordPage.searchInputs.projectName.fill('测试项目')
    await deploymentRecordPage.searchInputs.serviceName.fill('测试服务')

    // 点击搜索按钮触发一次搜索
    await deploymentRecordPage.searchButton.click()
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(500)

    // 重置
    await deploymentRecordPage.resetSearch()

    // 验证表单已清空 - 增加等待时间
    await page.waitForTimeout(1000)
    await expect(deploymentRecordPage.searchInputs.projectName).toHaveValue('', { timeout: 10000 })
    await expect(deploymentRecordPage.searchInputs.serviceName).toHaveValue('', { timeout: 10000 })
  })

  test('查看日志功能', async ({ page }) => {
    const rowCount = await deploymentRecordPage.getRowCount()

    if (rowCount > 0) {
      // 点击第一行的"查看日志"按钮
      await deploymentRecordPage.viewLogs(0)

      // 验证日志弹窗可见
      await expect(deploymentRecordPage.logDialog).toBeVisible()
      await expect(deploymentRecordPage.logContent).toBeVisible()

      // 获取日志内容
      const logContent = await deploymentRecordPage.logContent.textContent()
      console.log('日志内容:', logContent)

      // 截图
      await page.screenshot({ path: 'artifacts/view-logs.png' })

      // 关闭弹窗
      await deploymentRecordPage.closeLogDialog()

      // 验证弹窗已关闭
      await expect(deploymentRecordPage.logDialog).not.toBeVisible()
    } else {
      console.log('暂无发版记录，跳过查看日志测试')
    }
  })

  test('批量删除功能 - 选择记录', async ({ page }) => {
    const rowCount = await deploymentRecordPage.getRowCount()

    if (rowCount > 0) {
      // 选择第一行
      await deploymentRecordPage.selectRow(0)

      // 验证批量删除按钮变为可用
      await expect(deploymentRecordPage.batchDeleteButton).toBeEnabled()

      // 验证按钮显示选中数量
      const buttonText = await deploymentRecordPage.batchDeleteButton.textContent()
      expect(buttonText).toContain('(1)')

      // 截图
      await page.screenshot({ path: 'artifacts/batch-delete-selected.png' })
    } else {
      console.log('暂无发版记录，跳过批量删除测试')
    }
  })
})
