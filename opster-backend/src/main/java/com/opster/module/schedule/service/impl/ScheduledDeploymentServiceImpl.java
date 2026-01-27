package com.opster.module.schedule.service.impl;

import cn.hutool.core.util.StrUtil;
import com.opster.common.enums.DeploymentStatus;
import com.opster.common.enums.RunStatus;
import com.opster.common.enums.RepositoryType;
import com.opster.common.SshUtils;
import com.opster.module.deployment.entity.DeploymentRecord;
import com.opster.module.deployment.service.DeploymentRecordService;
import com.opster.module.project.dto.RepositoryDTO;
import com.opster.module.project.entity.Project;
import com.opster.module.project.repository.ProjectRepository;
import com.opster.module.schedule.entity.ScheduledDeployment;
import com.opster.module.schedule.enums.ScheduledStatus;
import com.opster.module.schedule.repository.ScheduledDeploymentRepository;
import com.opster.module.schedule.service.ScheduledDeploymentService;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.repository.AppServiceRepository;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 定时发版任务状态常量
 */
interface ScheduledStatusConst {
    int PENDING = 0;
    int COMPLETED = 1;
    int CANCELLED = 2;
}

@Service
public class ScheduledDeploymentServiceImpl implements ScheduledDeploymentService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledDeploymentServiceImpl.class);

    @Autowired
    private ScheduledDeploymentRepository scheduledDeploymentRepository;

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DeploymentRecordService deploymentRecordService;

    @Value("${opster.maven-home}")
    private String mavenHome;

    @Value("${opster.java-home}")
    private String javaHome;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<Integer, Session> executingTasks = new ConcurrentHashMap<>();

    @Override
    public ScheduledDeployment create(ScheduledDeployment task) {
        // 查询服务信息，补充项目名和服务器信息
        AppService service = appServiceRepository.findById(task.getServiceId()).orElse(null);
        if (service == null) {
            throw new RuntimeException("Service not found: " + task.getServiceId());
        }

        Optional<Project> projectOpt = projectRepository.findById(service.getProjectId());
        if (projectOpt.isPresent()) {
            task.setProjectName(projectOpt.get().getProjectName());
        }

        Optional<Server> serverOpt = serverRepository.findById(service.getServerId());
        if (serverOpt.isPresent()) {
            Server server = serverOpt.get();
            task.setServerIp(server.getIp());
            task.setServerAlias(server.getAlias());
        }

        task.setStatus(ScheduledStatus.PENDING.getCode());
        return scheduledDeploymentRepository.save(task);
    }

    @Override
    public ScheduledDeployment update(ScheduledDeployment task) {
        return scheduledDeploymentRepository.save(task);
    }

    @Override
    public ScheduledDeployment getById(Integer id) {
        return scheduledDeploymentRepository.findById(id).orElse(null);
    }

    @Override
    public List<ScheduledDeployment> getAll() {
        return scheduledDeploymentRepository.findAllByOrderByCreateTimeDesc();
    }

    @Override
    public List<ScheduledDeployment> getPendingTasks() {
        return scheduledDeploymentRepository.findByStatusOrderByExecuteDateAndTime(ScheduledStatus.PENDING.getCode());
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        scheduledDeploymentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void cancelById(Integer id) {
        ScheduledDeployment task = scheduledDeploymentRepository.findById(id).orElse(null);
        if (task != null) {
            throw new RuntimeException("Task not found: " + id);
        }
        task.setStatus(ScheduledStatus.CANCELLED.getCode());
        task.setUpdateTime(LocalDateTime.now());
        scheduledDeploymentRepository.save(task);
    }

    /**
     * 定时扫描并执行待执行的定时发版任务（每1分钟执行一次）
     */
    @Override
    @Scheduled(cron = "0 */1 * * * *")
    public void checkAndExecuteTasks() {
        try {
            log.info("Checking scheduled deployment tasks...");

            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();
            String currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

            log.info("Current date: {}, time: {}", currentDate, currentTime);

            // 查询待执行且时间匹配的任务
            List<ScheduledDeployment> tasks = scheduledDeploymentRepository.findByStatusAndDateAndTime(
                    ScheduledStatus.PENDING.getCode(),
                    currentDate,
                    currentTime
            );

            log.info("Found {} pending tasks to execute", tasks.size());

            // 依次执行每个任务
            for (ScheduledDeployment task : tasks) {
                executorService.submit(() -> executeScheduledDeployment(task));
            }

        } catch (Exception e) {
            log.error("Error checking scheduled deployment tasks: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行单个定时发版任务
     */
    private void executeScheduledDeployment(ScheduledDeployment task) {
        // 检查任务ID是否为null
        if (task.getId() == null) {
            log.error("Task ID is null, skipping execution");
            return;
        }
        
        // 检查是否已在执行中
        if (executingTasks.containsKey(task.getId())) {
            log.warn("Task {} is already executing, skipping", task.getId());
            return;
        }

        Session sshSession = null;
        try {
            log.info("Executing scheduled deployment task: {} - {}", task.getId(), task.getName());

            // 标记任务为执行中（避免重复执行）
            executingTasks.put(task.getId(), null);

            // 1. 标记任务为已完成
            task.setStatus(ScheduledStatus.COMPLETED.getCode());
            task.setUpdateTime(LocalDateTime.now());
            scheduledDeploymentRepository.save(task);

            // 2. 获取服务信息
            Optional<AppService> serviceOpt = appServiceRepository.findById(task.getServiceId());
            if (serviceOpt.isEmpty()) {
                log.error("Service not found for task: {}", task.getId());
                return;
            }
            AppService service = serviceOpt.get();

            Optional<Server> serverOpt = serverRepository.findById(service.getServerId());
            if (serverOpt.isEmpty()) {
                log.error("Server not found for task: {}", task.getId());
                return;
            }
            Server server = serverOpt.get();

            Optional<Project> projectOpt = projectRepository.findById(service.getProjectId());
            if (projectOpt.isEmpty()) {
                log.error("Project not found for task: {}", task.getId());
                return;
            }
            Project project = projectOpt.get();

            // 3. 创建部署记录
            DeploymentRecord record = new DeploymentRecord();
            record.setProjectId(project.getId());
            record.setProjectName(project.getProjectName());
            record.setServerId(server.getId());
            record.setServerIp(server.getIp());
            record.setServerAlias(server.getAlias());
            record.setServiceId(service.getId());
            record.setServiceName("Service-" + service.getId());
            record.setStatus(DeploymentStatus.IN_PROGRESS);

            // 构建远程部署目录路径
            String remoteDir = buildRemoteDir(service, project.getProjectCode());

            // 生成日志文件路径
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String logDir = remoteDir + "/p_log";
            String logFilePath = logDir + "/" + timestamp + ".log";
            record.setLogPath(logFilePath);

            record = deploymentRecordService.create(record);
            log.info("Created deployment record: {} for scheduled task: {}", record.getId(), task.getId());

            // 4. 执行发版流程
            doDeployment(sshSession, server, service, project, logFilePath, remoteDir);

            log.info("Scheduled deployment task completed: {} - {}", task.getId(), task.getName());

        } catch (Exception e) {
            log.error("Error executing scheduled deployment task {}: {}", task.getId(), e.getMessage(), e);
        } finally {
            // 移除执行锁
            executingTasks.remove(task.getId());
            // 关闭SSH连接
            if (sshSession != null) {
                SshUtils.disconnect(sshSession);
            }
        }
    }

    /**
     * 构建远程服务器部署目录路径
     * 如果配置了项目路径，则在基础路径后追加项目路径
     *
     * @param service 服务配置
     * @param projectCode 项目编码
     * @return 远程部署目录路径，格式：{deployPath}/{projectCode} 或 {deployPath}/{projectCode}/{projectPath}
     */
    private String buildRemoteDir(AppService service, String projectCode) {
        String baseDir = service.getDeployPath() + "/" + projectCode;

        // 如果配置了项目路径，则追加到基础路径后
        if (StrUtil.isNotBlank(service.getProjectPath())) {
            return baseDir + "/" + service.getProjectPath();
        }

        return baseDir;
    }

    /**
     * 执行发版核心流程
     */
    private void doDeployment(Session sshSession, Server server, AppService service, Project project,
                           String logFilePath, String remoteDir) throws Exception {
        // 连接SSH
        // SshUtils.connect 内部会自动解密密码
        sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());

        // 准备环境变量
        String envVars = String.format("export JAVA_HOME=%s && export PATH=$JAVA_HOME/bin:$PATH && export M2_HOME=%s && export PATH=$M2_HOME/bin:$PATH", javaHome, mavenHome);

        // 创建p_log目录
        String createLogDirCmd = String.format("mkdir -p %s", remoteDir + "/p_log");
        executeCommand(sshSession, createLogDirCmd);

        // 写入开始日志
        String startLog = "=== Deployment Start: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " ===\n";
        String writeStartLogCmd = String.format("echo '%s' > %s", startLog, logFilePath);
        executeCommand(sshSession, writeStartLogCmd);

        // Git Pull/Clone
        String gitMessage = ">>> Checking out source code...";
        log.info(gitMessage);

        // 从 repositories 中获取后端项目的 Git 地址
        List<RepositoryDTO> repositories = project.getRepositories();
        String gitUrl = null;
        if (repositories != null && !repositories.isEmpty()) {
            // 优先获取 BACKEND 类型的仓库，如果没有则获取第一个
            Optional<RepositoryDTO> backendRepo = repositories.stream()
                .filter(r -> RepositoryType.BACKEND.equals(r.getType()))
                .findFirst();
            if (backendRepo.isPresent()) {
                gitUrl = backendRepo.get().getGitUrl();
            } else {
                gitUrl = repositories.get(0).getGitUrl();
            }
        }

        if (gitUrl == null || gitUrl.trim().isEmpty()) {
            String errorMsg = ">>> Error: No Git URL found for this project!";
            log.error(errorMsg);
            executeCommandWithLog(sshSession, "echo '" + errorMsg + "' >> " + logFilePath, logFilePath);
            return;
        }

        String branch = service.getGitBranch();
        String gitCmd = String.format(
                "if [ -d \"%s/source/.git\" ]; then cd \"%s/source\" && git checkout %s && git pull; else mkdir -p \"%s\" && cd \"%s\" && git clone -b %s %s source; fi",
                remoteDir, remoteDir, branch, remoteDir, remoteDir, branch, gitUrl
        );
        executeCommandWithLog(sshSession, gitCmd, logFilePath);

        // Maven Build
        String mavenMessage = ">>> Executing Maven build...";
        log.info(mavenMessage);
        String mavenCmd = service.getMavenCmd();
        String buildCmd = String.format("source /etc/profile && %s && cd %s/source && %s", envVars, remoteDir, mavenCmd);
        executeCommandWithLog(sshSession, buildCmd, logFilePath);

        // Copy Jar
        String copyMessage = ">>> Deploying Jar...";
        log.info(copyMessage);
        String copyCmd = String.format("cp %s/source/target/*.jar %s/", remoteDir, remoteDir);
        executeCommandWithLog(sshSession, copyCmd, logFilePath);

        // Restart
        String restartMessage = ">>> Restarting service...";
        log.info(restartMessage);
        String startScript = service.getStartScript();
        String restartCmd = String.format("source /etc/profile && %s && cd %s && sh %s", envVars, remoteDir, startScript);
        executeCommandWithLog(sshSession, restartCmd, logFilePath);

        // 更新服务状态
        if (service.getPort() != null) {
            service.setRunStatus(RunStatus.NORMAL);
            appServiceRepository.save(service);
        }

        // 写入结束日志
        String endLog = "=== Deployment End: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " ===\n";
        String writeEndLogCmd = String.format("echo '%s' >> %s", endLog, logFilePath);
        executeCommand(sshSession, writeEndLogCmd);
    }

    /**
     * 执行SSH命令
     */
    private void executeCommand(Session sshSession, String command) throws Exception {
        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setCommand(command);
        InputStream in = channel.getInputStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            log.debug(line);
        }

        channel.disconnect();
    }

    /**
     * 执行SSH命令并记录日志
     */
    private void executeCommandWithLog(Session sshSession, String command, String logFilePath) throws Exception {
        String logCmd = String.format("echo '%s' >> %s", "$ " + command, logFilePath);
        executeCommand(sshSession, logCmd);
    }
}
