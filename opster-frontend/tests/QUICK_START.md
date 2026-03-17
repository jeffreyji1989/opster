# E2E 测试快速指南

## 一分钟开始

```bash
# 1. 安装 Playwright
cd opster-frontend
npm install -D @playwright/test
npx playwright install chromium

# 2. 启动服务
npm run dev              # 终端 1：前端
cd ../opster-backend
mvn spring-boot:run      # 终端 2：后端

# 3. 运行测试
cd ../opster-frontend
npx playwright test      # 终端 3：测试
```

## 常用命令

```bash
# 运行所有测试
npx playwright test

# 运行特定测试文件
npx playwright test tests/e2e/service-deploy.spec.ts

# 有头模式（看浏览器操作）
npx playwright test --headed

# 调试模式（逐步执行）
npx playwright test --debug

# 只看报告
npx playwright show-report
```

## npm 脚本

```bash
npm run test:e2e              # 运行测试
npm run test:e2e:headed       # 有头模式
npm run test:e2e:debug        # 调试模式
npm run test:e2e:report       # 查看报告
```

## 测试文件

| 文件 | 测试内容 |
|------|----------|
| `tests/e2e/service-deploy.spec.ts` | 服务发版功能 |
| `tests/e2e/deployment-record.spec.ts` | 发版记录管理 |

## Page Object

| 类 | 说明 |
|------|----------|
| `ServicePage` | 服务管理页面 |
| `DeploymentRecordPage` | 发版记录页面 |

## 测试报告

- HTML 报告：`playwright-report/index.html`
- 截图：`artifacts/*.png`
- 录像：`playwright-report/videos/`

## 遇到问题？

1. **页面不加载** → 检查前端服务是否运行
2. **API 错误** → 检查后端服务是否运行
3. **浏览器启动失败** → 运行 `npx playwright install`
4. **测试超时** → 增加配置中的 timeout 值
