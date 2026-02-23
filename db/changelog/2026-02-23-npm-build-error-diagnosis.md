# 前端构建失败诊断增强

## 日期
2026-02-23

## 问题描述

### 错误日志
```
npm verb stack Error: missing script: build
npm ERR! missing script: build
```

### 根本原因

**构建命令配置不匹配**:

1. 系统默认使用 `npm run build`
2. 但项目的 `package.json` 中没有定义 `build` 脚本
3. 这是一个 **React Native** 项目,通常使用不同的构建命令

**项目根目录**: `/Users/deffrey/D1_JEFFREY/aiworkspace/deploy/yn-bpm/source/tools/react-native`

**配置的构建命令**:
```
npm config set registry https://registry.npm.taobao.org && npm config set "strict-ssl" false -g && npm run build
```

## 修改内容

### LocalBuildServiceImpl.java
**文件**: `opster-backend/src/main/java/com/opster/module/service/service/impl/LocalBuildServiceImpl.java`

#### 修改 1: 增强构建错误提示 (第 780-822 行)

**修改前**:
```java
private boolean executeNpmBuild(Path projectRoot, String buildCmd, String nodeBinDir,
                               WebSocketSession wsSession, LocalBuildLogger logger) {
    // ... 省略代码
    logger.info("执行npm构建命令: " + fullBuildCmd);
    return LocalCommandUtils.executeCommand(projectRoot, fullBuildCmd, wsSession, envVars, logger);
}
```

**修改后**:
```java
private boolean executeNpmBuild(Path projectRoot, String buildCmd, String nodeBinDir,
                               WebSocketSession wsSession, LocalBuildLogger logger) {
    // ... 省略代码
    logger.info("执行npm构建命令: " + fullBuildCmd);
    boolean result = LocalCommandUtils.executeCommand(projectRoot, fullBuildCmd, wsSession, envVars, logger);

    // ✅ 如果构建失败,尝试提供诊断信息
    if (!result) {
        logger.error("========================================");
        logger.error("npm构建失败,可能的原因:");
        logger.error("1. package.json 中缺少对应的构建脚本");
        logger.error("2. 请检查 package.json 的 scripts 字段");
        logger.error("");
        logger.error("常用前端构建命令:");
        logger.error("  - Vue/React Web: npm run build");
        logger.error("  - React Native: npm run bundle 或使用特定平台构建");
        logger.error("  - Electron: npm run build 或 npm run package");
        logger.error("");
        logger.error("当前配置的构建命令: " + actualBuildCmd);
        logger.error("========================================");
    }

    return result;
}
```

#### 修改 2: 显示可用的npm脚本 (第 288-298 行 和新增方法)

在执行构建前,添加了日志输出显示 package.json 中可用的脚本:

```java
// 6.5. 检查package.json中的可用脚本(帮助用户配置正确的构建命令)
logAvailableNpmScripts(projectRoot, actualLogger);
```

**新增方法** (第 824-889 行):
```java
/**
 * 记录package.json中可用的npm脚本
 * 帮助用户了解项目有哪些可用的构建命令
 */
private void logAvailableNpmScripts(Path projectRoot, LocalBuildLogger logger) {
    try {
        Path packageJsonPath = projectRoot.resolve("package.json");
        if (!Files.exists(packageJsonPath)) {
            logger.warn("未找到 package.json 文件");
            return;
        }

        // 读取并解析 package.json,提取 scripts
        // ... 省略实现

        if (!availableScripts.isEmpty()) {
            logger.info("========================================");
            logger.info("package.json 中可用的构建脚本:");
            for (String script : availableScripts) {
                logger.info("  - npm run " + script);
            }
            logger.info("========================================");
        }
    } catch (Exception e) {
        logger.warn("读取 package.json 失败: " + e.getMessage());
    }
}
```

## 用户体验改进

### 修改前
```
npm ERR! missing script: build
npm构建失败
部署失败
```

### 修改后
```
========================================
package.json 中可用的构建脚本:
  - npm run android
  - npm run ios
  - npm run start
  - npm run test
========================================

执行npm构建命令: npm run build --loglevel=verbose
npm ERR! missing script: build

========================================
npm构建失败,可能的原因:
1. package.json 中缺少对应的构建脚本
2. 请检查 package.json 的 scripts 字段

常用前端构建命令:
  - Vue/React Web: npm run build
  - React Native: npm run bundle 或使用特定平台构建
  - Electron: npm run build 或 npm run package

当前配置的构建命令: npm run build
========================================
```

## 解决方案

### 针对当前项目 (yn-bpm React Native)

**选项 1: 使用正确的构建命令**
```
npm run android
# 或
npm run ios
```

**选项 2: 如果是 Web 版本,检查是否有 web 脚本**
```
npm run web
# 或
npm run start
```

**选项 3: 如果需要打包 JS Bundle**
```bash
npx react-native bundle --platform android --dev false --entry-file index.js --bundle-output android/app/src/main/assets/index.android.bundle --assets-dest android/app/src/main/res/
```

## 建议

1. **检查项目类型**: 确认项目是纯前端还是 React Native/Electron
2. **查看 package.json**: 使用新增的诊断功能查看可用脚本
3. **更新构建命令**: 在服务配置中使用正确的构建命令

## 相关文档

- React Native 官方文档: https://reactnative.dev/docs/building-for-android
- Vue 构建文档: https://cli.vuejs.org/guide/build.html
- Create React App 文档: https://create-react-app.dev/docs/production-build
