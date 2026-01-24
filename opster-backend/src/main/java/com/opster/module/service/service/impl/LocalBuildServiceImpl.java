package com.opster.module.service.service.impl;

import com.opster.common.LocalBuildLogger;
import com.opster.common.LocalCommandUtils;
import com.opster.common.enums.RepositoryType;
import com.opster.config.OpsterProperties;
import com.opster.module.service.service.LocalBuildService;
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

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public Path buildArtifact(String projectCode,
                             RepositoryType repositoryType,
                             String gitUrl,
                             String gitBranch,
                             String buildCmd,
                             WebSocketSession wsSession) throws Exception {
        // 根据仓库类型选择构建方式
        if (repositoryType == RepositoryType.FRONTEND ||
            repositoryType == RepositoryType.MOBILE) {
            // 前端或移动端项目使用npm构建
            return buildNpmArtifact(projectCode, gitUrl, gitBranch, buildCmd, wsSession);
        } else {
            // 后端或管理后台项目使用Maven构建
            return buildMavenArtifact(projectCode, gitUrl, gitBranch, buildCmd, wsSession);
        }
    }

    @Override
    public Path buildMavenArtifact(String projectCode,
                                   String gitUrl,
                                   String gitBranch,
                                   String mavenCmd,
                                   WebSocketSession wsSession) throws Exception {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        // 1. 创建日志记录器
        Path logsDir = getLogsDir(projectCode);
        LocalBuildLogger logger = LocalBuildLogger.create(
            logsDir.toString(),
            "build-maven-" + timestamp + ".log",
            wsSession
        );

        try {
            logger.info("=== 开始Maven项目打包 ===");
            logger.info("项目编码: " + projectCode);
            logger.info("Git地址: " + gitUrl);
            logger.info("Git分支: " + gitBranch);
            logger.info("Maven命令: " + mavenCmd);

            // 2. 准备工作目录
            Path sourceDir = getSourceDir(projectCode);
            LocalCommandUtils.createDirectories(sourceDir);

            // 3. Git操作（clone或pull）
            logger.info(">>> 开始Git操作...");
            boolean gitSuccess = performGitOperation(sourceDir, gitUrl, gitBranch, wsSession, logger);
            if (!gitSuccess) {
                logger.error("Git操作失败");
                throw new Exception("Git operation failed");
            }

            // 4. 查找项目根目录（假设source目录下可能有多个子项目）
            Path projectRoot = findMavenProjectRoot(sourceDir);
            logger.info("项目根目录: " + projectRoot);

            // 5. Maven打包
            logger.info(">>> 开始Maven打包...");
            boolean mavenSuccess = executeMavenBuild(projectRoot, mavenCmd, wsSession, logger);
            if (!mavenSuccess) {
                logger.error("Maven打包失败");
                throw new Exception("Maven build failed");
            }

            // 6. 查找打包产物
            Path jarFile = findMavenArtifact(projectRoot);
            if (jarFile == null) {
                logger.error("未找到打包产物（jar文件）");
                throw new Exception("Maven artifact not found");
            }

            logger.info("打包产物: " + jarFile);

            // 7. 归档产物
            Path artifactsDir = getArtifactsDir(projectCode);
            LocalCommandUtils.createDirectories(artifactsDir);

            String artifactName = "app-backend-" + timestamp + ".jar";
            Path artifactPath = artifactsDir.resolve(artifactName);

            Files.copy(jarFile, artifactPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("产物已归档到: " + artifactPath);

            logger.info("=== Maven项目打包完成 ===");
            return artifactPath;

        } catch (Exception e) {
            logger.error("Maven打包失败", e);
            throw e;
        }
    }

    @Override
    public Path buildNpmArtifact(String projectCode,
                                String gitUrl,
                                String gitBranch,
                                String buildCmd,
                                WebSocketSession wsSession) throws Exception {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        // 1. 创建日志记录器
        Path logsDir = getLogsDir(projectCode);
        LocalBuildLogger logger = LocalBuildLogger.create(
            logsDir.toString(),
            "build-npm-" + timestamp + ".log",
            wsSession
        );

        try {
            logger.info("=== 开始npm项目打包 ===");
            logger.info("项目编码: " + projectCode);
            logger.info("Git地址: " + gitUrl);
            logger.info("Git分支: " + gitBranch);
            logger.info("构建命令: " + buildCmd);

            // 2. 准备工作目录
            Path sourceDir = getSourceDir(projectCode);
            LocalCommandUtils.createDirectories(sourceDir);

            // 3. Git操作（clone或pull）
            logger.info(">>> 开始Git操作...");
            boolean gitSuccess = performGitOperation(sourceDir, gitUrl, gitBranch, wsSession, logger);
            if (!gitSuccess) {
                logger.error("Git操作失败");
                throw new Exception("Git operation failed");
            }

            // 4. 查找package.json（前端项目根目录）
            Path projectRoot = findNpmProjectRoot(sourceDir);
            logger.info("项目根目录: " + projectRoot);

            // 5. npm安装依赖
            logger.info(">>> 开始npm install...");
            boolean npmInstallSuccess = executeNpmInstall(projectRoot, wsSession, logger);
            if (!npmInstallSuccess) {
                logger.warn("npm install失败，但继续尝试构建");
            }

            // 6. npm打包
            logger.info(">>> 开始npm构建...");
            boolean buildSuccess = executeNpmBuild(projectRoot, buildCmd, wsSession, logger);
            if (!buildSuccess) {
                logger.error("npm构建失败");
                throw new Exception("npm build failed");
            }

            // 7. 查找打包产物
            Path distDir = findNpmDistDir(projectRoot);
            if (distDir == null || !Files.exists(distDir)) {
                logger.error("未找到打包产物（dist目录）");
                throw new Exception("npm dist directory not found");
            }

            logger.info("打包产物目录: " + distDir);

            // 8. 压缩dist目录为zip
            Path artifactsDir = getArtifactsDir(projectCode);
            LocalCommandUtils.createDirectories(artifactsDir);

            String artifactName = "app-frontend-" + timestamp + ".zip";
            Path artifactPath = artifactsDir.resolve(artifactName);

            zipDirectory(distDir, artifactPath);
            logger.info("产物已归档到: " + artifactPath);

            logger.info("=== npm项目打包完成 ===");
            return artifactPath;

        } catch (Exception e) {
            logger.error("npm打包失败", e);
            throw e;
        }
    }

    @Override
    public void cleanupOldArtifacts(String projectCode, int keepVersions) {
        try {
            Path artifactsDir = getArtifactsDir(projectCode);
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
    public Path getSourceDir(String projectCode) {
        String deployPath = opsterProperties.getDeployPath();
        return Paths.get(deployPath, projectCode, "source");
    }

    @Override
    public Path getArtifactsDir(String projectCode) {
        String deployPath = opsterProperties.getDeployPath();
        return Paths.get(deployPath, projectCode, "artifacts");
    }

    /**
     * 获取日志目录
     */
    private Path getLogsDir(String projectCode) {
        String deployPath = opsterProperties.getDeployPath();
        return Paths.get(deployPath, projectCode, "logs");
    }

    /**
     * 执行Git操作（clone或pull）
     */
    private boolean performGitOperation(Path sourceDir, String gitUrl, String gitBranch,
                                       WebSocketSession wsSession, LocalBuildLogger logger) {
        try {
            if (LocalCommandUtils.directoryExists(sourceDir.resolve(".git"))) {
                // 目录已存在，执行pull
                logger.info("检测到Git仓库已存在，执行git pull...");
                String pullCmd = String.format("git fetch origin && git checkout %s && git pull origin %s",
                    gitBranch, gitBranch);
                return LocalCommandUtils.executeCommand(sourceDir, pullCmd, wsSession);
            } else {
                // 目录不存在，执行clone
                logger.info("Git仓库不存在，执行git clone...");
                // 清空source目录
                if (LocalCommandUtils.directoryExists(sourceDir)) {
                    deleteDirectory(sourceDir);
                }
                LocalCommandUtils.createDirectories(sourceDir);

                String cloneCmd = String.format("git clone -b %s %s .", gitBranch, gitUrl);
                return LocalCommandUtils.executeCommand(sourceDir, cloneCmd, wsSession);
            }
        } catch (Exception e) {
            logger.error("Git操作失败", e);
            return false;
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

        // 递归查找子目录
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
     */
    private boolean executeMavenBuild(Path projectRoot, String mavenCmd,
                                     WebSocketSession wsSession, LocalBuildLogger logger) {
        // 设置环境变量
        String mavenHome = opsterProperties.getMavenHome();
        String javaHome = opsterProperties.getJavaHome();

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

        String[] envVars = {
            "JAVA_HOME=" + correctedJavaHome,
            "M2_HOME=" + mavenHome,
            "PATH=" + mavenHome + "/bin:" + correctedJavaHome + "/bin:" + systemPath
        };

        return LocalCommandUtils.executeCommand(projectRoot, mavenCmd, wsSession, envVars);
    }

    /**
     * 查找Maven打包产物（jar文件）
     */
    private Path findMavenArtifact(Path projectRoot) {
        Path targetDir = projectRoot.resolve("target");
        if (!Files.exists(targetDir)) {
            return null;
        }

        try (Stream<Path> paths = Files.list(targetDir)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jar"))
                .filter(p -> !p.toString().contains("-sources.jar"))
                .filter(p -> !p.toString().contains("-javadoc.jar"))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            log.error("Error finding Maven artifact", e);
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

        // 递归查找子目录
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
     */
    private boolean executeNpmInstall(Path projectRoot, WebSocketSession wsSession, LocalBuildLogger logger) {
        return LocalCommandUtils.executeCommand(projectRoot, "npm install", wsSession);
    }

    /**
     * 执行npm构建
     */
    private boolean executeNpmBuild(Path projectRoot, String buildCmd,
                                   WebSocketSession wsSession, LocalBuildLogger logger) {
        return LocalCommandUtils.executeCommand(projectRoot, buildCmd, wsSession);
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

        if (isWindows) {
            // Windows使用PowerShell压缩
            String cmd = String.format(
                "Compress-Archive -Path '%s/*' -DestinationPath '%s' -Force",
                sourceDir, zipFile
            );
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", cmd);
            Process process = pb.start();
            process.waitFor();
        } else {
            // Unix/Mac使用zip命令
            String cmd = String.format("cd %s && zip -r %s .", sourceDir, zipFile);
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
            Process process = pb.start();
            process.waitFor();
        }

        log.info("Compressed {} to {}", sourceDir, zipFile);
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
}
