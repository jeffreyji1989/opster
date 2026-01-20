# 优化根据描述生成Linux命令功能的实施计划

## 当前状态分析
- 项目已集成spring-ai-alibaba框架（版本1.1.0.0）
- application.yml已配置DashScope API密钥和模型（qwen-plus）
- 缺少TerminalController.java文件和相关服务类

## 实施步骤

### 1. 创建CommandPromptTemplate.java
- 路径：`/src/main/java/com/opster/module/terminal/service/CommandPromptTemplate.java`
- 功能：构建AI提示词模板，包含Linux命令生成要求
- 实现：`buildUserPrompt(String description)`方法

### 2. 创建LinuxCommandAIService.java
- 路径：`/src/main/java/com/opster/module/terminal/service/LinuxCommandAIService.java`
- 功能：使用Spring AI Alibaba的DashScope API生成Linux命令
- 实现：
  - 使用`DashScopeChatModel`和`DashScopeApi`
  - 调用AI模型生成命令
  - 处理Markdown包裹的安全过滤
  - 异常处理机制

### 3. 创建TerminalController.java
- 路径：`/src/main/java/com/opster/module/terminal/controller/TerminalController.java`
- 功能：提供REST API接口供前端调用
- 实现：
  - `@PostMapping("/generate-command")`接口
  - 注入`LinuxCommandAIService`
  - 解析AI返回的命令（按行分割）
  - 确保返回3个命令
  - 异常处理和降级策略

### 4. 技术要点
- 使用Spring AI Alibaba的`DashScopeChatModel`和`DashScopeApi`
- 配置模型：qwen-plus（已在application.yml中配置）
- API密钥：从环境变量或配置文件读取
- 安全过滤：移除Markdown代码块包裹
- 错误处理：提供降级方案

## 预期效果
- 前端调用`/api/terminal/generate-command`接口
- AI根据用户描述智能生成3个相关Linux命令
- 每次生成结果可能不同，避免重复
- 异常情况下返回默认命令作为降级