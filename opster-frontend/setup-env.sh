#!/bin/bash

# 前端部署配置助手
# 使用方法: bash setup-env.sh

echo "======================================"
echo "  Opster 前端部署配置助手"
echo "======================================"
echo ""

# 检查 .env.production 是否存在
if [ -f .env.production ]; then
    echo "📝 检测到现有 .env.production 配置："
    cat .env.production
    echo ""
    read -p "是否重新配置？(y/n): " overwrite
    if [ "$overwrite" != "y" ]; then
        echo "保持现有配置，退出。"
        exit 0
    fi
fi

echo "请选择部署方案："
echo "1) 前后端同服务器部署（推荐，使用 Nginx 反向代理）"
echo "2) 前后端分离部署（需要配置后端服务器地址）"
echo ""
read -p "请输入选项 (1 或 2): " choice

case $choice in
    1)
        echo ""
        echo "✅ 已选择：前后端同服务器部署"
        echo "VITE_API_BASE_URL=/api" > .env.production
        echo ""
        echo "📋 配置说明："
        echo "  - API 地址: /api（相对路径）"
        echo "  - 需要配置 Nginx 反向代理到后端"
        echo ""
        echo "🔧 Nginx 配置示例："
        cat << 'EOF'
location /api {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
EOF
        ;;
    2)
        echo ""
        echo "✅ 已选择：前后端分离部署"
        echo ""
        read -p "请输入后端服务器地址 (例: http://172.16.113.220:8080/api): " backend_url
        if [ -z "$backend_url" ]; then
            echo "❌ 未输入地址，使用默认配置"
            backend_url="http://localhost:8080/api"
        fi
        echo "VITE_API_BASE_URL=$backend_url" > .env.production
        echo ""
        echo "📋 配置说明："
        echo "  - API 地址: $backend_url"
        echo "  - 确保后端服务器已启动并可访问"
        echo "  - 如遇 CORS 问题，请参考 DEPLOY.md 配置后端 CORS"
        ;;
    *)
        echo "❌ 无效选项，退出"
        exit 1
        ;;
esac

echo ""
echo "✅ 配置已保存到 .env.production"
echo ""
echo "📦 下一步操作："
echo "  1. 执行打包: npm run build"
echo "  2. 部署 dist 目录到服务器"
echo "  3. 配置 Nginx/Apache（如需要）"
echo ""
echo "📖 详细说明请查看: cat DEPLOY.md"
echo ""
