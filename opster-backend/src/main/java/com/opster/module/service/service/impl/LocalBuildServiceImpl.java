package com.opster.module.service.service.impl;

import com.opster.common.LocalBuildLogger;
import com.opster.common.LocalCommandUtils;
import com.opster.common.LocalDeploymentLogger;
import com.opster.common.enums.RepositoryType;
import com.opster.config.OpsterProperties;
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

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public Path buildArtifact(String projectCode,
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
            return buildNpmArtifact(projectCode, serviceAlias, gitUrl, gitBranch, buildCmd, projectPath, wsSession, username, password, nodeVersion, deploymentLogger);
        } else {
            // 后端或管理后台项目使用Maven构建，nodeVersion 参数作为 JDK 版本传递
            return buildMavenArtifact(projectCode, serviceAlias, gitUrl, gitBranch, buildCmd, projectPath, wsSession, username, password, nodeVersion, deploymentLogger);
        }
    }

    @Override
    public Path buildMavenArtifact(String projectCode,
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

            // 2. 准备工作目录
            Path sourceDir = getUniqueSourceDir(projectCode, serviceAlias, gitUrl);
            LocalCommandUtils.createDirectories(sourceDir);

            // 3. 处理项目路径（在 source 目录下按 projectPath 创建子目录）
            Path buildDir = sourceDir;
            if (cn.hutool.core.util.StrUtil.isNotBlank(projectPath)) {
                // 防止路径穿越攻击
                if (projectPath.contains("..")) {
                    actualLogger.error("项目路径包含非法字符: " + projectPath);
                    throw new Exception("Invalid project path");
                }
                buildDir = sourceDir.resolve(projectPath);
                LocalCommandUtils.createDirectories(buildDir);
                actualLogger.info("创建项目路径子目录: " + buildDir);
            }

            // 4. Git操作（在 buildDir 目录下执行 clone 或 pull）
            actualLogger.info(">>> 开始Git操作...");
            actualLogger.info(">>> 源码目录: " + buildDir);
            boolean gitSuccess = performGitOperation(buildDir, gitUrl, gitBranch, wsSession, actualLogger, username, password);
            if (!gitSuccess) {
                actualLogger.error("Git操作失败");
                throw new Exception("Git operation failed");
            }

            // 5. 查找项目根目录
            Path projectRoot = findMavenProjectRoot(buildDir);
            actualLogger.info("项目根目录: " + projectRoot);

            // 5. Maven打包
            actualLogger.info(">>> 开始Maven打包...");
            boolean mavenSuccess = executeMavenBuild(projectRoot, mavenCmd, mavenCmd, wsSession, actualLogger);
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
    public Path buildNpmArtifact(String projectCode,
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

            // 2. 准备工作目录
            Path sourceDir = getUniqueSourceDir(projectCode, serviceAlias, gitUrl);
            LocalCommandUtils.createDirectories(sourceDir);

            // 3. 处理项目路径（在 source 目录下按 projectPath 创建子目录）
            Path buildDir = sourceDir;
            if (cn.hutool.core.util.StrUtil.isNotBlank(projectPath)) {
                // 防止路径穿越攻击
                if (projectPath.contains("..")) {
                    actualLogger.error("项目路径包含非法字符: " + projectPath);
                    throw new Exception("Invalid project path");
                }
                buildDir = sourceDir.resolve(projectPath);
                LocalCommandUtils.createDirectories(buildDir);
                actualLogger.info("创建项目路径子目录: " + buildDir);
            }

            // 4. Git操作（在 buildDir 目录下执行 clone 或 pull）
            actualLogger.info(">>> 开始Git操作...");
            actualLogger.info(">>> 源码目录: " + buildDir);
            boolean gitSuccess = performGitOperation(buildDir, gitUrl, gitBranch, wsSession, actualLogger, username, password);
            if (!gitSuccess) {
                actualLogger.error("Git操作失败");
                throw new Exception("Git operation failed");
            }

            // 5. 查找package.json（前端项目根目录）
            Path projectRoot = findNpmProjectRoot(buildDir);
            actualLogger.info("项目根目录: " + projectRoot);

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

            // 7. npm打包
            actualLogger.info(">>> 开始npm构建...");
            boolean buildSuccess = executeNpmBuild(projectRoot, buildCmd, nodeBinDir, wsSession, actualLogger);
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
        // 如果没有配置 serviceAlias，使用默认值
        String alias = (cn.hutool.core.util.StrUtil.isNotBlank(serviceAlias)) ? serviceAlias : "service";
        String deployPath = opsterProperties.getDeployPath();
        return Paths.get(deployPath, projectCode, alias, "source");
    }

    /**
     * 获取源码目录（统一使用 source 目录）
     * 同一项目的不同服务通过 serviceAlias + projectPath 字段区分子目录
     *
     * @param projectCode 项目编码
     * @param serviceAlias 服务别名
     * @param gitUrl Git 仓库地址（不再使用，保留参数兼容性）
     * @return 源码目录
     */
    private Path getUniqueSourceDir(String projectCode, String serviceAlias, String gitUrl) {
        // 如果没有配置 serviceAlias，使用默认值
        String alias = (cn.hutool.core.util.StrUtil.isNotBlank(serviceAlias)) ? serviceAlias : "service";
        // 统一使用 source 目录，通过 serviceAlias + projectPath 字段区分不同子项目
        String deployPath = opsterProperties.getDeployPath();
        return Paths.get(deployPath, projectCode, alias, "source");
    }

    @Override
    public Path getArtifactsDir(String projectCode, String serviceAlias) {
        // 如果没有配置 serviceAlias，使用默认值
        String alias = (cn.hutool.core.util.StrUtil.isNotBlank(serviceAlias)) ? serviceAlias : "service";
        String deployPath = opsterProperties.getDeployPath();
        return Paths.get(deployPath, projectCode, alias, "artifacts");
    }

    @Override
    public Path getLogsDir(String projectCode, String serviceAlias) {
        // 如果没有配置 serviceAlias，使用默认值
        String alias = (cn.hutool.core.util.StrUtil.isNotBlank(serviceAlias)) ? serviceAlias : "service";
        String deployPath = opsterProperties.getDeployPath();
        return Paths.get(deployPath, projectCode, alias, "logs");
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
                String pullCmd = String.format("git fetch origin && git checkout %s && git pull origin %s",
                    gitBranch, gitBranch);
                boolean result = LocalCommandUtils.executeCommand(sourceDir, pullCmd, wsSession, null, logger);
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

                String cloneCmd = String.format("git clone -b %s %s .", gitBranch, authenticatedUrl);
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

                // 构建带认证的URL
                return protocol + username + ":" + password + "@" + restOfUrl;
            }

            // SSH URL不需要在这里处理认证（应该使用SSH密钥）
            return gitUrl;
        } catch (Exception e) {
            log.error("Error building authenticated Git URL", e);
            return gitUrl;
        }
    }

    /**
     * 查找Maven项目根目录（包含pom.xml的目录）
     */
    private Path findMavenProjectRoot(Path sourceDir) {
        // 先检查当前目录
        Path pomXml = sourceDir.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            return sourceDir;
        }

        // 检查是否存在与当前目录同名的子目录（常见于 monorepo 结构）
        // 例如：sourceDir = /path/to/backend，检查 /path/to/backend/backend 是否存在
        Path subDirWithSameName = sourceDir.resolve(sourceDir.getFileName().toString());
        if (Files.exists(subDirWithSameName)) {
            Path subPomXml = subDirWithSameName.resolve("pom.xml");
            if (Files.exists(subPomXml)) {
                return subDirWithSameName;
            }
        }

        // 递归查找子目录（最多3层）
        try (Stream<Path> paths = Files.walk(sourceDir, 3)) {
            return paths
                .filter(Files::isDirectory)
                .filter(dir -> Files.exists(dir.resolve("pom.xml")))
                .findFirst()
                .orElse(sourceDir);
        } catch (Exception e) {
            log.warn("Error finding Maven project root", e);
            return sourceDir;
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
        logger.info("Maven命令: " + mavenCmd);
        logger.info("使用PATH中的Maven: " + mavenHomeForEnv + "/bin/mvn");

        // 构建完整的 Maven 命令，添加详细输出参数
        // -B: batch mode（批处理模式，输出更详细的进度信息）
        // -e: 显示完整的错误堆栈信息
        String fullMavenCmd = mavenCmd;
        if (!mavenCmd.contains("-B")) {
            fullMavenCmd = mavenCmd + " -B";
        }
        if (!mavenCmd.contains("-e")) {
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
     * 查找npm项目根目录（包含package.json的目录）
     */
    private Path findNpmProjectRoot(Path sourceDir) {
        // 先检查当前目录
        Path packageJson = sourceDir.resolve("package.json");
        if (Files.exists(packageJson)) {
            return sourceDir;
        }

        // 检查是否存在与当前目录同名的子目录（常见于 monorepo 结构）
        // 例如：sourceDir = /path/to/manage，检查 /path/to/manage/manage 是否存在
        Path subDirWithSameName = sourceDir.resolve(sourceDir.getFileName().toString());
        if (Files.exists(subDirWithSameName)) {
            Path subPackageJson = subDirWithSameName.resolve("package.json");
            if (Files.exists(subPackageJson)) {
                return subDirWithSameName;
            }
        }

        // 递归查找子目录（最多3层）
        try (Stream<Path> paths = Files.walk(sourceDir, 3)) {
            return paths
                .filter(Files::isDirectory)
                .filter(dir -> Files.exists(dir.resolve("package.json")))
                .findFirst()
                .orElse(sourceDir);
        } catch (Exception e) {
            log.warn("Error finding npm project root", e);
            return sourceDir;
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
        // 构建完整的 npm 命令，添加详细输出参数
        // 如果构建命令不包含日志级别参数，则自动添加
        String fullBuildCmd = buildCmd;
        if (!buildCmd.contains("--loglevel")) {
            fullBuildCmd = buildCmd + " --loglevel=verbose";
        }
        logger.info("执行npm构建命令: " + fullBuildCmd);
        return LocalCommandUtils.executeCommand(projectRoot, fullBuildCmd, wsSession, envVars, logger);
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
