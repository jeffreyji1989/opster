# 修复发版失败 - 构建命令为空的问题

## 变更日期
2026-02-22

## 问题描述
发版时出现 NullPointerException 错误：
```
Cannot invoke "String.contains(java.lang.CharSequence)" because "mavenCmd" is null
```

## 根本原因
1. **数据库旧数据问题**：已标记为 `@Deprecated` 的字段（`mavenCmd`、`buildCmd`）在数据库中为 `null`
2. **代码防御不足**：`LocalBuildServiceImpl` 在执行构建前没有检查命令是否为空
3. **触发位置**：
   - `executeMavenBuild()` 方法第 647 行：`if (!mavenCmd.contains("-B"))`
   - `executeNpmBuild()` 方法第 787 行：`if (!buildCmd.contains("--loglevel"))`

## 修复方案

### 1. LocalBuildServiceImpl.java - buildMavenArtifact()
在第 136-142 行添加默认命令处理：
```java
// 如果 mavenCmd 为空，使用默认命令
String actualMavenCmd = mavenCmd;
if (StrUtil.isBlank(actualMavenCmd)) {
    actualMavenCmd = "mvn clean package -DskipTests";
    actualLogger.info("Maven命令为空，使用默认命令: " + actualMavenCmd);
}
```

### 2. LocalBuildServiceImpl.java - buildNpmArtifact()
在第 289-295 行添加默认命令处理：
```java
// 如果 buildCmd 为空，使用默认命令
String actualBuildCmd = buildCmd;
if (StrUtil.isBlank(actualBuildCmd)) {
    actualBuildCmd = "npm run build";
    actualLogger.info("构建命令为空，使用默认命令: " + actualBuildCmd);
}
```

### 3. LocalBuildServiceImpl.java - executeMavenBuild()
在第 638-656 行添加二次防护：
```java
// 如果 mavenCmd 为空，使用默认命令
String actualMavenCmd = mavenCmd;
if (StrUtil.isBlank(actualMavenCmd)) {
    actualMavenCmd = "mvn clean package -DskipTests";
    logger.info("Maven命令为空，使用默认命令");
}
```

### 4. LocalBuildServiceImpl.java - executeNpmBuild()
在第 781-792 行添加二次防护：
```java
// 如果 buildCmd 为空，使用默认命令
String actualBuildCmd = buildCmd;
if (StrUtil.isBlank(actualBuildCmd)) {
    actualBuildCmd = "npm run build";
    logger.info("构建命令为空，使用默认命令: " + actualBuildCmd);
}
```

## 默认构建命令

| 项目类型 | 默认命令 |
|---------|---------|
| Maven 后端 | `mvn clean package -DskipTests` |
| npm 前端 | `npm run build` |

## 影响范围
- ✅ 修复旧数据发版失败问题
- ✅ 新建服务时可不填写构建命令，系统自动使用默认值
- ✅ 向后兼容，不影响已配置自定义构建命令的服务

## 测试建议
1. 使用旧数据（mavenCmd/buildCmd 为 null）的服务发版，验证使用默认命令
2. 使用新数据（已配置自定义命令）的服务发版，验证使用自定义命令
3. 前端项目发版验证默认使用 `npm run build`
4. 后端项目发版验证默认使用 `mvn clean package -DskipTests`

## 数据库变更
无（仅代码逻辑修复）
