package com.opster.module.service.service.impl;

import com.opster.common.LocalBuildLogger;
import com.opster.common.LocalCommandUtils;
import com.opster.common.LocalDeploymentLogger;
import com.opster.common.enums.RepositoryType;
import com.opster.config.OpsterProperties;
import com.opster.module.project.entity.Project;
import com.opster.module.project.repository.ProjectRepository;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.repository.AppServiceRepository;
import com.opster.module.service.service.AppServiceService;
import com.opster.module.service.service.LocalBuildService;
import com.opster.module.service.service.NodeVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 本地打包服务实现类
 * 负责在本地执行Git拉取、Maven/npm构建、产物归档等操作
 */
@Slf4j
@Service
public class LocalBuildServiceImpl implements LocalBuildService {

    @Autowired
    private OpsterProperties opsterProperties;

    @Autowired
    private NodeVersionService nodeVersionService;

    @Autowired
    private AppServiceService appServiceService;

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public Path buildArtifact(Integer serviceId, String projectCode,
                             String serviceAlias,
                             RepositoryType repositoryType,
                             String gitUrl,
                             String gitBranch,
                             String buildCmd,
                             String projectPath,
                             WebSocketSession wsSession,
                             String username,
                             String password,
                             String nodeVersion,
                             LocalDeploymentLogger deploymentLogger) throws Exception {
        // 根据仓库类型选择构建方式
        if (repositoryType == RepositoryType.FRONTEND ||
            repositoryType == RepositoryType.MOBILE) {
            // 前端或移动端项目使用npm构建，传递 nodeVersion 参数
            return buildNpmArtifact(serviceId, projectCode, serviceAlias, gitUrl, gitBranch, buildCmd, projectPath, wsSession, username, password, nodeVersion, deploymentLogger);
        } else {
            // 后端或管理后台项目使用Maven构建，nodeVersion 参数作为 JDK 版本传递
            return buildMavenArtifact(serviceId, projectCode, serviceAlias, gitUrl, gitBranch, buildCmd, projectPath, wsSession, username, password, nodeVersion, deploymentLogger);
        }
    }

