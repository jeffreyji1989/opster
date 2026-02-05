# 前端部署配置说明

## 📋 目录
- [开发环境配置](#开发环境配置)
- [生产环境配置](#生产环境配置)
- [部署方案](#部署方案)
- [常见问题](#常见问题)

---

## 🔧 开发环境配置

开发环境已配置好代理，无需修改：

```javascript
// vite.config.js
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端地址
      changeOrigin: true
    }
  }
}
```

启动开发服务器：
```bash
npm run dev
```

访问 `http://localhost:5173`，API 请求会自动代理到 `http://localhost:8080`

---

## 🚀 生产环境配置

### 方案一：前后端同服务器部署（推荐）

#### 1. 修改 `.env.production`

```bash
VITE_API_BASE_URL=/api
```

#### 2. 打包前端

```bash
npm run build
```

#### 3. 配置 Nginx

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /opt/project/opster/opster-frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

#### 4. 部署步骤

```bash
# 1. 打包
npm run build

# 2. 上传 dist 目录到服务器
scp -r dist/* user@server:/opt/project/opster/opster-frontend/

# 3. 配置 Nginx（如上）
sudo nginx -t
sudo nginx -s reload
```

---

### 方案二：前后端分离部署

#### 1. 修改 `.env.production`

```bash
# 后端服务器地址
VITE_API_BASE_URL=http://172.16.113.220:8080/api
```

#### 2. 打包前端

```bash
npm run build
```

#### 3. 部署到静态服务器（Nginx/Apache）

```nginx
server {
    listen 80;
    server_name frontend.your-domain.com;

    location / {
        root /var/www/opster-frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

#### 4. 配置后端 CORS（如果跨域）

**Spring Boot 后端添加 CORS 配置**：

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
```

---

### 方案三：使用 Docker Compose

#### docker-compose.yml

```yaml
version: '3.8'
services:
  backend:
    build: ./opster-backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod

  frontend:
    build: ./opster-frontend
    ports:
      - "80:80"
    depends_on:
      - backend
```

#### 前端 Dockerfile

```dockerfile
FROM node:18-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 🛠️ 快速配置指南

### 步骤 1：选择部署方案

| 部署方式 | 适用场景 | API 配置 |
|---------|---------|---------|
| **同服务器部署** | 生产环境推荐 | `/api` |
| **分离部署** | 前后端不同服务器 | `http://backend-server:8080/api` |
| **开发测试** | 本地开发 | 默认代理配置 |

### 步骤 2：修改环境变量

编辑 `.env.production`：

```bash
# 同服务器
VITE_API_BASE_URL=/api

# 或分离部署
VITE_API_BASE_URL=http://your-backend-ip:8080/api
```

### 步骤 3：打包

```bash
npm run build
```

### 步骤 4：部署

```bash
# 上传 dist 目录到服务器
scp -r dist/* user@server:/path/to/frontend/

# 或使用 rsync
rsync -avz dist/ user@server:/path/to/frontend/
```

---

## 🔍 验证部署

### 1. 检查打包后的配置

打包后，检查 `dist/assets/index-*.js` 文件，确认 API 地址：

```bash
grep -o "VITE_API_BASE_URL[^,]*" dist/assets/*.js
```

### 2. 浏览器测试

打开浏览器开发者工具（F12），查看 Network 标签：

- ✅ 请求地址正确
- ✅ 状态码 200
- ✅ 能正常获取数据

### 3. 常见问题排查

```bash
# 1. 检查环境变量是否生效
npm run build
cat dist/assets/*.js | grep -o 'http[^"]*api'

# 2. 检查 Nginx 配置
sudo nginx -t

# 3. 查看 Nginx 日志
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

---

## ❓ 常见问题

### Q1: 打包后 API 请求 404

**原因**：环境变量没有生效或配置错误

**解决**：
1. 确保 `.env.production` 文件存在
2. 重新执行 `npm run build`
3. 检查打包后的 JS 文件中 API 地址

### Q2: CORS 跨域问题

**原因**：前后端分离部署，端口/域名不同

**解决**：
1. 推荐使用 Nginx 反向代理（方案一）
2. 或在后端添加 CORS 配置

### Q3: 静态文件 404

**原因**：Nginx 配置错误，或 Vue Router history 模式问题

**解决**：
```nginx
location / {
    try_files $uri $uri/ /index.html;  # 添加这行
}
```

### Q4: 生产环境仍然连接开发服务器

**原因**：使用了开发命令打包

**解决**：
```bash
npm run build  # 正确，使用 .env.production
# 而不是 npm run dev
```

---

## 📞 技术支持

如有问题，请检查：
1. `.env.production` 配置是否正确
2. 是否重新执行了 `npm run build`
3. 浏览器控制台是否有错误信息
4. Nginx/Apache 配置是否正确
