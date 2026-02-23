# 修复发版时 Git 认证失败问题

## 日期
2026-02-23

## 问题描述

### 错误日志
```
[2026-02-23 15:37:41] fatal: could not read Username for 'http://public.shengchaozhineng.com:18230': Device not configured
[2026-02-23 15:37:41] >>> Command failed with exit code: 128
[2026-02-23 15:37:41] git clone 命令执行失败
```

### 根本原因

**Git 认证信息获取逻辑过时**:

1. 系统已经废弃 `Project` 实体中的 `git_username` 和 `git_password` 字段
2. 新架构使用 `SubProject` + `GitAccount` 来管理 Git 认证信息
3. 但 `DeploymentOrchestrationServiceImpl` 中的 `determineGitUsername` 和 `determineGitPassword` 方法仍然从废弃的 `Project` 字段中获取认证信息
4. 导致执行 `git clone` 时用户名密码为 `null`,Git 尝试交互式读取但失败

**旧逻辑** (错误):
```java
private String determineGitUsername(AppService service, Project project) {
    return project.getGitUsername();  // 字段已废弃,返回 null
}
```

## 修改内容

### DeploymentOrchestrationServiceImpl.java
**文件**: `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentOrchestrationServiceImpl.java`

#### 修改 1: 添加 Repository 依赖 (第 61-76 行)

**修改前**:
```java
@Autowired
private AppServiceRepository appServiceRepository;

@Autowired
private ServerRepository serverRepository;

@Autowired
private ProjectRepository projectRepository;

@Autowired
private LocalBuildService localBuildService;
```

**修改后**:
```java
@Autowired
private AppServiceRepository appServiceRepository;

@Autowired
private ServerRepository serverRepository;

@Autowired
private ProjectRepository projectRepository;

@Autowired
private com.opster.module.project.repository.SubProjectRepository subProjectRepository;

@Autowired
private com.opster.module.git.repository.GitAccountRepository gitAccountRepository;

@Autowired
private LocalBuildService localBuildService;
```

#### 修改 2: 更新 Git 认证信息获取逻辑 (第 779-821 行)

**修改前**:
```java
/**
 * 确定Git认证用户名
 */
private String determineGitUsername(AppService service, Project project) {
    // 从项目配置获取用户名
    return project.getGitUsername();
}

/**
 * 确定Git认证密码
 */
private String determineGitPassword(AppService service, Project project) {
    // 从项目配置获取密码
    return project.getGitPassword();
}
```

**修改后**:
```java
/**
 * 确定Git认证用户名
 */
private String determineGitUsername(AppService service, Project project) {
    // 优先从SubProject关联的GitAccount获取用户名
    if (service.getSubProjectId() != null) {
        return subProjectRepository.findById(service.getSubProjectId())
            .map(subProject -> {
                if (subProject.getGitAccountId() != null) {
                    return gitAccountRepository.findById(subProject.getGitAccountId())
                        .map(com.opster.module.git.entity.GitAccount::getGitUsername)
                        .orElse(null);
                }
                return null;
            })
            .orElse(null);
    }

    // 兼容旧数据：从Project配置获取用户名
    return project.getGitUsername();
}

/**
 * 确定Git认证密码
 */
private String determineGitPassword(AppService service, Project project) {
    // 优先从SubProject关联的GitAccount获取密码
    if (service.getSubProjectId() != null) {
        return subProjectRepository.findById(service.getSubProjectId())
            .map(subProject -> {
                if (subProject.getGitAccountId() != null) {
                    return gitAccountRepository.findById(subProject.getGitAccountId())
                        .map(com.opster.module.git.entity.GitAccount::getGitPassword)
                        .orElse(null);
                }
                return null;
            })
            .orElse(null);
    }

    // 兼容旧数据：从Project配置获取密码
    return project.getGitPassword();
}
```

## 数据关系

```
Project (项目)
    ↓ (1对多)
SubProject (子项目)
    ↓ (多对1)
GitAccount (Git账号)
    ├── gitUsername
    └── gitPassword
```

## 影响范围

### 优先级逻辑
1. **新架构**: 如果 `Service.subProjectId` 不为空,从 `SubProject → GitAccount` 获取认证信息
2. **兼容旧数据**: 如果 `subProjectId` 为空,从 `Project` 的废弃字段获取(兼容性考虑)

### 相关功能
- ✅ 本地 Maven 构建
- ✅ 本地 NPM 构建
- ✅ 定时部署任务
- ✅ 手动发版操作

## 测试建议

1. **新建项目测试**: 创建新项目(使用 SubProject + GitAccount),验证发版功能
2. **旧项目兼容**: 验证旧的 Project 配置仍然可用
3. **Git HTTPS 认证**: 验证带特殊字符的密码(如 @、:、#、%)能正确编码
4. **Git SSH 认证**: 验证 SSH 方式也能正常工作

## 相关文档

- `2026-02-13-create-git-account-table.sql` - Git账号表创建
- `2026-02-13-create-sub-project-table.sql` - 子项目表创建
