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
import com.opster.module.schedule.entity.ScheduledDeploymentServiceEntity;
import com.opster.module.schedule.enums.ScheduledStatus;
import com.opster.module.schedule.repository.ScheduledDeploymentRepository;
import com.opster.module.schedule.repository.ScheduledDeploymentServiceRepository;
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
import org.springframework.scheduling.annotation.Async;
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
    private ScheduledDeploymentServiceRepository scheduledDeploymentServiceRepository;

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

    private final ConcurrentHashMap<Integer, Session> executingTasks = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public ScheduledDeployment create(ScheduledDeployment task, List<Integer> serviceIds) {
        // 设置服务数量
        task.setServiceCount(serviceIds.size());
        task.setStatus(ScheduledStatus.PENDING.getCode());

        // 为了兼容数据库中废弃字段的NOT NULL约束，设置第一个serviceId
        if (serviceIds != null && !serviceIds.isEmpty()) {
            task.setServiceId(serviceIds.get(0));
        }

        // 保存主任务
        ScheduledDeployment savedTask = scheduledDeploymentRepository.save(task);
        log.info("创建定时发版任务: {}, 服务数量: {}", savedTask.getName(), serviceIds.size());

        // 为每个服务创建关联记录
        for (Integer serviceId : serviceIds) {
            AppService service = appServiceRepository.findById(serviceId).orElse(null);
            if (service == null) {
                log.warn("服务不存在: {}", serviceId);
                continue;
            }

            // 查询项目信息
            String projectName = "";
            Optional<Project> projectOpt = projectRepository.findById(service.getProjectId());
            if (projectOpt.isPresent()) {
                projectName = projectOpt.get().getProjectName();
            }

            // 查询服务器信息
            String serverIp = "";
            String serverAlias = "";
            Optional<Server> serverOpt = serverRepository.findById(service.getServerId());
            if (serverOpt.isPresent()) {
                Server server = serverOpt.get();
                serverIp = server.getIp();
                serverAlias = server.getAlias();
            }

            // 创建关联记录
            ScheduledDeploymentServiceEntity sds = new ScheduledDeploymentServiceEntity();
            sds.setTaskId(savedTask.getId());
            sds.setServiceId(serviceId);
            sds.setProjectName(projectName);
            sds.setServerIp(serverIp);
            sds.setServerAlias(serverAlias);
            sds.setDeployStatus(0); // 待执行

            scheduledDeploymentServiceRepository.save(sds);
            log.info("添加服务到任务: serviceId={}, projectName={}, serverIp={}",
                    serviceId, projectName, serverIp);
        }

        return savedTask;
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
     * 执行时间在当前时间前后2分钟内的未执行任务
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

            // 计算前2分钟和后2分钟的时间
            LocalDateTime timeBefore = now.minusMinutes(2);
            LocalDateTime timeAfter = now.plusMinutes(2);
            String timeBeforeStr = timeBefore.format(DateTimeFormatter.ofPattern("HH:mm"));
            String timeAfterStr = timeAfter.format(DateTimeFormatter.ofPattern("HH:mm"));

            log.info("Current date: {}, time: {}, time range: [{} - {}]", currentDate, currentTime, timeBeforeStr, timeAfterStr);

            // 查询待执行且执行时间在前后2分钟内的任务
            List<ScheduledDeployment> tasks = scheduledDeploymentRepository.findPendingTasksInTimeRange(
                    ScheduledStatus.PENDING.getCode(),
                    currentDate,
                    timeBeforeStr,
                    timeAfterStr
            );

            log.info("Found {} pending tasks to execute", tasks.size());

            // 异步执行每个任务
            for (ScheduledDeployment task : tasks) {
                executeScheduledDeploymentAsync(task);
            }

        } catch (Exception e) {
            log.error("Error checking scheduled deployment tasks: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行单个定时发版任务（支持多服务）
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

        try {
            log.info("执行定时发版任务: {} - {}", task.getId(), task.getName());

            // 标记任务为执行中（避免重复执行）
            executingTasks.put(task.getId(), null);

            // 获取任务的所有关联服务
            List<ScheduledDeploymentServiceEntity> serviceList =
                    scheduledDeploymentServiceRepository.findByTaskId(task.getId());

            if (serviceList == null || serviceList.isEmpty()) {
                log.warn("任务 {} 没有关联的服务，跳过执行", task.getId());
                return;
            }

            log.info("任务 {} 包含 {} 个服务，开始并发部署", task.getId(), serviceList.size());

            // 并发执行每个服务的部署
            for (ScheduledDeploymentServiceEntity sds : serviceList) {
                try {
                    executeServiceDeployment(task, sds);
                } catch (Exception e) {
                    log.error("服务部署失败: taskId={}, serviceId={}",
                            task.getId(), sds.getServiceId(), e);
                    // 更新部署状态为失败
                    sds.setDeployStatus(2);
                    scheduledDeploymentServiceRepository.save(sds);
                }
            }

            // 1. 标记任务为已完成
            task.setStatus(ScheduledStatus.COMPLETED.getCode());
            task.setUpdateTime(LocalDateTime.now());
            scheduledDeploymentRepository.save(task);

            log.info("定时发版任务完成: {} - {}", task.getId(), task.getName());

        } catch (Exception e) {
            log.error("执行定时发版任务失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
        } finally {
            // 移除执行锁
            executingTasks.remove(task.getId());
        }
    }

    /**
     * 执行单个服务的部署
     */
    private void executeServiceDeployment(ScheduledDeployment task, ScheduledDeploymentServiceEntity sds) throws Exception {
        Session sshSession = null;
        try {
            log.info("开始部署服务: taskId={}, serviceId={}", task.getId(), sds.getServiceId());

            // 2. 获取服务信息
            Optional<AppService> serviceOpt = appServiceRepository.findById(sds.getServiceId());
            if (serviceOpt.isEmpty()) {
                log.error("服务不存在: serviceId={}", sds.getServiceId());
                sds.setDeployStatus(2); // 失败
                scheduledDeploymentServiceRepository.save(sds);
                return;
            }
            AppService service = serviceOpt.get();

            Optional<Server> serverOpt = serverRepository.findById(service.getServerId());
            if (serverOpt.isEmpty()) {
                log.error("服务器不存在: serverId={}", service.getServerId());
                sds.setDeployStatus(2); // 失败
                scheduledDeploymentServiceRepository.save(sds);
                return;
            }
            Server server = serverOpt.get();

            Optional<Project> projectOpt = projectRepository.findById(service.getProjectId());
            if (projectOpt.isEmpty()) {
                log.error("项目不存在: projectId={}", service.getProjectId());
                sds.setDeployStatus(2); // 失败
                scheduledDeploymentServiceRepository.save(sds);
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
            String remoteDir = buildRemoteDir(project, service);

            // 生成日志文件路径
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String logDir = remoteDir + "/p_log";
            String logFilePath = logDir + "/" + timestamp + "_service" + service.getId() + ".log";
            record.setLogPath(logFilePath);

            record = deploymentRecordService.create(record);
            log.info("创建部署记录: recordId={}, taskId={}, serviceId={}",
                    record.getId(), task.getId(), sds.getServiceId());

            // 4. 执行发版流程
            doDeployment(sshSession, server, service, project, logFilePath, remoteDir);

            // 更新部署状态为成功
            sds.setDeployStatus(1); // 成功
            sds.setDeploymentRecordId(record.getId());
            scheduledDeploymentServiceRepository.save(sds);

            log.info("服务部署成功: taskId={}, serviceId={}, recordId={}",
                    task.getId(), sds.getServiceId(), record.getId());

        } catch (Exception e) {
            log.error("服务部署失败: taskId={}, serviceId={}, error={}",
                    task.getId(), sds.getServiceId(), e.getMessage(), e);
            sds.setDeployStatus(2); // 失败
            scheduledDeploymentServiceRepository.save(sds);
            throw e;
        } finally {
            // 关闭SSH连接
            if (sshSession != null) {
                SshUtils.disconnect(sshSession);
            }
        }
    }

    /**
     * 异步执行单个定时发版任务
     * 使用 @Async("scheduledDeploymentExecutor") 异步执行
     *
     * @param task 定时发版任务
     */
    @Async("scheduledDeploymentExecutor")
    private void executeScheduledDeploymentAsync(ScheduledDeployment task) {
        executeScheduledDeployment(task);
    }

    /**
     * 构建远程服务器部署目录路径
     * 如果配置了项目路径，则在基础路径后追加项目路径
     *
     * @param project 项目配置
     * @param service 服务配置
     * @return 远程部署目录路径，格式：{deployPath}/{projectCode} 或 {deployPath}/{projectCode}/{projectPath}
     */
    private String buildRemoteDir(Project project, AppService service) {
        String baseDir = project.getDeployPath() + "/" + project.getProjectCode();

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
