# Opster System 开发指南

这是一个基于 Spring Boot (后端) 和 Vue 3 (前端) 的全栈项目。

## 目录结构

```
opster/
├── opster-backend/    # 后端项目 (Java/Spring Boot)
└── opster-frontend/   # 前端项目 (Vue 3/Vite)
```

## 1. 环境准备

确保你的电脑上安装了以下软件：
- **Java JDK 25**: 运行后端 (环境变量配置)。
- **Maven 4+**: 构建后端 (环境变量配置)。
- **Node.js 18+**: 运行前端构建工具。

## 2. 运行后端 (Spring Boot)

1. 进入后端目录：
   ```bash
   cd opster-backend
   ```
2. 使用 Maven 运行：
   ```bash
   mvn spring-boot:run
   ```
   或者在 IDE (IntelliJ IDEA) 中直接运行 `OpsterApplication.java`。

后端启动后，将在 `http://localhost:8080` 监听。
你可以访问 `http://localhost:8080/api/hello` 测试接口，应返回 JSON 数据。

## 3. 运行前端 (Vue)

1. 进入前端目录：
   ```bash
   cd opster-frontend
   ```
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```

前端启动后，通常在 `http://localhost:5173`。
打开浏览器访问该地址，你应该能看到页面显示 "Hello from Spring Boot!"，这证明前后端联调成功。

## 4. 关键配置说明

### 后端跨域 (CORS)
在 `com.opster.config.WebConfig` 中配置了允许 `http://localhost:5173` 访问。

### 前端代理 (Proxy)
在 `vite.config.js` 中配置了 `/api` 的代理，将请求转发到 `http://localhost:8080`，解决了开发环境的跨域问题。
