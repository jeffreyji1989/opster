#!/bin/bash

# Playwright 测试环境设置脚本

set -e

echo "=========================================="
echo "Playwright 测试环境设置"
echo "=========================================="

# 检查 Node.js
echo ""
echo "检查 Node.js..."
if ! command -v node &> /dev/null; then
    echo "错误：未检测到 Node.js，请先安装 Node.js 18+"
    exit 1
fi
echo "Node.js 版本：$(node -v)"

# 检查 npm
echo ""
echo "检查 npm..."
if ! command -v npm &> /dev/null; then
    echo "错误：未检测到 npm"
    exit 1
fi
echo "npm 版本：$(npm -v)"

# 修复 npm 缓存权限
echo ""
echo "修复 npm 缓存权限..."
if [ -d "$HOME/.npm" ]; then
    sudo chown -R $(whoami) "$HOME/.npm" 2>/dev/null || {
        echo "警告：无法自动修复 npm 权限，请手动运行："
        echo "  sudo chown -R $(whoami) $HOME/.npm"
    }
fi

# 安装 Playwright
echo ""
echo "安装 Playwright..."
cd "$(dirname "$0")/.."
npm install -D @playwright/test

# 安装浏览器
echo ""
echo "安装 Chromium 浏览器..."
npx playwright install chromium

# 安装其他浏览器（可选）
echo ""
read -p "是否安装 Firefox 和 WebKit 浏览器？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    npx playwright install firefox webkit
fi

# 生成测试报告目录
echo ""
echo "创建测试报告目录..."
mkdir -p playwright-report
mkdir -p artifacts

echo ""
echo "=========================================="
echo "设置完成！"
echo "=========================================="
echo ""
echo "运行测试命令："
echo "  npx playwright test                    # 运行所有测试"
echo "  npx playwright test --project=chromium # 仅运行 Chromium 测试"
echo "  npx playwright test --headed           # 有头模式运行"
echo "  npx playwright test --debug            # 调试模式"
echo "  npx playwright show-report             # 查看测试报告"
echo ""