    @Override
    public Path buildMavenArtifact(Integer serviceId, String projectCode,
                                   String serviceAlias,
                                   String gitUrl,
                                   String gitBranch,
                                   String mavenCmd,
                                   String projectPath,
                                   WebSocketSession wsSession,
                                   String username,
                                   String password,
                                   String jdkVersion,
                                   LocalDeploymentLogger deploymentLogger) throws Exception {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        // 1. 创建日志记录器
        Path logsDir = getLogsDir(projectCode, serviceAlias);
        LocalBuildLogger logger = LocalBuildLogger.create(
            logsDir.toString(),
            "build-maven-" + timestamp + ".log",
            wsSession
        );

        // 如果提供了发版日志记录器，创建代理logger同时写入两个日志文件
        LocalBuildLogger actualLogger = logger;
        if (deploymentLogger != null) {
            actualLogger = new ProxyBuildLogger(logger, deploymentLogger);
        }

        try {
            actualLogger.info("=== 开始Maven项目打包 ===");
            actualLogger.info("项目编码: " + projectCode);
            actualLogger.info("服务别名: " + serviceAlias);
            actualLogger.info("Git地址: " + gitUrl);
            actualLogger.info("Git分支: " + gitBranch);
            actualLogger.info("Maven命令: " + mavenCmd);
            if (jdkVersion != null && !jdkVersion.isEmpty()) {
                actualLogger.info("JDK版本: " + jdkVersion);
            }

            // 2. 准备源码目录：固定公式 opster.deploy-path + 项目编码 + source
            Path sourceDir = getSourceDir(projectCode, serviceAlias);
            actualLogger.info("源码目录: " + sourceDir);
            LocalCommandUtils.createDirectories(sourceDir);

            // 3. 准备编译目录：源码目录 + 编译路径（如果编译路径为空则使用源码目录）
            Path buildDir;
            if (serviceId != null) {
                Optional<AppService> serviceOpt = appServiceRepository.findById(serviceId);
                if (serviceOpt.isPresent() && serviceOpt.get().getCompilePath() != null && !serviceOpt.get().getCompilePath().isEmpty()) {
                    // 使用数据库中配置的编译路径
                    String compilePath = serviceOpt.get().getCompilePath();
                    // 防止路径穿越攻击
                    if (compilePath.contains("..")) {
                        actualLogger.error("编译路径包含非法字符: " + compilePath);
                        throw new Exception("Invalid compile path");
                    }
                    buildDir = sourceDir.resolve(compilePath);
                    actualLogger.info("编译目录: " + buildDir);
                } else {
                    // 编译路径为空，使用源码目录作为编译目录
                    buildDir = sourceDir;
                    actualLogger.info("编译路径为空，使用源码目录: " + buildDir);
                }
            } else {
                // 兼容旧逻辑：如果没有 serviceId，使用 projectPath
                if (cn.hutool.core.util.StrUtil.isNotBlank(projectPath)) {
                    if (projectPath.contains("..")) {
                        actualLogger.error("项目路径包含非法字符: " + projectPath);
                        throw new Exception("Invalid project path");
                    }
                    buildDir = sourceDir.resolve(projectPath);
                    actualLogger.info("编译目录（使用 projectPath）: " + buildDir);
                } else {
                    // projectPath 也为空，使用源码目录
                    buildDir = sourceDir;
                    actualLogger.info("编译路径为空，使用源码目录: " + buildDir);
                }
            }
            LocalCommandUtils.createDirectories(buildDir);

            // 4. Git操作（在 sourceDir 源码目录下执行 clone 或 pull）
            actualLogger.info(">>> 开始Git操作...");
            actualLogger.info(">>> Git操作目录（源码目录）: " + sourceDir);
            boolean gitSuccess = performGitOperation(sourceDir, gitUrl, gitBranch, wsSession, actualLogger, username, password);
            if (!gitSuccess) {
                actualLogger.error("Git操作失败");
                throw new Exception("Git operation failed");
            }

            // 5. 项目根目录 = 编译目录
            Path projectRoot = buildDir;
            actualLogger.info("项目根目录（编译目录）: " + projectRoot);

            // 5. Maven打包
            actualLogger.info(">>> 开始Maven打包...");
            // 如果 mavenCmd 为空，使用默认命令
            String actualMavenCmd = mavenCmd;
            if (cn.hutool.core.util.StrUtil.isBlank(actualMavenCmd)) {
                actualMavenCmd = "mvn clean package -DskipTests";
                actualLogger.info("Maven命令为空，使用默认命令: " + actualMavenCmd);
            }
            boolean mavenSuccess = executeMavenBuild(projectRoot, actualMavenCmd, actualMavenCmd, wsSession, actualLogger);
            if (!mavenSuccess) {
                actualLogger.error("Maven打包失败");
                throw new Exception("Maven build failed");
            }

            // 6. 查找打包产物
            Path jarFile = findMavenArtifact(projectRoot);
            if (jarFile == null) {
                actualLogger.error("未找到打包产物（jar文件）");
                throw new Exception("Maven artifact not found");
            }

            actualLogger.info("打包产物: " + jarFile);

            // 7. 归档产物（保持原始文件名，添加时间戳前缀避免冲突）
            Path artifactsDir = getArtifactsDir(projectCode, serviceAlias);
            LocalCommandUtils.createDirectories(artifactsDir);

            String originalJarName = jarFile.getFileName().toString();
            String artifactName = timestamp + "_" + originalJarName; // 例如: 20260131165555_eip-backend-1.0.0.jar
            Path artifactPath = artifactsDir.resolve(artifactName);

            Files.copy(jarFile, artifactPath, StandardCopyOption.REPLACE_EXISTING);
            actualLogger.info("产物已归档到: " + artifactPath);

            actualLogger.info("=== Maven项目打包完成 ===");
            return artifactPath;

        } catch (Exception e) {
            actualLogger.error("Maven打包失败", e);
            throw e;
        }
    }

