# Playwright 测试运行脚本

## 安装依赖

```bash
# 安装 Playwright
npm install -D @playwright/test

# 安装浏览器
npx playwright install
```

## 运行测试

```bash
# 运行所有测试
npx playwright test

# 运行特定测试文件
npx playwright test tests/e2e/service-deploy.spec.ts
npx playwright test tests/e2e/deployment-record.spec.ts

# 运行特定测试用例
npx playwright test --grep "单服务发版"

# 有头模式运行（显示浏览器）
npx playwright test --headed

# 调试模式运行
npx playwright test --debug

# 指定浏览器运行
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit

# 重复运行测试（检测稳定性）
npx playwright test --repeat-each=10

# 失败重试
npx playwright test --retries=3
```

## 查看测试报告

```bash
# 打开 HTML 报告
npx playwright show-report

# 打开跟踪查看器
npx playwright show-trace artifacts/trace.json
```

## 环境变量

```bash
# 设置基础 URL
BASE_URL=http://localhost:5173 npx playwright test

# CI 环境
CI=true npx playwright test
```

## 测试文件说明

- `tests/e2e/service-deploy.spec.ts` - 服务发版功能测试
- `tests/e2e/deployment-record.spec.ts` - 发版记录管理测试
- `tests/e2e/pages/` - Page Object 模型
- `tests/e2e/fixtures/` - 测试夹具和工具函数

## 截图和录像

- 截图：`artifacts/*.png`
- 录像：`playwright-report/videos/`
- 跟踪：`playwright-report/traces/`
