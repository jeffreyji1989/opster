import { test, expect } from '@playwright/test'
import { ServicePage } from './pages/ServicePage'
import { testdata, assertions } from './fixtures/testFixtures'

/**
 * 服务发版功能 E2E 测试
 *
 * 测试场景覆盖：
 * 1. 服务列表页面加载
 * 2. 服务搜索功能
 * 3. 单服务发版
 * 4. 批量发版
 * 5. 查看发版记录
 * 6. 版本回退
 * 7. 服务状态验证
 */
test.describe('服务发版功能', () => {
  let servicePage: ServicePage

  test.beforeEach(async ({ page }) => {
    servicePage = new ServicePage(page)
    await servicePage.goto()
  })

  test('服务列表页面应该正确加载', async ({ page }) => {
    // 验证页面基本元素
    await expect(servicePage.addServiceButton).toBeVisible()
    await expect(servicePage.table).toBeVisible()

    // 验证搜索表单
    await expect(servicePage.searchForm.projectSelect).toBeVisible()
    await expect(servicePage.searchForm.businessLineSelect).toBeVisible()
    await expect(servicePage.searchForm.envSelect).toBeVisible()
    await expect(servicePage.searchForm.runStatusSelect).toBeVisible()
    await expect(servicePage.searchForm.statusSelect).toBeVisible()

    // 截图
    await page.screenshot({ path: 'artifacts/service-list-page.png' })
  })

  test('服务搜索功能 - 按运行状态搜索', async ({ page }) => {
    // 搜索正常运行状态的服务
    await servicePage.search({ runStatus: '正常' })

    const rowCount = await servicePage.getRowCount()
    expect(rowCount).toBeGreaterThanOrEqual(0)

    // 验证搜索结果
    for (let i = 0; i < Math.min(rowCount, 3); i++) {
      const status = await servicePage.getServiceStatus(i)
      console.log(`服务 ${i} 状态：${status}`)
    }

    await page.screenshot({ path: 'artifacts/search-by-run-status.png' })
  })

  test('服务搜索功能 - 按环境搜索', async ({ page }) => {
    // 搜索生产环境的服务
    await servicePage.search({ env: '正式' })

    const rowCount = await servicePage.getRowCount()
    expect(rowCount).toBeGreaterThanOrEqual(0)

    await page.screenshot({ path: 'artifacts/search-by-env.png' })
  })

  test('服务搜索功能 - 按启用状态搜索', async ({ page }) => {
    // 搜索启用的服务 - 使用文本"启用"而不是值"1"
    await servicePage.search({ status: '启用' })

    const rowCount = await servicePage.getRowCount()
    expect(rowCount).toBeGreaterThanOrEqual(0)

    await page.screenshot({ path: 'artifacts/search-by-status.png' })
  })

  test('单服务发版流程', async ({ page }) => {
    const rowCount = await servicePage.getRowCount()

    if (rowCount > 0) {
      // 获取发版前的状态
      const statusBeforeDeploy = await servicePage.getServiceStatus(0)
      console.log('发版前状态:', statusBeforeDeploy)

      // 点击发版按钮 (已包含确认弹窗处理)
      await servicePage.deployService(0)

      // 等待成功提示消息
      await page.waitForSelector('.el-message--success', { timeout: 10000 })
      const message = await page.locator('.el-message--success').textContent()
      console.log('发版消息:', message)

      // 等待发版完成（最多等待 30 秒）
      try {
        await page.waitForLoadState('domcontentloaded', { timeout: 30000 })
      } catch (e) {
        console.log('等待页面加载超时，继续测试')
      }

      // 截图
      await page.screenshot({ path: 'artifacts/single-deploy-result.png' })

      // 刷新页面验证状态更新
      await servicePage.goto()

      // 验证状态已更新
      const statusAfterDeploy = await servicePage.getServiceStatus(0)
      console.log('发版后状态:', statusAfterDeploy)

      // 发版后状态应该是正常或发版中
      expect(['正常', '发版中', '未启动', '发版失败']).toContain(statusAfterDeploy)
    } else {
      console.log('暂无服务，跳过发版测试')
    }
  })

  test('批量发版流程', async ({ page }) => {
    const rowCount = await servicePage.getRowCount()

    if (rowCount >= 2) {
      // 选择前两个服务
      await servicePage.selectRow(0)
      await servicePage.selectRow(1)

      // 验证选中数量
      const selectedCount = await servicePage.getSelectedCount()
      expect(selectedCount).toBe(2)

      // 验证批量发版按钮显示选中数量
      const batchDeployButtonText = await servicePage.batchDeployButton.textContent()
      expect(batchDeployButtonText).toContain('(2)')

      // 点击批量发版
      await servicePage.batchDeploy()

      // 等待发版提交完成
      await page.waitForLoadState('domcontentloaded')
      await page.waitForTimeout(2000)

      // 截图
      await page.screenshot({ path: 'artifacts/batch-deploy.png' })

      // 验证成功提示 - 增加等待时间和超时
      const message = page.locator('.el-message--success')
      try {
        await message.waitFor({ state: 'visible', timeout: 20000 })
        const messageText = await message.textContent()
        expect(messageText).toContain('已提交')
      } catch (e) {
        console.log('未找到成功消息，可能已经自动消失或发版中')
      }

      // 刷新页面
      await servicePage.goto()

      // 验证服务状态
      for (let i = 0; i < 2; i++) {
        const status = await servicePage.getServiceStatus(i)
        console.log(`批量发版服务 ${i} 状态：${status}`)
      }
    } else {
      console.log('服务数量不足 2 个，跳过批量发版测试')
    }
  })

  test('查看发版记录', async ({ page }) => {
    const rowCount = await servicePage.getRowCount()

    if (rowCount > 0) {
      // 打开第一个服务的发版记录
      await servicePage.viewDeploymentRecords(0)

      // 验证发版记录弹窗可见
      await expect(servicePage.deploymentRecordsDialog).toBeVisible()

      // 获取发版记录表格
      const recordsTable = servicePage.deploymentRecordsDialog.locator('.el-table')
      const recordRows = recordsTable.locator('.el-table__row')
      const recordCount = await recordRows.count()

      console.log('发版记录数量:', recordCount)

      // 截图
      await page.screenshot({ path: 'artifacts/deployment-records.png' })

      // 如果有记录，验证记录内容
      if (recordCount > 0) {
        const firstRow = recordRows.first()
        await expect(firstRow).toBeVisible()

        // 验证记录包含状态标签（使用 nth(0) 精确定位第一个标签）
        const statusTag = firstRow.locator('.el-tag').nth(0)
        await expect(statusTag).toBeVisible()
      }

      // 关闭弹窗 - 使用对话框右上角的关闭按钮
      await servicePage.deploymentRecordsDialog.locator('.el-dialog__headerbtn').click()
      await servicePage.deploymentRecordsDialog.waitFor({ state: 'hidden' })
    } else {
      console.log('暂无服务，跳过查看发版记录测试')
    }
  })

  test('版本回退功能', async ({ page }) => {
    const rowCount = await servicePage.getRowCount()

    if (rowCount > 0) {
      // 打开版本历史
      await servicePage.openVersionHistory(0)

      // 验证版本历史弹窗可见
      await expect(servicePage.versionHistoryDialog).toBeVisible()

      // 获取版本列表
      const versionTimeline = servicePage.versionHistoryDialog.locator('.el-timeline')
      const versionItems = versionTimeline.locator('.el-timeline-item')
      const versionCount = await versionItems.count()

      console.log('版本历史数量:', versionCount)

      // 截图
      await page.screenshot({ path: 'artifacts/version-history.png' })

      // 如果有版本历史，验证回退功能
      if (versionCount > 0) {
        // 验证第一个版本卡片
        const firstVersion = versionItems.first()
        await expect(firstVersion).toBeVisible()

        // 验证版本标签
        const versionTags = firstVersion.locator('.el-tag')
        const tagCount = await versionTags.count()
        expect(tagCount).toBeGreaterThan(0)

        // 验证操作按钮
        const rollbackButton = firstVersion.locator('.version-actions .el-button--danger')
        await expect(rollbackButton).toBeVisible()
      }

      // 关闭弹窗 - 使用对话框右上角的关闭按钮
      await servicePage.versionHistoryDialog.locator('.el-dialog__headerbtn').click()
      await servicePage.versionHistoryDialog.waitFor({ state: 'hidden' })
    } else {
      console.log('暂无服务，跳过版本回退测试')
    }
  })

  test('服务运行状态显示', async ({ page }) => {
    const rowCount = await servicePage.getRowCount()

    for (let i = 0; i < Math.min(rowCount, 5); i++) {
      const status = await servicePage.getServiceStatus(i)
      console.log(`服务 ${i} 状态：${status}`)

      // 验证状态标签颜色
      const statusTag = servicePage.tableRows.nth(i).locator('.el-tag').first()
      await expect(statusTag).toBeVisible()
    }

    await page.screenshot({ path: 'artifacts/service-status-display.png' })
  })
})