    @Override
    public Path buildNpmArtifact(Integer serviceId, String projectCode,
                                String serviceAlias,
                                String gitUrl,
                                String gitBranch,
                                String buildCmd,
                                String projectPath,
                                WebSocketSession wsSession,
                                String username,
                                String password,
                                String nodeVersion,
                                LocalDeploymentLogger deploymentLogger) throws Exception {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        // 1. 创建日志记录器
        Path logsDir = getLogsDir(projectCode, serviceAlias);
        LocalBuildLogger logger = LocalBuildLogger.create(
            logsDir.toString(),
            "build-npm-" + timestamp + ".log",
            wsSession
        );

        // 如果提供了发版日志记录器，创建代理logger同时写入两个日志文件
        LocalBuildLogger actualLogger = logger;
        if (deploymentLogger != null) {
            actualLogger = new ProxyBuildLogger(logger, deploymentLogger);
        }

        try {
            actualLogger.info("=== 开始npm项目打包 ===");
            actualLogger.info("项目编码: " + projectCode);
            actualLogger.info("服务别名: " + serviceAlias);
            actualLogger.info("Git地址: " + gitUrl);
            actualLogger.info("Git分支: " + gitBranch);
            actualLogger.info("构建命令: " + buildCmd);

            // 2. 准备源码目录：固定公式 opster.deploy-path + 项目编码 + source
            Path sourceDir = getSourceDir(projectCode, serviceAlias);
            actualLogger.info("源码目录: " + sourceDir);
            LocalCommandUtils.createDirectories(sourceDir);

            // 3. 准备编译目录：源码目录 + 编译路径（如果编译路径为空则使用源码目录）
            Path buildDir;
            if (serviceId != null) {
                Optional<AppService> serviceOpt = appServiceRepository.findById(serviceId);
                if (serviceOpt.isPresent() && serviceOpt.get().getCompilePath() != null && !serviceOpt.get().getCompilePath().isEmpty()) {
                    // 使用数据库中配置的编译路径
                    String compilePath = serviceOpt.get().getCompilePath();
                    // 防止路径穿越攻击
                    if (compilePath.contains("..")) {
                        actualLogger.error("编译路径包含非法字符: " + compilePath);
                        throw new Exception("Invalid compile path");
                    }
                    buildDir = sourceDir.resolve(compilePath);
                    actualLogger.info("编译目录: " + buildDir);
                } else {
                    // 编译路径为空，使用源码目录作为编译目录
                    buildDir = sourceDir;
                    actualLogger.info("编译路径为空，使用源码目录: " + buildDir);
                }
            } else {
                // 兼容旧逻辑：如果没有 serviceId，使用 projectPath
                if (cn.hutool.core.util.StrUtil.isNotBlank(projectPath)) {
                    if (projectPath.contains("..")) {
                        actualLogger.error("项目路径包含非法字符: " + projectPath);
                        throw new Exception("Invalid project path");
                    }
                    buildDir = sourceDir.resolve(projectPath);
                    actualLogger.info("编译目录（使用 projectPath）: " + buildDir);
                } else {
                    // projectPath 也为空，使用源码目录
                    buildDir = sourceDir;
                    actualLogger.info("编译路径为空，使用源码目录: " + buildDir);
                }
            }
            LocalCommandUtils.createDirectories(buildDir);

            // 4. Git操作（在 sourceDir 源码目录下执行 clone 或 pull）
            actualLogger.info(">>> 开始Git操作...");
            actualLogger.info(">>> Git操作目录（源码目录）: " + sourceDir);
            boolean gitSuccess = performGitOperation(sourceDir, gitUrl, gitBranch, wsSession, actualLogger, username, password);
            if (!gitSuccess) {
                actualLogger.error("Git操作失败");
                throw new Exception("Git operation failed");
            }

            // 5. 项目根目录 = 编译目录
            Path projectRoot = buildDir;
            actualLogger.info("项目根目录（编译目录）: " + projectRoot);

            // ========== Node.js 版本管理 ==========
            String nodeBinDir = null;  // 用于存储 Node.js bin 目录路径
            if (nodeVersion != null && !nodeVersion.isEmpty() && opsterProperties.getNodejs().getEnabled()) {
                actualLogger.info(">>> 检查 Node.js 版本: " + nodeVersion);

                // 验证版本格式
                if (!nodeVersionService.isValidVersionFormat(nodeVersion)) {
                    throw new Exception("无效的 Node.js 版本号格式: " + nodeVersion + "，正确格式应为：v18.17.0");
                }

                // 检查版本是否已安装
                if (!nodeVersionService.isVersionInstalled(nodeVersion)) {
                    if (opsterProperties.getNodejs().getAutoInstall()) {
                        actualLogger.info(">>> Node.js 版本 " + nodeVersion + " 未安装，开始自动安装...");
                        boolean installed = nodeVersionService.installVersion(nodeVersion);
                        if (!installed) {
                            throw new Exception("Node.js 版本 " + nodeVersion + " 安装失败");
                        }
                        actualLogger.info(">>> Node.js 版本 " + nodeVersion + " 安装成功");
                    } else {
                        throw new Exception("Node.js 版本 " + nodeVersion + " 未安装且自动安装已禁用");
                    }
                }

                // 获取 Node.js 路径
                String nodePath = nodeVersionService.getNodePath(nodeVersion);
                actualLogger.info(">>> 使用 Node.js 版本: " + nodeVersion + " (" + nodePath + ")");

                // 获取 bin 目录（node 可执行文件的父目录）
                File nodeFile = new File(nodePath);
                nodeBinDir = nodeFile.getParent();
                actualLogger.info(">>> Node.js bin 目录: " + nodeBinDir);
            } else {
                actualLogger.info(">>> 使用系统默认 Node.js 版本");
            }
            // ==========================================

            // 6. npm安装依赖
            actualLogger.info(">>> 开始npm install...");
            boolean npmInstallSuccess = executeNpmInstall(projectRoot, nodeBinDir, wsSession, actualLogger);
            if (!npmInstallSuccess) {
                actualLogger.warn("npm install失败，但继续尝试构建");
            }

            // 6.5. 检查package.json中的可用脚本(帮助用户配置正确的构建命令)
            logAvailableNpmScripts(projectRoot, actualLogger);

            // 7. npm打包
            actualLogger.info(">>> 开始npm构建...");
            // 如果 buildCmd 为空，使用默认命令
            String actualBuildCmd = buildCmd;
            if (cn.hutool.core.util.StrUtil.isBlank(actualBuildCmd)) {
                actualBuildCmd = "npm run build";
                actualLogger.info("构建命令为空，使用默认命令: " + actualBuildCmd);
            }
            boolean buildSuccess = executeNpmBuild(projectRoot, actualBuildCmd, nodeBinDir, wsSession, actualLogger);
            if (!buildSuccess) {
                actualLogger.error("npm构建失败");
                throw new Exception("npm build failed");
            }

            // 7. 查找打包产物
            Path distDir = findNpmDistDir(projectRoot);
            if (distDir == null || !Files.exists(distDir)) {
                actualLogger.error("未找到打包产物（dist目录）");
                throw new Exception("npm dist directory not found");
            }

            actualLogger.info("打包产物目录: " + distDir);

            // 8. 压缩dist目录为zip
            Path artifactsDir = getArtifactsDir(projectCode, serviceAlias);
            LocalCommandUtils.createDirectories(artifactsDir);

            String artifactName = "app-frontend-" + timestamp + ".zip";
            Path artifactPath = artifactsDir.resolve(artifactName);

            zipDirectory(distDir, artifactPath);
            actualLogger.info("产物已归档到: " + artifactPath);

            actualLogger.info("=== npm项目打包完成 ===");
            return artifactPath;

        } catch (Exception e) {
            actualLogger.error("npm打包失败", e);
            throw e;
        }
    }

