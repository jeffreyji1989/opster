# 修复 Git 密码解密缺失问题

## 日期
2026-02-23

## 问题描述

### 关键发现

**Git 密码加密/解密不一致**:

1. **保存时**: 密码通过 `SecurityUtils.encrypt()` **AES 加密**后存储到数据库
2. **使用时**: 直接从数据库读取**加密后的密文**,没有**解密**
3. **结果**: Git clone 使用的是**加密后的密文**而不是明文密码,导致认证失败!

### 加密机制

**算法**: AES 加密
**密钥**: 配置项 `opster.encrypt-key` (默认: `opster-default-key`)
**实现**: `SecurityUtils.java`

**保存流程**:
```java
// GitAccountServiceImpl.save()
if (gitAccount.getGitPassword() != null && !gitAccount.getGitPassword().isEmpty()) {
    if (!SecurityUtils.isEncrypted(gitAccount.getGitPassword())) {
        gitAccount.setGitPassword(SecurityUtils.encrypt(gitAccount.getGitPassword()));
    }
}
```

**问题代码** (使用时未解密):
```java
// DeploymentOrchestrationServiceImpl.determineGitPassword() - 修改前
return gitAccountRepository.findById(subProject.getGitAccountId())
    .map(com.opster.module.git.entity.GitAccount::getGitPassword)  // ❌ 返回加密的密文!
    .orElse(null);
```

## 修改内容

### DeploymentOrchestrationServiceImpl.java
**文件**: `opster-backend/src/main/java/com/opster/module/service/service/impl/DeploymentOrchestrationServiceImpl.java`

#### 修改: `determineGitPassword` 方法 (第 801-832 行)

**修改前**:
```java
private String determineGitPassword(AppService service, Project project) {
    // 优先从SubProject关联的GitAccount获取密码
    if (service.getSubProjectId() != null) {
        return subProjectRepository.findById(service.getSubProjectId())
            .map(subProject -> {
                if (subProject.getGitAccountId() != null) {
                    return gitAccountRepository.findById(subProject.getGitAccountId())
                        .map(com.opster.module.git.entity.GitAccount::getGitPassword)  // ❌ 直接返回加密密码
                        .orElse(null);
                }
                return null;
            })
            .orElse(null);
    }

    // 兼容旧数据：从Project配置获取密码
    return project.getGitPassword();  // ❌ 直接返回加密密码
}
```

**修改后**:
```java
/**
 * 确定Git认证密码(自动解密)
 */
private String determineGitPassword(AppService service, Project project) {
    // 优先从SubProject关联的GitAccount获取密码
    if (service.getSubProjectId() != null) {
        return subProjectRepository.findById(service.getSubProjectId())
            .map(subProject -> {
                if (subProject.getGitAccountId() != null) {
                    return gitAccountRepository.findById(subProject.getGitAccountId())
                        .map(gitAccount -> {
                            // ✅ 密码是加密存储的,需要解密
                            String encryptedPassword = gitAccount.getGitPassword();
                            if (encryptedPassword != null && !encryptedPassword.isEmpty()) {
                                return com.opster.common.SecurityUtils.decrypt(encryptedPassword);
                            }
                            return null;
                        })
                        .orElse(null);
                }
                return null;
            })
            .orElse(null);
    }

    // 兼容旧数据：从Project配置获取密码
    String oldPassword = project.getGitPassword();
    if (oldPassword != null && !oldPassword.isEmpty()) {
        return com.opster.common.SecurityUtils.decrypt(oldPassword);  // ✅ 解密旧密码
    }
    return null;
}
```

## 数据流对比

### 修改前 (错误)
```
用户输入明文密码
    ↓
SecurityUtils.encrypt() → AES加密
    ↓
数据库存储密文 (如: "a1b2c3d4e5f6...")
    ↓
读取密文 ❌
    ↓
git clone 使用密文认证 → 认证失败!
```

### 修改后 (正确)
```
用户输入明文密码
    ↓
SecurityUtils.encrypt() → AES加密
    ↓
数据库存储密文 (如: "a1b2c3d4e5f6...")
    ↓
读取密文
    ↓
SecurityUtils.decrypt() → 解密 ✅
    ↓
git clone 使用明文认证 → 认证成功!
```

## 影响范围

### 受影响的功能
- ✅ 本地构建 (Maven/NPM)
- ✅ 远程部署 (定时部署/手动发版)
- ✅ Git clone 和 git pull 操作

### 加密字段
1. `GitAccount.gitPassword` - HTTPS 认证密码
2. `GitAccount.sshKeyPassphrase` - SSH 私钥密码
3. `Project.gitPassword` - 旧字段 (兼容)

## 测试建议

1. **新建 Git 账号**: 创建新 Git 账号,验证密码正确加密存储
2. **发版测试**: 使用新账号进行发版,验证 Git clone 成功
3. **旧数据兼容**: 验证使用旧 `Project.gitPassword` 的服务仍能正常发版
4. **特殊字符测试**: 验证包含特殊字符 (@、:、#、%) 的密码能正确处理

## 安全建议

1. ✅ **已实现**: 密码在数据库中加密存储
2. ✅ **已实现**: API 响应中不返回密码 (`@JsonProperty(access = WRITE_ONLY)`)
3. ⚠️ **建议**: 日志中屏蔽敏感信息 (不要打印明文密码)
4. ⚠️ **建议**: 定期更换加密密钥并重新加密现有密码

## 相关文档

- `2026-02-13-create-git-account-table.sql` - Git账号表创建
- `2026-02-23-fix-git-auth-from-subproject.md` - Git认证来源修复
