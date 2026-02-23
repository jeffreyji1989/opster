# 目录结构重构说明

## 变更日期
2026-02-22

## 变更概述
简化本地构建目录结构，移除 `{serviceAlias}` 层级，统一使用项目级别的目录管理。

## 变更详情

### 原目录结构
```
{opster.deploy-path}/
└── {projectCode}/                          # 项目编码
    └── {serviceAlias}/                      # 服务别名（从 Git URL 自动提取仓库名）
        ├── source/                          # 源码目录（Git 仓库）
        ├── artifacts/                       # 编译产物归档目录
        └── logs/                            # 构建日志目录
        └── p_log/                           # 发版日志目录
```

### 新目录结构
```
{opster.deploy-path}/
└── {projectCode}/                          # 项目编码
    ├── source/                              # 源码目录（Git 仓库，所有服务共享）
    ├── artifacts/                           # 编译产物归档目录（所有服务共享）
    └── logs/                                # 日志目录（构建日志 + 发版日志）
```

## 影响分析

### 优点
1. **简化目录结构**：减少一层嵌套，路径更短，更易管理
2. **共享源码**：同一项目的多个服务可以共享同一个 Git 仓库源码
3. **统一产物管理**：所有编译产物集中管理，便于清理和归档
4. **日志集中化**：构建日志和发版日志统一存储

### 注意事项
1. **Monorepo 支持**：通过 `projectPath` 字段区分同一仓库下的不同子项目
2. **产物命名**：编译产物文件名包含时间戳前缀，避免同名冲突
3. **日志文件名**：日志文件名包含毫秒级时间戳，确保唯一性

### 代码变更
1. **LocalBuildServiceImpl.java**
   - `getUniqueSourceDir()`：返回 `{deployPath}/{projectCode}/source`
   - `getArtifactsDir()`：返回 `{deployPath}/{projectCode}/artifacts`
   - `getLogsDir()`：返回 `{deployPath}/{projectCode}/logs`
   - `getSourceDir()`：返回 `{deployPath}/{projectCode}/source`

2. **LocalDeploymentLogger.java**
   - `createLogFile()`：日志目录改为 `{deployPath}/{projectCode}/logs`
   - 移除 `p_log` 子目录，统一使用 `logs` 目录

## 兼容性
- 接口方法保留 `serviceAlias` 参数，确保向后兼容
- 参数值不再用于目录路径计算，仅作为占位符保留

## 数据库变更
无（仅本地文件系统目录结构调整）

## 迁移建议
对于已有部署，建议：
1. 备份旧目录结构中的数据
2. 手动迁移需要的源码和产物到新目录
3. 新发版将自动使用新目录结构