    @Override
    public void cleanupOldArtifacts(String projectCode, String serviceAlias, int keepVersions) {
        try {
            Path artifactsDir = getArtifactsDir(projectCode, serviceAlias);
            if (!LocalCommandUtils.directoryExists(artifactsDir)) {
                return;
            }

            // 获取所有产物文件并按修改时间排序
            try (Stream<Path> paths = Files.list(artifactsDir)) {
                paths.filter(Files::isRegularFile)
                     .sorted((p1, p2) -> {
                         try {
                             long time1 = Files.getLastModifiedTime(p1).toMillis();
                             long time2 = Files.getLastModifiedTime(p2).toMillis();
                             return Long.compare(time2, time1); // 降序排列
                         } catch (Exception e) {
                             return 0;
                         }
                     })
                     .skip(keepVersions)
                     .forEach(oldFile -> {
                         try {
                             Files.delete(oldFile);
                             log.info("Deleted old artifact: {}", oldFile);
                         } catch (Exception e) {
                             log.warn("Failed to delete old artifact: {}", oldFile, e);
                         }
                     });
            }

        } catch (Exception e) {
            log.error("Error cleaning up old artifacts for project: {}", projectCode, e);
        }
    }

    @Override
    public Path getSourceDir(String projectCode, String serviceAlias) {
        // 源码目录：opster.deploy-path + 项目编码 + source
        String deployPath = opsterProperties.getDeployPath();
        return Paths.get(deployPath, projectCode, "source");
    }

    @Override
    public Path getArtifactsDir(String projectCode, String serviceAlias) {
        // 统一使用项目级别的 artifacts 目录
        String deployPath = opsterProperties.getDeployPath();
        return Paths.get(deployPath, projectCode, "artifacts");
    }

    @Override
    public Path getLogsDir(String projectCode, String serviceAlias) {
        // 统一使用项目级别的 logs 目录
        String deployPath = opsterProperties.getDeployPath();
        return Paths.get(deployPath, projectCode, "logs");
    }

