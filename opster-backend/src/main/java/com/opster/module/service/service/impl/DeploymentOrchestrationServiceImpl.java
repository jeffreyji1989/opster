package com.opster.module.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.opster.common.LocalDeploymentLogger;
import com.opster.common.SshUtils;
import com.opster.common.enums.DeploymentStatus;
import com.opster.common.enums.RepositoryType;
import com.opster.common.enums.RunStatus;
import com.opster.config.OpsterProperties;
import com.opster.module.deployment.entity.DeploymentRecord;
import com.opster.module.deployment.service.DeploymentRecordService;
import com.opster.module.project.entity.Project;
import com.opster.module.project.repository.ProjectRepository;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.repository.AppServiceRepository;
import com.opster.module.service.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 部署编排服务实现类
 * 负责协调整个部署流程，包括本地打包、文件传输、远程部署、健康检查等
 */
@Slf4j
@Service
public class DeploymentOrchestrationServiceImpl implements DeploymentOrchestrationService {

    /**
     * 备份信息
     */
    private static class BackupInfo {
        String filePath;
        Long fileSize;

        BackupInfo(String filePath, Long fileSize) {
            this.filePath = filePath;
            this.fileSize = fileSize;
        }
    }

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private LocalBuildService localBuildService;

    @Autowired
    private FileTransferService fileTransferService;

    @Autowired
    private DeploymentLockService deploymentLockService;

    @Autowired
    private DeploymentRecordService deploymentRecordService;

    @Autowired
    private OpsterProperties opsterProperties;

    @Value("${opster.java-home}")
    private String javaHome;

