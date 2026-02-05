# 前端 API 配置 - 快速指南

## 🎯 三种部署方案

### 方案 1：前后端同服务器（推荐⭐）

**适用场景**：生产环境，前后端在同一台服务器

#### 配置步骤：

1. **修改 `.env.production`**
   ```bash
   VITE_API_BASE_URL=/api
   ```

2. **打包**
   ```bash
   npm run build
   ```

3. **配置 Nginx**
   ```nginx
   server {
       listen 80;

       # 前端静态文件
       location / {
           root /opt/project/opster/opster-frontend/dist;
           try_files $uri $uri/ /index.html;
       }

       # 后端 API 代理
       location /api {
           proxy_pass http://localhost:8080;
       }
   }
   ```

✅ **优点**：无需处理 CORS，配置简单，性能好

---

### 方案 2：前后端分离部署

**适用场景**：前后端不同服务器，或云环境

#### 配置步骤：

1. **修改 `.env.production`**
   ```bash
   VITE_API_BASE_URL=http://172.16.113.220:8080/api
   ```

2. **打包**
   ```bash
   npm run build
   ```

3. **部署到静态服务器**
   ```bash
   scp -r dist/* user@frontend-server:/var/www/html/
   ```

4. **配置后端 CORS**（重要！）

   在后端添加 `CorsConfig.java`：
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

✅ **优点**：前后端独立部署，扩展性强
⚠️ **注意**：需要配置 CORS

---

### 方案 3：使用配置助手（最简单）

**适用场景**：快速配置，不熟悉命令行

#### 配置步骤：

1. **运行配置助手**
   ```bash
   bash setup-env.sh
   ```

2. **按照提示选择部署方案**

3. **打包**
   ```bash
   npm run build
   ```

4. **部署**
   ```bash
   scp -r dist/* user@server:/path/to/deploy/
   ```

---

## 🚀 快速命令参考

### 本地开发
```bash
npm run dev
# 访问 http://localhost:5173
# API 自动代理到 http://localhost:8080
```

### 生产打包
```bash
npm run build
# 生成 dist 目录
```

### 部署到服务器
```bash
# 方式1：scp
scp -r dist/* user@server:/path/to/deploy/

# 方式2：rsync（推荐）
rsync -avz --delete dist/ user@server:/path/to/deploy/
```

### 验证配置
```bash
# 检查打包后的 API 地址
cat dist/assets/*.js | grep -o 'http[^"]*api'
```

---

## 📝 配置文件说明

| 文件 | 说明 | 使用场景 |
|------|------|---------|
| `.env.development` | 开发环境配置 | `npm run dev` |
| `.env.production` | 生产环境配置 | `npm run build` |
| `.env.example` | 配置模板 | 复制后修改 |
| `setup-env.sh` | 配置助手 | 快速配置 |

---

## ⚙️ 环境变量

| 变量名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `VITE_API_BASE_URL` | API 基础路径 | `/api` | `/api` 或 `http://server:8080/api` |

---

## 🔧 常见问题速查

### Q: 打包后 API 请求失败？
```bash
# 检查环境变量
cat .env.production

# 重新打包
rm -rf dist
npm run build

# 检查打包后的配置
cat dist/assets/*.js | grep api
```

### Q: CORS 跨域错误？
- **方案1**：使用 Nginx 反向代理（推荐）
- **方案2**：配置后端 CORS（见上文）

### Q: 静态文件 404？
Nginx 添加 `try_files`：
```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### Q: 如何验证配置生效？
```bash
# 1. 打包后检查 JS 文件
grep -o 'VITE_API_BASE_URL[^,]*' dist/assets/*.js

# 2. 浏览器 F12 -> Network -> 查看 API 请求地址

# 3. 终端测试
curl http://your-server/api/server/list
```

---

## 📚 更多文档

详细配置说明请查看：
- 📖 [完整部署文档](./DEPLOY.md)
- 📋 [环境变量模板](./.env.example)
- 🔧 [配置助手脚本](./setup-env.sh)

---

## ✅ 配置检查清单

部署前确认：

- [ ] 已修改 `.env.production`
- [ ] 已执行 `npm run build`
- [ ] 已检查 `dist` 目录生成
- [ ] 已验证 API 地址配置正确
- [ ] 已配置 Nginx/Apache（如需要）
- [ ] 已配置后端 CORS（分离部署）
- [ ] 已测试 API 请求正常

---

**快速帮助**：`bash setup-env.sh`