    @Override
    public Path getLatestBuildLogFile(String projectCode, String serviceAlias, RepositoryType repositoryType) {
        try {
            Path logsDir = getLogsDir(projectCode, serviceAlias);
            if (!LocalCommandUtils.directoryExists(logsDir)) {
                return null;
            }

            // 确定日志文件前缀
            String logPrefix;
            if (repositoryType == RepositoryType.FRONTEND || repositoryType == RepositoryType.MOBILE) {
                logPrefix = "build-npm-";
            } else {
                logPrefix = "build-maven-";
            }

            // 查找最新的日志文件（按修改时间排序）
            try (var paths = Files.list(logsDir)) {
                return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(logPrefix) && p.getFileName().toString().endsWith(".log"))
                    .max((p1, p2) -> {
                        try {
                            long time1 = Files.getLastModifiedTime(p1).toMillis();
                            long time2 = Files.getLastModifiedTime(p2).toMillis();
                            return Long.compare(time1, time2); // 返回最新的
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .orElse(null);
            }
        } catch (Exception e) {
            log.error("获取最新构建日志文件失败", e);
            return null;
        }
    }

    /**
     * 获取编译目录
     * 公式：源码目录 + 编译路径
     * 源码目录 = opster.deploy-path + 项目编码 + source
     *
     * @param serviceId 服务ID
     * @return 编译目录，如果服务不存在或未配置编译路径则返回null
     */
    public Path getCompilePathByServiceId(Integer serviceId) {
        if (serviceId == null) {
            return null;
        }

        Optional<AppService> serviceOpt = appServiceRepository.findById(serviceId);
        if (serviceOpt.isPresent()) {
            AppService service = serviceOpt.get();
            if (service.getCompilePath() != null && !service.getCompilePath().isEmpty()) {
                // 获取项目编码以计算源码目录
                String projectCode = projectRepository.findById(service.getProjectId())
                    .map(Project::getProjectCode)
                    .orElse(null);
                if (projectCode == null) {
                    log.warn("服务 {} 关联的项目不存在", serviceId);
                    return null;
                }
                // 源码目录 = opster.deploy-path + 项目编码 + source
                Path sourceDir = getSourceDir(projectCode, null);
                // 编译目录 = 源码目录 + 编译路径
                return sourceDir.resolve(service.getCompilePath());
            }
        }

        return null;
    }

    /**
     * 执行Git操作（clone或pull）
     *
     * @param sourceDir 源码目录
     * @param gitUrl Git仓库地址
     * @param gitBranch Git分支
     * @param wsSession WebSocket会话
     * @param logger 日志记录器
     * @param username Git认证用户名（可选）
     * @param password Git认证密码（可选）
     * @return 操作是否成功
     */
    private boolean performGitOperation(Path sourceDir, String gitUrl, String gitBranch,
                                       WebSocketSession wsSession, LocalBuildLogger logger,
                                       String username, String password) {
        try {
            // 如果提供了用户名和密码，构建带认证的Git URL
            String authenticatedUrl = buildAuthenticatedGitUrl(gitUrl, username, password);

            if (LocalCommandUtils.directoryExists(sourceDir.resolve(".git"))) {
                // 目录已存在，执行pull
                logger.info("检测到Git仓库已存在，执行git pull...");

                // 先更新远程仓库 URL（如果需要认证）
                if (!authenticatedUrl.equals(gitUrl)) {
                    logger.info("更新远程仓库 URL 以包含认证信息");
                    String setUrlCmd = String.format("git remote set-url origin %s", authenticatedUrl);
                    LocalCommandUtils.executeCommand(sourceDir, setUrlCmd, wsSession, null, logger);
                }

                // 方案1：先重置未提交的更改，再 pull
                // 强制重置到 origin/<branch>，丢弃本地更改
                String resetCmd = String.format("git reset --hard origin/%s || git reset --hard && git checkout %s",
                    gitBranch, gitBranch);
                boolean resetResult = LocalCommandUtils.executeCommand(sourceDir, resetCmd, wsSession, null, logger);

                // 然后拉取最新代码
                String pullCmd = String.format("git pull origin %s", gitBranch);
                boolean pullResult = LocalCommandUtils.executeCommand(sourceDir, pullCmd, wsSession, null, logger);

                boolean result = resetResult && pullResult;
                if (!result) {
                    logger.error("git pull 命令执行失败");
                }
                return result;
            } else {
                // 目录不存在，执行clone
                logger.info("Git仓库不存在，执行git clone...");
                // 清空source目录
                if (LocalCommandUtils.directoryExists(sourceDir)) {
                    deleteDirectory(sourceDir);
                }
                LocalCommandUtils.createDirectories(sourceDir);

                String cloneCmd = String.format("git clone --depth 1 -b %s %s .", gitBranch, authenticatedUrl);
                boolean result = LocalCommandUtils.executeCommand(sourceDir, cloneCmd, wsSession, null, logger);
                if (!result) {
                    logger.error("git clone 命令执行失败");
                }
                return result;
            }
        } catch (Exception e) {
            logger.error("Git操作失败", e);
            return false;
        }
    }

    /**
     * 构建带认证的Git URL
     *
     * @param gitUrl 原始Git URL
     * @param username 用户名（可选）
     * @param password 密码（可选）
     * @return 带认证的Git URL，如果未提供认证信息则返回原始URL
     */
    private String buildAuthenticatedGitUrl(String gitUrl, String username, String password) {
        // 如果没有提供用户名和密码，返回原始URL
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return gitUrl;
        }

        try {
            // 只对HTTP/HTTPS URL添加认证信息
            if (gitUrl.startsWith("http://") || gitUrl.startsWith("https://")) {
                // 解析URL并插入认证信息
                // 例如: https://git.example.com/repo.git
                // 变为: https://username:password@git.example.com/repo.git

                String protocol = gitUrl.contains("https://") ? "https://" : "http://";
                String restOfUrl = gitUrl.substring(protocol.length());

                // 对用户名和密码进行 URL 编码，处理特殊字符（@、:、#、% 等）
                String encodedUsername = java.net.URLEncoder.encode(username, "UTF-8");
                String encodedPassword = java.net.URLEncoder.encode(password, "UTF-8");

                // 构建带认证的URL
                String authenticatedUrl = protocol + encodedUsername + ":" + encodedPassword + "@" + restOfUrl;
                log.info("构建认证 Git URL（用户名和密码已编码）");
                return authenticatedUrl;
            }

            // SSH URL不需要在这里处理认证（应该使用SSH密钥）
            return gitUrl;
        } catch (Exception e) {
            log.error("Error building authenticated Git URL", e);
            return gitUrl;
        }
    }

    /**
     * 执行Maven构建
     * @param projectRoot 项目根目录
     * @param mavenCmd Maven命令
     * @param jdkVersion JDK版本（jdk8、jdk17 或 null）
     * @param wsSession WebSocket会话
     * @param logger 日志记录器
     */
    private boolean executeMavenBuild(Path projectRoot, String mavenCmd, String jdkVersion,
                                     WebSocketSession wsSession, LocalBuildLogger logger) {
        // 设置环境变量
        String mavenHome = opsterProperties.getMavenHome();
        String javaHome;

        // 根据 jdkVersion 选择 JAVA_HOME
        if ("jdk8".equals(jdkVersion)) {
            javaHome = opsterProperties.getJdk().getJdk8();
            logger.info("使用 JDK 8: " + javaHome);
        } else if ("jdk17".equals(jdkVersion)) {
            javaHome = opsterProperties.getJdk().getJdk17();
            logger.info("使用 JDK 17: " + javaHome);
        } else {
            // 默认使用配置的 java-home
            javaHome = opsterProperties.getJavaHome();
            logger.info("使用默认 JDK: " + javaHome);
        }

        // 修正macOS上的JAVA_HOME路径
        // macOS的JDK目录结构: jdk-25.jdk/Contents/Home
        String correctedJavaHome = javaHome;
        if (javaHome != null && javaHome.endsWith(".jdk")) {
            Path contentsHome = Paths.get(javaHome, "Contents", "Home");
            if (Files.exists(contentsHome)) {
                correctedJavaHome = contentsHome.toString();
                logger.info("检测到macOS JDK目录，自动修正JAVA_HOME: " + correctedJavaHome);
            }
        }

        // 获取系统原有的PATH环境变量
        String systemPath = System.getenv("PATH");
        if (systemPath == null || systemPath.isEmpty()) {
            systemPath = "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin";
        }

        // 修正 M2_HOME 和 PATH（如果配置已包含 /bin，需要去除）
        String mavenHomeForEnv = mavenHome.endsWith("/bin") ?
            mavenHome.substring(0, mavenHome.length() - 4) : mavenHome;

        String[] envVars = {
            "JAVA_HOME=" + correctedJavaHome,
            "M2_HOME=" + mavenHomeForEnv,
            "PATH=" + mavenHomeForEnv + "/bin:" + correctedJavaHome + "/bin:" + systemPath
        };

        logger.info("JAVA_HOME: " + correctedJavaHome);
        logger.info("M2_HOME: " + mavenHomeForEnv);

        // 如果 mavenCmd 为空，使用默认命令
        String actualMavenCmd = mavenCmd;
        if (cn.hutool.core.util.StrUtil.isBlank(actualMavenCmd)) {
            actualMavenCmd = "mvn clean package -DskipTests";
            logger.info("Maven命令为空，使用默认命令");
        }

        logger.info("Maven命令: " + actualMavenCmd);
        logger.info("使用PATH中的Maven: " + mavenHomeForEnv + "/bin/mvn");

        // 构建完整的 Maven 命令，添加详细输出参数
        // -B: batch mode（批处理模式，输出更详细的进度信息）
        // -e: 显示完整的错误堆栈信息
        String fullMavenCmd = actualMavenCmd;
        if (!actualMavenCmd.contains("-B")) {
            fullMavenCmd = actualMavenCmd + " -B";
        }
        if (!actualMavenCmd.contains("-e")) {
            fullMavenCmd = fullMavenCmd + " -e";
        }

        logger.info("执行完整Maven命令: " + fullMavenCmd);

        // 传递 logger 参数以记录命令输出
        // 使用增强后的 Maven 命令以获取更详细的编译输出
        return LocalCommandUtils.executeCommand(projectRoot, fullMavenCmd, wsSession, envVars, logger);
    }

    /**
     * 查找Maven打包产物（jar文件）
     * 支持单模块和多模块项目，递归查找所有子模块的 target 目录
     */
    private Path findMavenArtifact(Path projectRoot) {
        // 1. 首先尝试在根目录的 target 中查找（单模块项目）
        Path rootTargetDir = projectRoot.resolve("target");
        if (Files.exists(rootTargetDir)) {
            Path jarInRoot = findJarInTargetDir(rootTargetDir);
            if (jarInRoot != null) {
                log.info("在根目录 target 中找到 jar 文件: {}", jarInRoot);
                return jarInRoot;
            }
        }

        // 2. 如果根目录未找到，递归查找所有子模块的 target 目录（多模块项目）
        log.info("根目录 target 中未找到 jar 文件，开始递归查找子模块...");
        try (Stream<Path> paths = Files.walk(projectRoot, 4)) {
            return paths
                .filter(Files::isDirectory)
                .filter(dir -> dir.getFileName().toString().equals("target"))
                .map(this::findJarInTargetDir)
                .filter(jar -> jar != null)
                .max(Comparator.comparingLong(file -> {
                    try {
                        return Files.size(file);
                    } catch (Exception e) {
                        return 0L;
                    }
                }))
                .orElse(null);
        } catch (Exception e) {
            log.error("递归查找 jar 文件时发生错误", e);
            return null;
        }
    }

    /**
     * 在指定的 target 目录中查找 jar 文件
     *
     * @param targetDir target 目录路径
     * @return 找到的 jar 文件路径，如果没有找到则返回 null
     */
    private Path findJarInTargetDir(Path targetDir) {
        if (!Files.exists(targetDir)) {
            return null;
        }

        try (Stream<Path> paths = Files.list(targetDir)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".jar"))
                .filter(p -> !p.getFileName().toString().contains("-sources.jar"))
                .filter(p -> !p.getFileName().toString().contains("-javadoc.jar"))
                .filter(p -> !p.getFileName().toString().contains("-repack.jar")) // Spring Boot repackaged
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            log.error("在目录 {} 中查找 jar 文件时发生错误", targetDir, e);
            return null;
        }
    }

    /**
     * 检查 package.json 是否包含 build 脚本
     */
    private boolean hasBuildScript(Path packageJsonPath) {
        try {
            String content = Files.readString(packageJsonPath);
            // 简单检查是否包含 "build" 脚本
            return content.contains("\"build\"") || content.contains("'build'");
        } catch (Exception e) {
            log.warn("Failed to read package.json: {}", packageJsonPath, e);
            return false;
        }
    }

    /**
     * 执行npm install
     * @param nodeBinDir Node.js bin 目录路径，如果为 null 则使用系统默认
     */
    private boolean executeNpmInstall(Path projectRoot, String nodeBinDir, WebSocketSession wsSession, LocalBuildLogger logger) {
        String[] envVars = buildNodeEnvVars(nodeBinDir);
        // 添加详细输出参数，显示所有依赖安装过程
        String npmInstallCmd = "npm install --loglevel=verbose";
        logger.info("执行npm install命令: " + npmInstallCmd);
        return LocalCommandUtils.executeCommand(projectRoot, npmInstallCmd, wsSession, envVars, logger);
    }

    /**
     * 执行npm构建
     * @param nodeBinDir Node.js bin 目录路径，如果为 null 则使用系统默认
     */
    private boolean executeNpmBuild(Path projectRoot, String buildCmd, String nodeBinDir,
                                   WebSocketSession wsSession, LocalBuildLogger logger) {
        String[] envVars = buildNodeEnvVars(nodeBinDir);

        // 如果 buildCmd 为空，使用默认命令
        String actualBuildCmd = buildCmd;
        if (cn.hutool.core.util.StrUtil.isBlank(actualBuildCmd)) {
            actualBuildCmd = "npm run build";
            logger.info("构建命令为空，使用默认命令: " + actualBuildCmd);
        }

        // 构建完整的 npm 命令，添加详细输出参数
        // 如果构建命令不包含日志级别参数，则自动添加
        String fullBuildCmd = actualBuildCmd;
        if (!actualBuildCmd.contains("--loglevel")) {
            fullBuildCmd = actualBuildCmd + " --loglevel=verbose";
        }
        logger.info("执行npm构建命令: " + fullBuildCmd);

        boolean result = LocalCommandUtils.executeCommand(projectRoot, fullBuildCmd, wsSession, envVars, logger);

        // 如果构建失败,尝试提供诊断信息
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

            // 读取 package.json 内容
            String content = Files.readString(packageJsonPath);

            // 使用简单的文本解析提取 scripts (避免引入 JSON 解析依赖)
            int scriptsIndex = content.indexOf("\"scripts\"");
            if (scriptsIndex == -1) {
                logger.warn("package.json 中未定义 scripts");
                return;
            }

            // 提取 scripts 对象的内容 (从 { 到对应的 })
            int scriptsStart = content.indexOf("{", scriptsIndex);
            if (scriptsStart == -1) {
                return;
            }

            // 简单提取: 找到所有 "key": "value" 格式
            java.util.List<String> availableScripts = new java.util.ArrayList<>();
            int searchPos = scriptsStart + 1;
            int braceCount = 1;

            while (searchPos < content.length() && braceCount > 0) {
                char c = content.charAt(searchPos);
                if (c == '{') braceCount++;
                if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) break;
                }

                // 查找 "scriptname": 格式
                if (c == '"') {
                    int nextQuote = content.indexOf('"', searchPos + 1);
                    if (nextQuote != -1 && content.charAt(nextQuote + 1) == ':') {
                        String scriptName = content.substring(searchPos + 1, nextQuote);
                        // 跳过特殊脚本 (pre/post 开头的)
                        if (!scriptName.startsWith("pre") && !scriptName.startsWith("post")) {
                            availableScripts.add(scriptName);
                        }
                        searchPos = nextQuote + 1;
                    }
                }
                searchPos++;
            }

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

    /**
     * 构建 Node.js 环境变量
     * @param nodeBinDir Node.js bin 目录路径，如果为 null 则返回 null
     * @return 环境变量数组，将 Node.js bin 目录添加到 PATH 前面
     */
    private String[] buildNodeEnvVars(String nodeBinDir) {
        if (nodeBinDir == null || nodeBinDir.isEmpty()) {
            return null;
        }

        // 获取系统 PATH
        String systemPath = System.getenv("PATH");

        // 将指定版本的 Node.js bin 目录添加到 PATH 最前面
        // 这样执行 npm/node 时会优先使用指定版本
        String newPath = nodeBinDir + ":" + systemPath;

        return new String[]{"PATH=" + newPath};
    }

    /**
     * 查找npm打包产物目录（dist或build）
     */
    private Path findNpmDistDir(Path projectRoot) {
        Path distDir = projectRoot.resolve("dist");
        if (Files.exists(distDir)) {
            return distDir;
        }

        Path buildDir = projectRoot.resolve("build");
        if (Files.exists(buildDir)) {
            return buildDir;
        }

        return null;
    }

    /**
     * 压缩目录为zip文件
     */
    private void zipDirectory(Path sourceDir, Path zipFile) throws Exception {
        // 使用系统命令压缩（Unix/Mac使用zip命令，Windows使用PowerShell）
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        Process process;
        int exitCode;

        if (isWindows) {
            // Windows使用PowerShell压缩
            String cmd = String.format(
                "Compress-Archive -Path '%s/*' -DestinationPath '%s' -Force",
                sourceDir, zipFile
            );
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", cmd);
            pb.redirectErrorStream(true);
            process = pb.start();
            exitCode = process.waitFor();
        } else {
            // Unix/Mac使用zip命令
            String cmd = String.format("cd %s && zip -rq %s .", sourceDir, zipFile);
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
            pb.redirectErrorStream(true);
            process = pb.start();
            exitCode = process.waitFor();
        }

        // 检查进程退出码
        if (exitCode != 0) {
            // 读取错误输出
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder errorOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            reader.close();

            throw new Exception(String.format(
                "压缩命令执行失败，退出码: %d，错误信息: %s",
                exitCode, errorOutput.toString()
            ));
        }

        // 验证压缩文件是否生成成功
        if (!Files.exists(zipFile) || !Files.isRegularFile(zipFile)) {
            throw new Exception("压缩文件生成失败: " + zipFile);
        }

        // 验证压缩文件大小（至少应该大于1KB）
        long fileSize = Files.size(zipFile);
        if (fileSize < 1024) {
            throw new Exception(String.format(
                "压缩文件大小异常: %d bytes，文件可能损坏: %s",
                fileSize, zipFile
            ));
        }

        log.info("Compressed {} to {} ({} bytes)", sourceDir, zipFile, fileSize);
    }

    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(Path directory) throws Exception {
        if (Files.exists(directory)) {
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder())
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (Exception e) {
                             log.warn("Failed to delete: {}", path);
                         }
                     });
            }
        }
    }

    /**
     * 代理日志记录器
     * 同时将日志写入构建日志文件和发版日志文件
     */
    private static class ProxyBuildLogger extends LocalBuildLogger {
        private final LocalBuildLogger buildLogger;
        private final LocalDeploymentLogger deploymentLogger;

        public ProxyBuildLogger(LocalBuildLogger buildLogger, LocalDeploymentLogger deploymentLogger) {
            // 传递一个虚拟的日志文件路径和 WebSocket session，实际不会使用
            super(buildLogger.getLogFile(), null);
            this.buildLogger = buildLogger;
            this.deploymentLogger = deploymentLogger;
        }

        @Override
        public void log(String message) {
            // 同时写入两个日志
            buildLogger.log(message);
            deploymentLogger.log(message);
        }

        @Override
        public void info(String message) {
            buildLogger.info(message);
            deploymentLogger.log(message);
        }

        @Override
        public void error(String message) {
            buildLogger.error(message);
            deploymentLogger.log(message);
        }

        @Override
        public void warn(String message) {
            buildLogger.warn(message);
            deploymentLogger.log(message);
        }

        @Override
        public void error(String message, Exception e) {
            buildLogger.error(message, e);
            deploymentLogger.log(message + " - " + e.getMessage());
        }
    }
}