    @Value("${opster.maven-home}")
    private String mavenHome;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public void executeDeployment(Integer serviceId, WebSocketSession wsSession) {
        String lockId = null;
        DeploymentRecord deploymentRecord = null;
        LocalDeploymentLogger logger = null;

        try {
            // 1. 获取服务信息
            AppService service = appServiceRepository.findById(serviceId)
                .orElseThrow(() -> new Exception("服务不存在: " + serviceId));
            Project project = projectRepository.findById(service.getProjectId())
                .orElseThrow(() -> new Exception("项目不存在: " + service.getProjectId()));
            Server server = serverRepository.findById(service.getServerId())
                .orElseThrow(() -> new Exception("服务器不存在: " + service.getServerId()));

            // 2. 创建本地日志记录器
            String projectCode = project.getProjectCode();
            logger = new LocalDeploymentLogger(projectCode, opsterProperties.getDeployPath(), wsSession);

            // 3. 获取部署锁
            logger.log(">>> 获取部署锁...");
            lockId = deploymentLockService.tryLock(serviceId);
            if (lockId == null) {
                logger.log(">>> 该服务正在部署中，请稍后重试");
                return;
            }

            // 4. 创建部署记录（使用本地日志路径）
            deploymentRecord = new DeploymentRecord();
            deploymentRecord.setProjectId(project.getId());
            deploymentRecord.setProjectName(project.getProjectName());
            deploymentRecord.setServerId(server.getId());
            deploymentRecord.setServerIp(server.getIp());
            deploymentRecord.setServerAlias(server.getAlias());
            deploymentRecord.setServiceId(service.getId());
            deploymentRecord.setServiceName("Service-" + service.getId());
            deploymentRecord.setLogPath(logger.getLogFilePath().toString()); // 本地日志路径
            deploymentRecord.setStatus(DeploymentStatus.IN_PROGRESS);
            deploymentRecord = deploymentRecordService.create(deploymentRecord);

            // 5. 本地打包
            logger.log(">>> 开始本地打包...");
            logger.log(">>> 提示: 前端项目打包可能需要几分钟，请耐心等待...");
            String gitUrl = determineGitUrl(service, project);
            String projectPath = determineProjectPath(service, project);
            String gitUsername = determineGitUsername(service, project);
            String gitPassword = determineGitPassword(service, project);

            Path artifact = localBuildService.buildArtifact(
                projectCode,
                service.getRepositoryType() != null ?
                    RepositoryType.values()[service.getRepositoryType()] : RepositoryType.BACKEND,
                gitUrl,
                service.getGitBranch(),
                service.getRepositoryType() != null && service.getRepositoryType() == 0 ?
                    service.getBuildCmd() : service.getMavenCmd(),
                projectPath,
                wsSession,
                gitUsername,
                gitPassword,
                service.getNodeVersion()  // 传递 Node.js 版本
            );

            // 验证打包产物是否生成成功
            if (artifact == null || !java.nio.file.Files.exists(artifact)) {
                throw new Exception("打包失败: 未生成打包产物文件");
            }

            long artifactSize = java.nio.file.Files.size(artifact);
            logger.log(">>> 本地打包完成! 产物: " + artifact.getFileName().toString() + " (" + (artifactSize / 1024 / 1024) + " MB)");

            // 6. 连接远程服务器
            logger.log(">>> 连接远程服务器 " + server.getIp() + "...");
            // SshUtils.connect 内部会自动解密密码
            Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());
            logger.log(">>> 已连接");

            // 7. 创建远程目录结构（不包含p_log，日志在本地记录）
            String remoteDir = buildRemoteDir(project, service);
            logger.log(">>> 创建远程目录: " + remoteDir);
            executeRemoteCommand(logger, sshSession, "mkdir -p " + remoteDir + "/{bak,logs}");

            // 8. 先备份当前版本（在上传新文件之前备份）
            logger.log(">>> 备份当前版本...");
            BackupInfo backupInfo = backupCurrentVersion(sshSession, remoteDir, logger);
            if (backupInfo.filePath != null) {
                deploymentRecord.setBackupFilePath(backupInfo.filePath);
                deploymentRecord.setBackupFileSize(backupInfo.fileSize);
            }

            // 9. 上传打包产物
            logger.log(">>> 开始上传打包产物 (" + (artifactSize / 1024 / 1024) + " MB)...");
            logger.log(">>> 提示: 大文件上传可能需要几分钟，请勿关闭页面...");
            if (service.getRepositoryType() != null && service.getRepositoryType() == 0) {
                // 前端项目：上传zip并解压
                fileTransferService.uploadAndExtractZip(artifact, sshSession, remoteDir, wsSession);
            } else {
                // 后端项目：上传jar文件
                String remoteJarPath = remoteDir + "/" + artifact.getFileName().toString();
                fileTransferService.uploadFile(artifact, sshSession, remoteJarPath, wsSession);
            }
            logger.log(">>> 打包产物上传成功");

            // 10. 部署新版本（用新文件覆盖app.jar）
            logger.log(">>> 部署新版本...");
            deployNewVersion(sshSession, remoteDir, artifact, logger);

            // 11. 重启服务
            logger.log(">>> 重启服务...");
            restartRemoteService(sshSession, service, logger);

            // 12. 健康检查
            if (service.getPort() != null) {
                logger.log(">>> 健康检查（端口 " + service.getPort() + "）...");
                boolean isHealthy = checkHealth(sshSession, service.getPort(), logger);

                if (isHealthy) {
                    logger.log(">>> 部署成功完成！");
                    service.setRunStatus(RunStatus.NORMAL);
                    deploymentRecord.setStatus(DeploymentStatus.COMPLETED);
                } else {
                    throw new Exception("服务启动失败，端口未监听");
                }
            } else {
                logger.log(">>> 部署完成（未配置端口，跳过健康检查）");
                deploymentRecord.setStatus(DeploymentStatus.COMPLETED);
            }

            appServiceRepository.save(service);
            deploymentRecordService.update(deploymentRecord);

            // 13. 清理旧版本
            int keepVersions = 5; // 默认保留5个版本
            localBuildService.cleanupOldArtifacts(projectCode, keepVersions);

        } catch (Exception e) {
            log.error("Deployment failed for service: {}", serviceId, e);
            if (logger != null) {
                logger.log(">>> 部署失败: " + e.getMessage());
            } else {
                sendMessage(wsSession, ">>> 部署失败: " + e.getMessage());
            }

            if (deploymentRecord != null) {
                deploymentRecord.setStatus(DeploymentStatus.FAILED);
                deploymentRecordService.update(deploymentRecord);
            }

            // 自动回滚
            try {
                if (logger != null) {
                    logger.log(">>> 正在自动回滚...");
                }
                executeRollback(serviceId, wsSession);
            } catch (Exception rollbackException) {
                if (logger != null) {
                    logger.log(">>> 回滚失败: " + rollbackException.getMessage());
                }
            }

        } finally {
            if (logger != null) {
                logger.close();
            }
            if (lockId != null) {
                deploymentLockService.unlock(lockId);
            }
            if (logger != null) {
                logger.log(">>> Done.");
            } else {
                sendMessage(wsSession, ">>> Done.");
            }
        }
    }

    @Override
    public void executeRestart(Integer serviceId, WebSocketSession wsSession) {
        try {
            AppService service = appServiceRepository.findById(serviceId)
                .orElseThrow(() -> new Exception("服务不存在: " + serviceId));
            Server server = serverRepository.findById(service.getServerId())
                .orElseThrow(() -> new Exception("服务器不存在: " + service.getServerId()));

            sendMessage(wsSession, ">>> 连接远程服务器 " + server.getIp() + "...");
            // SshUtils.connect 内部会自动解密密码
            Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());
            sendMessage(wsSession, ">>> 已连接");

            sendMessage(wsSession, ">>> 重启服务...");
            restartRemoteService(sshSession, service, wsSession);

            if (service.getPort() != null) {
                sendMessage(wsSession, ">>> 健康检查...");
                boolean isHealthy = checkHealth(sshSession, service.getPort(), wsSession);

                if (isHealthy) {
                    sendMessage(wsSession, ">>> 重启成功！");
                    service.setRunStatus(RunStatus.NORMAL);
                } else {
                    sendMessage(wsSession, ">>> 重启后端口未监听");
                    service.setRunStatus(RunStatus.ABNORMAL);
                }
                appServiceRepository.save(service);
            } else {
                sendMessage(wsSession, ">>> 重启命令已执行");
            }

        } catch (Exception e) {
            log.error("Restart failed for service: {}", serviceId, e);
            sendMessage(wsSession, ">>> 重启失败: " + e.getMessage());
        }

        sendMessage(wsSession, ">>> Done.");
    }

    @Override
    public void executeRollback(Integer serviceId, WebSocketSession wsSession) {
        String lockId = null;
        try {
            AppService service = appServiceRepository.findById(serviceId)
                .orElseThrow(() -> new Exception("服务不存在: " + serviceId));
            Project project = projectRepository.findById(service.getProjectId())
                .orElseThrow(() -> new Exception("项目不存在: " + service.getProjectId()));
            Server server = serverRepository.findById(service.getServerId())
                .orElseThrow(() -> new Exception("服务器不存在: " + service.getServerId()));

            // 获取锁
            lockId = deploymentLockService.tryLock(serviceId);
            if (lockId == null) {
                sendMessage(wsSession, ">>> 服务正在部署中，无法回滚");
                return;
            }

            sendMessage(wsSession, ">>> 连接远程服务器 " + server.getIp() + "...");
            // SshUtils.connect 内部会自动解密密码
            Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());

            String remoteDir = buildRemoteDir(project, service);

            // 从备份目录恢复
            sendMessage(wsSession, ">>> 从备份目录恢复...");
            restoreFromBackup(sshSession, remoteDir, wsSession);

            // 重启服务
            sendMessage(wsSession, ">>> 重启服务...");
            restartRemoteService(sshSession, service, wsSession);

            // 健康检查
            if (service.getPort() != null) {
                boolean isHealthy = checkHealth(sshSession, service.getPort(), wsSession);
                if (isHealthy) {
                    sendMessage(wsSession, ">>> 回滚成功！");
                } else {
                    sendMessage(wsSession, ">>> 回滚后服务启动失败");
                }
            } else {
                sendMessage(wsSession, ">>> 回滚完成");
            }

        } catch (Exception e) {
            log.error("Rollback failed for service: {}", serviceId, e);
            sendMessage(wsSession, ">>> 回滚失败: " + e.getMessage());
        } finally {
            if (lockId != null) {
                deploymentLockService.unlock(lockId);
            }
            sendMessage(wsSession, ">>> Done.");
        }
    }

    @Override
    public void executeStart(Integer serviceId, WebSocketSession wsSession) {
        executeRestart(serviceId, wsSession);
    }

    @Override
    public void executeStop(Integer serviceId, WebSocketSession wsSession) {
        try {
            AppService service = appServiceRepository.findById(serviceId)
                .orElseThrow(() -> new Exception("服务不存在: " + serviceId));
            Server server = serverRepository.findById(service.getServerId())
                .orElseThrow(() -> new Exception("服务器不存在: " + service.getServerId()));

            sendMessage(wsSession, ">>> 连接远程服务器 " + server.getIp() + "...");
            // SshUtils.connect 内部会自动解密密码
            Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());

            Project project = projectRepository.findById(service.getProjectId())
                .orElseThrow(() -> new Exception("项目不存在: " + service.getProjectId()));
            String remoteDir = buildRemoteDir(project, service);

            // 停止服务
            String stopCmd = String.format(
                "cd %s && sh stop.sh || echo 'Stop script not found, trying to kill by port...' && " +
                "if [ -n '%s' ]; then fuser -k %s/tcp; fi",
                remoteDir, service.getPort(), service.getPort()
            );

            executeRemoteCommand(wsSession, sshSession, stopCmd);
            sendMessage(wsSession, ">>> 停止命令已执行");

        } catch (Exception e) {
            log.error("Stop failed for service: {}", serviceId, e);
            sendMessage(wsSession, ">>> 停止失败: " + e.getMessage());
        }

        sendMessage(wsSession, ">>> Done.");
    }

    @Override
    public void rollbackToSpecificVersion(Integer recordId, WebSocketSession wsSession) {
        String lockId = null;
        DeploymentRecord rollbackRecord = null;
        LocalDeploymentLogger logger = null;

        try {
            // 1. 获取目标版本记录
            DeploymentRecord targetRecord = deploymentRecordService.getById(recordId);
            if (targetRecord == null) {
                sendMessage(wsSession, ">>> 部署记录不存在: " + recordId);
                return;
            }

            if (targetRecord.getBackupFilePath() == null) {
                sendMessage(wsSession, ">>> 该版本没有备份文件，无法回退");
                return;
            }

            Integer serviceId = targetRecord.getServiceId();

            // 2. 获取服务信息
            AppService service = appServiceRepository.findById(serviceId)
                .orElseThrow(() -> new Exception("服务不存在: " + serviceId));
            Project project = projectRepository.findById(service.getProjectId())
                .orElseThrow(() -> new Exception("项目不存在: " + service.getProjectId()));
            Server server = serverRepository.findById(service.getServerId())
                .orElseThrow(() -> new Exception("服务器不存在: " + service.getServerId()));

            // 3. 创建本地日志记录器
            String projectCode = project.getProjectCode();
            logger = new LocalDeploymentLogger(projectCode, opsterProperties.getDeployPath(), wsSession);

            // 4. 获取部署锁
            logger.log(">>> 获取部署锁...");
            lockId = deploymentLockService.tryLock(serviceId);
            if (lockId == null) {
                logger.log(">>> 服务正在部署中，无法回退");
                return;
            }

            // 5. 连接远程服务器
            logger.log(">>> 连接远程服务器 " + server.getIp() + "...");
            // SshUtils.connect 内部会自动解密密码
            Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());

            String remoteDir = buildRemoteDir(project, service);

            // 6. 回退前先备份当前版本（防止回退失败）
            logger.log(">>> 备份当前版本（防止回退失败）...");
            backupCurrentVersion(sshSession, remoteDir, logger);

            // 7. 检查目标备份文件是否存在
            logger.log(">>> 检查备份文件: " + targetRecord.getBackupFilePath());
            String checkCmd = String.format("test -f '%s' && echo 'EXISTS' || echo 'NOT_FOUND'",
                targetRecord.getBackupFilePath());

            String checkResult = executeRemoteCommandAndGetOutput(sshSession, checkCmd);
            if (!checkResult.contains("EXISTS")) {
                throw new Exception("备份文件不存在: " + targetRecord.getBackupFilePath());
            }

            // 8. 恢复目标版本的jar文件
            logger.log(">>> 恢复版本: " + targetRecord.getCreateTime());

            // 从备份文件路径中提取原始jar文件名
            // 备份文件路径格式: /path/to/bak/original-name.jar_backup_timestamp.jar
            String backupFilePath = targetRecord.getBackupFilePath();
            String originalJarName = extractOriginalJarNameFromBackup(backupFilePath);

            String restoreCmd = String.format(
                "cp -f '%s' %s/%s",
                backupFilePath, remoteDir, originalJarName
            );
            executeRemoteCommand(logger, sshSession, restoreCmd);
            logger.log(">>> 已恢复文件: " + originalJarName);

            // 9. 创建回退记录（使用本地日志路径）
            rollbackRecord = new DeploymentRecord();
            rollbackRecord.setProjectId(project.getId());
            rollbackRecord.setProjectName(project.getProjectName());
            rollbackRecord.setServerId(server.getId());
            rollbackRecord.setServerIp(server.getIp());
            rollbackRecord.setServerAlias(server.getAlias());
            rollbackRecord.setServiceId(service.getId());
            rollbackRecord.setServiceName("Service-" + service.getId());
            rollbackRecord.setLogPath(logger.getLogFilePath().toString()); // 本地日志路径
            rollbackRecord.setStatus(DeploymentStatus.IN_PROGRESS);
            rollbackRecord.setIsRollback(true);
            rollbackRecord.setRollbackFromId(recordId);
            rollbackRecord.setVersionDescription("回退到版本: " + targetRecord.getCreateTime());
            if (targetRecord.getVersionTag() != null) {
                rollbackRecord.setVersionTag(targetRecord.getVersionTag() + "-rollback");
            }
            rollbackRecord = deploymentRecordService.create(rollbackRecord);

            // 10. 重启服务
            logger.log(">>> 重启服务...");
            restartRemoteService(sshSession, service, logger);

            // 11. 健康检查
            if (service.getPort() != null) {
                logger.log(">>> 健康检查（端口 " + service.getPort() + "）...");
                boolean isHealthy = checkHealth(sshSession, service.getPort(), logger);

                if (isHealthy) {
                    logger.log(">>> 回退成功完成！");
                    service.setRunStatus(RunStatus.NORMAL);
                    rollbackRecord.setStatus(DeploymentStatus.COMPLETED);
                } else {
                    throw new Exception("回退后服务启动失败");
                }
            } else {
                logger.log(">>> 回退完成（未配置端口，跳过健康检查）");
                rollbackRecord.setStatus(DeploymentStatus.COMPLETED);
            }

            appServiceRepository.save(service);
            deploymentRecordService.update(rollbackRecord);

        } catch (Exception e) {
            log.error("Rollback to version failed for record: {}", recordId, e);
            if (logger != null) {
                logger.log(">>> 回退失败: " + e.getMessage());
            } else {
                sendMessage(wsSession, ">>> 回退失败: " + e.getMessage());
            }

            if (rollbackRecord != null) {
                rollbackRecord.setStatus(DeploymentStatus.FAILED);
                deploymentRecordService.update(rollbackRecord);
            }

        } finally {
            if (logger != null) {
                logger.close();
            }
            if (lockId != null) {
                deploymentLockService.unlock(lockId);
            }
            if (logger != null) {
                logger.log(">>> Done.");
            } else {
                sendMessage(wsSession, ">>> Done.");
            }
        }
    }

    /**
     * 构建远程服务器部署目录路径
     * 如果配置了项目路径，则在基础路径后追加项目路径
     *
     * @param project 项目配置
     * @param service 服务配置
     * @return 远程部署目录路径，格式：{deployPath}/{projectCode} 或 {deployPath}/{projectCode}/{projectPath}
     * @throws IllegalArgumentException 如果参数为空或无效
     */
    private String buildRemoteDir(Project project, AppService service) {
        // 参数验证
        if (project == null) {
            throw new IllegalArgumentException("项目配置不能为空");
        }
        if (service == null) {
            throw new IllegalArgumentException("服务配置不能为空");
        }
        String projectCode = project.getProjectCode();
        if (StrUtil.isBlank(projectCode)) {
            throw new IllegalArgumentException("项目编码不能为空");
        }
        String deployPath = project.getDeployPath();
        if (StrUtil.isBlank(deployPath)) {
            throw new IllegalArgumentException("部署路径不能为空");
        }

        // 标准化路径：去除首尾空格，确保不以斜杠结尾
        String normalizedBasePath = deployPath.trim().replaceAll("/+$", "");
        String normalizedProjectCode = projectCode.trim().replaceAll("^/+", "").replaceAll("/+$", "");

        // 构建基础路径
        String baseDir = normalizedBasePath + "/" + normalizedProjectCode;

        // 如果配置了项目路径，则追加到基础路径后
        if (StrUtil.isNotBlank(service.getProjectPath())) {
            String normalizedProjectPath = service.getProjectPath().trim()
                    .replaceAll("^/+", "")  // 去除开头斜杠
                    .replaceAll("/+$", "");  // 去除结尾斜杠
            return baseDir + "/" + normalizedProjectPath;
        }

        return baseDir;
    }

    /**
     * 确定Git URL
     */
    private String determineGitUrl(AppService service, Project project) {
        // 优先使用服务的Git URL
        if (StrUtil.isNotBlank(service.getRepoGitUrl())) {
            return service.getRepoGitUrl();
        }

        // 从项目的repositories中查找
        if (project.getRepositories() != null && !project.getRepositories().isEmpty()) {
            RepositoryType repoType = service.getRepositoryType() != null ?
                RepositoryType.values()[service.getRepositoryType()] : RepositoryType.BACKEND;

            return project.getRepositories().stream()
                .filter(r -> r.getType() == repoType)
                .findFirst()
                .map(com.opster.module.project.dto.RepositoryDTO::getGitUrl)
                .orElse(project.getRepositories().get(0).getGitUrl());
        }

        throw new RuntimeException("未找到Git仓库地址");
    }

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

    /**
     * 确定项目路径
     * 优先级：Service.projectPath > Project.repositories[].projectPath > null
     */
    private String determineProjectPath(AppService service, Project project) {
        // 优先使用服务级别的项目路径
        if (StrUtil.isNotBlank(service.getProjectPath())) {
            return service.getProjectPath();
        }

        // 从项目的repositories中查找
        if (project.getRepositories() != null && !project.getRepositories().isEmpty()) {
            RepositoryType repoType = service.getRepositoryType() != null ?
                RepositoryType.values()[service.getRepositoryType()] : RepositoryType.BACKEND;

            return project.getRepositories().stream()
                .filter(r -> r.getType() == repoType)
                .findFirst()
                .map(com.opster.module.project.dto.RepositoryDTO::getProjectPath)
                .orElse(null);
        }

        return null;
    }

    /**
     * 备份当前版本（使用 WebSocketSession）
     * @return 备份信息（包含文件路径和大小）
     */
    private BackupInfo backupCurrentVersion(Session sshSession, String remoteDir, WebSocketSession wsSession)
            throws Exception {
        String backupCmd = String.format(
            "bash -c '\n" +
            "mkdir -p %s/bak\n" +
            "cd %s\n" +
            "timestamp=$(date +\"%%Y%%m%%d%%H%%M%%S\")\n" +
            "backed_up=\"\"\n" +
            "for jar_file in *.jar; do\n" +  // 遍历所有jar文件
            "    if [ -f \"$jar_file\" ]; then\n" +
            "        backup_file=\"%s/bak/${jar_file}_backup_${timestamp}.jar\"\n" +  // 修复：直接使用完整文件名，不去除.jar
            "        cp -f \"$jar_file\" \"$backup_file\" && echo \"BACKUP_SUCCESS:$backup_file\"\n" +
            "        ls -l \"$backup_file\" 2>/dev/null | awk \"{print \\\"BACKUP_SIZE:\\\" \\$5}\"\n" +
            "        backed_up=\"yes\"\n" +
            "    fi\n" +
            "done\n" +
            "if [ -z \"$backed_up\" ]; then\n" +
            "    echo \"NO_JAR_FOUND\"\n" +
            "fi\n" +
            "'",
            remoteDir, remoteDir, remoteDir
        );

        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setCommand(backupCmd);
        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        BufferedReader errReader = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8));

        String backupPath = null;
        Long fileSize = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("BACKUP_SUCCESS:")) {
                backupPath = line.substring("BACKUP_SUCCESS:".length());
                sendMessage(wsSession, ">>> 备份完成: " + backupPath);
            } else if (line.startsWith("BACKUP_SIZE:")) {
                try {
                    fileSize = Long.parseLong(line.substring("BACKUP_SIZE:".length()));
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            } else if (!line.isEmpty()) {
                sendMessage(wsSession, line);
            }
        }
        while ((line = errReader.readLine()) != null) {
            sendMessage(wsSession, line);
        }

        channel.disconnect();

        if (backupPath == null) {
            sendMessage(wsSession, ">>> 没有发现运行的jar文件，跳过备份");
        }

        return new BackupInfo(backupPath, fileSize);
    }

    /**
     * 备份当前版本（使用 LocalDeploymentLogger）
     * @return 备份信息（包含文件路径和大小）
     */
    private BackupInfo backupCurrentVersion(Session sshSession, String remoteDir, LocalDeploymentLogger logger)
            throws Exception {
        String backupCmd = String.format(
            "bash -c '\n" +
            "mkdir -p %s/bak\n" +
            "cd %s\n" +
            "timestamp=$(date +\"%%Y%%m%%d%%H%%M%%S\")\n" +
            "backed_up=\"\"\n" +
            "for jar_file in *.jar; do\n" +  // 遍历所有jar文件
            "    if [ -f \"$jar_file\" ]; then\n" +
            "        backup_file=\"%s/bak/${jar_file}_backup_${timestamp}.jar\"\n" +  // 修复：直接使用完整文件名
            "        cp -f \"$jar_file\" \"$backup_file\" && echo \"BACKUP_SUCCESS:$backup_file\"\n" +
            "        ls -l \"$backup_file\" 2>/dev/null | awk \"{print \\\"BACKUP_SIZE:\\\" \\$5}\"\n" +
            "        backed_up=\"yes\"\n" +
            "    fi\n" +
            "done\n" +
            "if [ -z \"$backed_up\" ]; then\n" +
            "    echo \"NO_JAR_FOUND\"\n" +
            "fi\n" +
            "'",
            remoteDir, remoteDir, remoteDir
        );

        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setCommand(backupCmd);
        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        BufferedReader errReader = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8));

        String backupPath = null;
        Long fileSize = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("BACKUP_SUCCESS:")) {
                backupPath = line.substring("BACKUP_SUCCESS:".length());
                logger.log(">>> 备份完成: " + backupPath);
            } else if (line.startsWith("BACKUP_SIZE:")) {
                try {
                    fileSize = Long.parseLong(line.substring("BACKUP_SIZE:".length()));
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            } else if (!line.isEmpty()) {
                logger.log(line);
            }
        }
        while ((line = errReader.readLine()) != null) {
            logger.log(line);
        }

        channel.disconnect();

        if (backupPath == null) {
            logger.log(">>> 没有发现运行的jar文件，跳过备份");
        }

        return new BackupInfo(backupPath, fileSize);
    }

    /**
     * 部署新版本（使用 WebSocketSession）
     */
    private void deployNewVersion(Session sshSession, String remoteDir, Path artifact,
                                 WebSocketSession wsSession) throws Exception {
        String fileName = artifact.getFileName().toString();

        if (fileName.endsWith(".jar")) {
            // 后端项目：保持原始jar文件名（去除时间戳前缀）
            // 文件名格式: timestamp_original-name.jar
            String originalJarName = extractOriginalJarName(fileName);

            // 移动jar文件到最终位置，使用原始文件名
            String deployCmd = String.format(
                "mv -f %s/%s %s/%s",
                remoteDir, fileName, remoteDir, originalJarName
            );
            executeRemoteCommand(wsSession, sshSession, deployCmd);

            // 清理旧的 app.jar 文件（如果存在）
            String cleanCmd = String.format(
                "rm -f %s/app.jar && echo '已清理旧的app.jar文件' || echo '没有找到旧的app.jar文件'",
                remoteDir
            );
            executeRemoteCommand(wsSession, sshSession, cleanCmd);

            sendMessage(wsSession, ">>> 部署完成: " + originalJarName);
        } else if (fileName.endsWith(".zip")) {
            // 前端项目：已在uploadAndExtractZip中处理
            sendMessage(wsSession, ">>> 前端文件已部署");
        }
    }

    /**
     * 从归档文件名中提取原始jar文件名
     * 例如: 20260131165555_eip-backend-1.0.0.jar -> eip-backend-1.0.0.jar
     */
    private String extractOriginalJarName(String archiveName) {
        if (archiveName != null && archiveName.contains("_")) {
            // 移除时间戳前缀
            return archiveName.substring(archiveName.indexOf("_") + 1);
        }
        return archiveName;
    }

    /**
     * 从备份文件路径中提取原始jar文件名
     * 例如: /path/to/eip-backend-1.0.0.jar_backup_20260131203137.jar -> eip-backend-1.0.0.jar
     */
    private String extractOriginalJarNameFromBackup(String backupFilePath) {
        if (backupFilePath == null || backupFilePath.isEmpty()) {
            return "app.jar"; // 默认值
        }

        // 获取文件名（不含路径）
        String fileName = backupFilePath.substring(backupFilePath.lastIndexOf("/") + 1);

        // 去除 _backup_timestamp.jar 后缀
        // 备份文件名格式: original-name.jar_backup_timestamp.jar
        if (fileName.contains("_backup_")) {
            int backupIndex = fileName.indexOf("_backup_");
            fileName = fileName.substring(0, backupIndex) + ".jar";
        }

        return fileName;
    }

    /**
     * 部署新版本（使用 LocalDeploymentLogger）
     */
    private void deployNewVersion(Session sshSession, String remoteDir, Path artifact,
                                 LocalDeploymentLogger logger) throws Exception {
        String fileName = artifact.getFileName().toString();

        if (fileName.endsWith(".jar")) {
            // 后端项目：保持原始jar文件名（去除时间戳前缀）
            String originalJarName = extractOriginalJarName(fileName);

            // 移动jar文件到最终位置，使用原始文件名
            String deployCmd = String.format(
                "mv -f %s/%s %s/%s",
                remoteDir, fileName, remoteDir, originalJarName
            );
            executeRemoteCommand(logger, sshSession, deployCmd);

            // 清理旧的 app.jar 文件（如果存在）
            String cleanCmd = String.format(
                "rm -f %s/app.jar && echo '已清理旧的app.jar文件' || echo '没有找到旧的app.jar文件'",
                remoteDir
            );
            executeRemoteCommand(logger, sshSession, cleanCmd);

            logger.log(">>> 部署完成: " + originalJarName);
        } else if (fileName.endsWith(".zip")) {
            // 前端项目：已在uploadAndExtractZip中处理
            logger.log(">>> 前端文件已部署");
        }
    }

    /**
     * 重启远程服务（使用 WebSocketSession）
     */
    private void restartRemoteService(Session sshSession, AppService service,
                                     WebSocketSession wsSession) throws Exception {
        Project project = projectRepository.findById(service.getProjectId())
            .orElseThrow(() -> new Exception("项目不存在"));

        String remoteDir = buildRemoteDir(project, service);
        String envVars = String.format(
            "export JAVA_HOME=%s && export M2_HOME=%s",
            javaHome, mavenHome
        );

        String restartCmd = String.format(
            "source /etc/profile && %s && cd %s && sh %s",
            envVars, remoteDir, service.getStartScript()
        );

        executeRemoteCommand(wsSession, sshSession, restartCmd);
    }

    /**
     * 重启远程服务（使用 LocalDeploymentLogger）
     */
    private void restartRemoteService(Session sshSession, AppService service,
                                     LocalDeploymentLogger logger) throws Exception {
        Project project = projectRepository.findById(service.getProjectId())
            .orElseThrow(() -> new Exception("项目不存在"));

        String remoteDir = buildRemoteDir(project, service);
        String envVars = String.format(
            "export JAVA_HOME=%s && export M2_HOME=%s",
            javaHome, mavenHome
        );

        String restartCmd = String.format(
            "source /etc/profile && %s && cd %s && sh %s",
            envVars, remoteDir, service.getStartScript()
        );

        executeRemoteCommand(logger, sshSession, restartCmd);
    }

    /**
     * 从备份恢复
     */
    private void restoreFromBackup(Session sshSession, String remoteDir,
                                  WebSocketSession wsSession) throws Exception {
        String restoreCmd = String.format(
            "bash -c '\n" +
            "cd %s\n" +
            "backup_file=$(ls -1t bak/*.jar 2>/dev/null | head -1)\n" +  // 找到最新的备份文件
            "if [ -n \"$backup_file\" ]; then\n" +
            "    # 从备份文件名中提取原始文件名\n" +
            "    # 备份文件名格式: original-name.jar_backup_timestamp.jar\n" +
            "    original_name=$(basename \"$backup_file\" | sed 's/_backup_[0-9]*\\.jar$/.jar/')\n" +
            "    cp -f \"$backup_file\" \"$original_name\"\n" +  // 恢复为原始文件名
            "    echo \"Restored from: $backup_file\"\n" +
            "    echo \"Original file name: $original_name\"\n" +
            "else\n" +
            "    echo \"No backup found\"\n" +
            "    exit 1\n" +
            "fi\n" +
            "'",
            remoteDir
        );
        executeRemoteCommand(wsSession, sshSession, restoreCmd);
    }

    /**
     * 健康检查（端口检测）- 使用 WebSocketSession
     */
    private boolean checkHealth(Session sshSession, int port, WebSocketSession wsSession) {
        try {
            for (int i = 0; i < 30; i++) {
                String cmd = String.format("netstat -tln 2>/dev/null | grep ':%s' || ss -tln 2>/dev/null | grep ':%s'",
                    port, port);

                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                channel.setCommand(cmd);
                InputStream in = channel.getInputStream();
                channel.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String output = "";
                String line;
                while ((line = reader.readLine()) != null) {
                    output += line;
                }
                channel.disconnect();

                if (StrUtil.isNotBlank(output)) {
                    return true;
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.error("Health check failed", e);
        }
        return false;
    }

    /**
     * 健康检查（端口检测）- 使用 LocalDeploymentLogger
     */
    private boolean checkHealth(Session sshSession, int port, LocalDeploymentLogger logger) {
        try {
            for (int i = 0; i < 30; i++) {
                String cmd = String.format("netstat -tln 2>/dev/null | grep ':%s' || ss -tln 2>/dev/null | grep ':%s'",
                    port, port);

                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                channel.setCommand(cmd);
                InputStream in = channel.getInputStream();
                channel.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String output = "";
                String line;
                while ((line = reader.readLine()) != null) {
                    output += line;
                }
                channel.disconnect();

                if (StrUtil.isNotBlank(output)) {
                    return true;
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.error("Health check failed", e);
        }
        return false;
    }

    /**
     * 执行远程命令（使用 WebSocketSession）
     */
    private void executeRemoteCommand(WebSocketSession wsSession, Session sshSession, String command)
            throws Exception {
        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setCommand(command);
        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        BufferedReader errReader = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            sendMessage(wsSession, line);
        }
        while ((line = errReader.readLine()) != null) {
            sendMessage(wsSession, line);
        }

        channel.disconnect();
    }

    /**
     * 执行远程命令（使用 LocalDeploymentLogger）
     */
    private void executeRemoteCommand(LocalDeploymentLogger logger, Session sshSession, String command)
            throws Exception {
        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setCommand(command);
        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        BufferedReader errReader = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            logger.log(line);
        }
        while ((line = errReader.readLine()) != null) {
            logger.log(line);
        }

        channel.disconnect();
    }

    /**
     * 发送消息到WebSocket
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
            }
        } catch (Exception e) {
            log.error("Error sending message to WebSocket", e);
        }
    }

    /**
     * 执行远程命令并获取输出
     */
    private String executeRemoteCommandAndGetOutput(Session sshSession, String command) throws Exception {
        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setCommand(command);
        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        BufferedReader errReader = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8));

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        while ((line = errReader.readLine()) != null) {
            output.append(line).append("\n");
        }

        channel.disconnect();
        return output.toString();
    }

    /**
     * 提取部署核心逻辑（用于异步执行）
     * @return 部署记录ID
     */
    private Integer doExecuteDeployment(Integer serviceId, LocalDeploymentLogger logger) throws Exception {
        String lockId = null;
        DeploymentRecord deploymentRecord = null;

        try {
            // 1. 获取服务信息
            AppService service = appServiceRepository.findById(serviceId)
                .orElseThrow(() -> new Exception("服务不存在: " + serviceId));
            Project project = projectRepository.findById(service.getProjectId())
                .orElseThrow(() -> new Exception("项目不存在: " + service.getProjectId()));
            Server server = serverRepository.findById(service.getServerId())
                .orElseThrow(() -> new Exception("服务器不存在: " + service.getServerId()));

            String projectCode = project.getProjectCode();

            // 2. 获取部署锁
            logger.log(">>> 获取部署锁...");
            lockId = deploymentLockService.tryLock(serviceId);
            if (lockId == null) {
                logger.log(">>> 该服务正在部署中，请稍后重试");
                throw new Exception("服务正在部署中");
            }

            // 3. 创建部署记录（使用本地日志路径）
            deploymentRecord = new DeploymentRecord();
            deploymentRecord.setProjectId(project.getId());
            deploymentRecord.setProjectName(project.getProjectName());
            deploymentRecord.setServerId(server.getId());
            deploymentRecord.setServerIp(server.getIp());
            deploymentRecord.setServerAlias(server.getAlias());
            deploymentRecord.setServiceId(service.getId());
            deploymentRecord.setServiceName("Service-" + service.getId());
            deploymentRecord.setLogPath(logger.getLogFilePath().toString());
            deploymentRecord.setStatus(DeploymentStatus.IN_PROGRESS);
            deploymentRecord = deploymentRecordService.create(deploymentRecord);

            // 4. 本地打包
            logger.log(">>> 开始本地打包...");
            String gitUrl = determineGitUrl(service, project);
            String projectPath = determineProjectPath(service, project);
            String gitUsername = determineGitUsername(service, project);
            String gitPassword = determineGitPassword(service, project);

            Path artifact = localBuildService.buildArtifact(
                projectCode,
                service.getRepositoryType() != null ?
                    RepositoryType.values()[service.getRepositoryType()] : RepositoryType.BACKEND,
                gitUrl,
                service.getGitBranch(),
                service.getRepositoryType() != null && service.getRepositoryType() == 0 ?
                    service.getBuildCmd() : service.getMavenCmd(),
                projectPath,
                null, // 无 WebSocket
                gitUsername,
                gitPassword,
                service.getNodeVersion()  // 传递 Node.js 版本
            );

            // 5. 连接远程服务器
            logger.log(">>> 连接远程服务器 " + server.getIp() + "...");
            // SshUtils.connect 内部会自动解密密码
            Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());
            logger.log(">>> 已连接");

            // 6. 创建远程目录结构（不包含p_log，日志在本地记录）
            String remoteDir = buildRemoteDir(project, service);
            logger.log(">>> 创建远程目录: " + remoteDir);
            executeRemoteCommand(logger, sshSession, "mkdir -p " + remoteDir + "/{bak,logs}");

            // 7. 先备份当前版本（在上传新文件之前备份）
            logger.log(">>> 备份当前版本...");
            BackupInfo backupInfo = backupCurrentVersion(sshSession, remoteDir, logger);
            if (backupInfo.filePath != null) {
                deploymentRecord.setBackupFilePath(backupInfo.filePath);
                deploymentRecord.setBackupFileSize(backupInfo.fileSize);
            }

            // 8. 上传打包产物
            logger.log(">>> 上传打包产物...");
            if (service.getRepositoryType() != null && service.getRepositoryType() == 0) {
                fileTransferService.uploadAndExtractZip(artifact, sshSession, remoteDir, null);
            } else {
                String remoteJarPath = remoteDir + "/" + artifact.getFileName().toString();
                fileTransferService.uploadFile(artifact, sshSession, remoteJarPath, null);
            }

            // 9. 部署新版本（用新文件覆盖app.jar）
            logger.log(">>> 部署新版本...");
            deployNewVersion(sshSession, remoteDir, artifact, logger);

            // 10. 重启服务
            logger.log(">>> 重启服务...");
            restartRemoteService(sshSession, service, logger);

            // 11. 健康检查
            if (service.getPort() != null) {
                logger.log(">>> 健康检查（端口 " + service.getPort() + "）...");
                boolean isHealthy = checkHealth(sshSession, service.getPort(), logger);

                if (isHealthy) {
                    logger.log(">>> 部署成功完成！");
                    service.setRunStatus(RunStatus.NORMAL);
                    deploymentRecord.setStatus(DeploymentStatus.COMPLETED);
                } else {
                    throw new Exception("服务启动失败，端口未监听");
                }
            } else {
                logger.log(">>> 部署完成（未配置端口，跳过健康检查）");
                deploymentRecord.setStatus(DeploymentStatus.COMPLETED);
            }

            appServiceRepository.save(service);
            deploymentRecordService.update(deploymentRecord);

            // 12. 清理旧版本
            localBuildService.cleanupOldArtifacts(projectCode, 5);

            return deploymentRecord.getId();

        } catch (Exception e) {
            log.error("Deployment failed for service: {}", serviceId, e);
            logger.log(">>> 部署失败: " + e.getMessage());

            if (deploymentRecord != null) {
                deploymentRecord.setStatus(DeploymentStatus.FAILED);
                deploymentRecordService.update(deploymentRecord);
            }
            throw e;

        } finally {
            if (lockId != null) {
                deploymentLockService.unlock(lockId);
            }
            logger.log(">>> Done.");
        }
    }

    /**
     * 提取回退核心逻辑（用于异步执行）
     * @return 新的回退记录ID
     */
    private Integer doRollbackToSpecificVersion(Integer recordId, LocalDeploymentLogger logger) throws Exception {
        String lockId = null;
        DeploymentRecord rollbackRecord = null;

        try {
            // 1. 获取目标版本记录
            DeploymentRecord targetRecord = deploymentRecordService.getById(recordId);
            if (targetRecord == null) {
                throw new Exception("部署记录不存在: " + recordId);
            }

            if (targetRecord.getBackupFilePath() == null) {
                throw new Exception("该版本没有备份文件，无法回退");
            }

            Integer serviceId = targetRecord.getServiceId();

            // 2. 获取服务信息
            AppService service = appServiceRepository.findById(serviceId)
                .orElseThrow(() -> new Exception("服务不存在: " + serviceId));
            Project project = projectRepository.findById(service.getProjectId())
                .orElseThrow(() -> new Exception("项目不存在: " + service.getProjectId()));
            Server server = serverRepository.findById(service.getServerId())
                .orElseThrow(() -> new Exception("服务器不存在: " + service.getServerId()));

            String projectCode = project.getProjectCode();

            // 3. 获取部署锁
            logger.log(">>> 获取部署锁...");
            lockId = deploymentLockService.tryLock(serviceId);
            if (lockId == null) {
                logger.log(">>> 服务正在部署中，无法回退");
                throw new Exception("服务正在部署中");
            }

            // 4. 连接远程服务器
            logger.log(">>> 连接远程服务器 " + server.getIp() + "...");
            // SshUtils.connect 内部会自动解密密码
            Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());

            String remoteDir = buildRemoteDir(project, service);

            // 5. 回退前先备份当前版本
            logger.log(">>> 备份当前版本（防止回退失败）...");
            backupCurrentVersion(sshSession, remoteDir, logger);

            // 6. 检查目标备份文件是否存在
            logger.log(">>> 检查备份文件: " + targetRecord.getBackupFilePath());
            String checkCmd = String.format("test -f '%s' && echo 'EXISTS' || echo 'NOT_FOUND'",
                targetRecord.getBackupFilePath());

            String checkResult = executeRemoteCommandAndGetOutput(sshSession, checkCmd);
            if (!checkResult.contains("EXISTS")) {
                throw new Exception("备份文件不存在: " + targetRecord.getBackupFilePath());
            }

            // 7. 恢复目标版本的jar文件
            logger.log(">>> 恢复版本: " + targetRecord.getCreateTime());
            String restoreCmd = String.format("cp -f '%s' %s/app.jar",
                targetRecord.getBackupFilePath(), remoteDir);
            executeRemoteCommand(logger, sshSession, restoreCmd);

            // 8. 创建回退记录
            rollbackRecord = new DeploymentRecord();
            rollbackRecord.setProjectId(project.getId());
            rollbackRecord.setProjectName(project.getProjectName());
            rollbackRecord.setServerId(server.getId());
            rollbackRecord.setServerIp(server.getIp());
            rollbackRecord.setServerAlias(server.getAlias());
            rollbackRecord.setServiceId(service.getId());
            rollbackRecord.setServiceName("Service-" + service.getId());
            rollbackRecord.setLogPath(logger.getLogFilePath().toString());
            rollbackRecord.setStatus(DeploymentStatus.IN_PROGRESS);
            rollbackRecord.setIsRollback(true);
            rollbackRecord.setRollbackFromId(recordId);
            rollbackRecord.setVersionDescription("回退到版本: " + targetRecord.getCreateTime());
            if (targetRecord.getVersionTag() != null) {
                rollbackRecord.setVersionTag(targetRecord.getVersionTag() + "-rollback");
            }
            rollbackRecord = deploymentRecordService.create(rollbackRecord);

            // 9. 重启服务
            logger.log(">>> 重启服务...");
            restartRemoteService(sshSession, service, logger);

            // 10. 健康检查
            if (service.getPort() != null) {
                logger.log(">>> 健康检查（端口 " + service.getPort() + "）...");
                boolean isHealthy = checkHealth(sshSession, service.getPort(), logger);

                if (isHealthy) {
                    logger.log(">>> 回退成功完成！");
                    service.setRunStatus(RunStatus.NORMAL);
                    rollbackRecord.setStatus(DeploymentStatus.COMPLETED);
                } else {
                    throw new Exception("回退后服务启动失败");
                }
            } else {
                logger.log(">>> 回退完成（未配置端口，跳过健康检查）");
                rollbackRecord.setStatus(DeploymentStatus.COMPLETED);
            }

            appServiceRepository.save(service);
            deploymentRecordService.update(rollbackRecord);

            return rollbackRecord.getId();

        } catch (Exception e) {
            log.error("Rollback to version failed for record: {}", recordId, e);
            logger.log(">>> 回退失败: " + e.getMessage());

            if (rollbackRecord != null) {
                rollbackRecord.setStatus(DeploymentStatus.FAILED);
                deploymentRecordService.update(rollbackRecord);
            }
            throw e;

        } finally {
            if (lockId != null) {
                deploymentLockService.unlock(lockId);
            }
            logger.log(">>> Done.");
        }
    }

    @Override
    public Integer executeDeploymentAsync(Integer serviceId) {
        // 获取服务信息
        AppService service = appServiceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("服务不存在: " + serviceId));
        Project project = projectRepository.findById(service.getProjectId())
            .orElseThrow(() -> new RuntimeException("项目不存在: " + service.getProjectId()));

        String projectCode = project.getProjectCode();

        // 创建本地日志记录器（无 WebSocket）
        try (LocalDeploymentLogger logger = new LocalDeploymentLogger(projectCode, opsterProperties.getDeployPath())) {
            return doExecuteDeployment(serviceId, logger);
        } catch (Exception e) {
            log.error("Async deployment failed for service: {}", serviceId, e);
            throw new RuntimeException("部署失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Integer rollbackToSpecificVersionAsync(Integer recordId) {
        // 获取目标版本记录
        DeploymentRecord targetRecord = deploymentRecordService.getById(recordId);
        if (targetRecord == null) {
            throw new RuntimeException("部署记录不存在: " + recordId);
        }

        Integer serviceId = targetRecord.getServiceId();
        AppService service = appServiceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("服务不存在: " + serviceId));
        Project project = projectRepository.findById(service.getProjectId())
            .orElseThrow(() -> new RuntimeException("项目不存在: " + service.getProjectId()));

        String projectCode = project.getProjectCode();

        // 创建本地日志记录器（无 WebSocket）
        try (LocalDeploymentLogger logger = new LocalDeploymentLogger(projectCode, opsterProperties.getDeployPath())) {
            return doRollbackToSpecificVersion(recordId, logger);
        } catch (Exception e) {
            log.error("Async rollback failed for record: {}", recordId, e);
            throw new RuntimeException("回退失败: " + e.getMessage(), e);
        }
    }
}
