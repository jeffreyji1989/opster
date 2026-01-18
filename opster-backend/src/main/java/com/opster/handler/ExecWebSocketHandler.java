package com.opster.handler;

import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.opster.common.SshUtils;
import com.opster.common.enums.RunStatus;
import com.opster.common.enums.Status;
import com.opster.common.enums.DeploymentStatus;
import com.opster.module.deployment.entity.DeploymentRecord;
import com.opster.module.deployment.service.DeploymentRecordService;
import com.opster.module.project.entity.Project;
import com.opster.module.project.repository.ProjectRepository;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.repository.AppServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ExecWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ExecWebSocketHandler.class);

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

    private final Map<String, SshSessionHolder> sessionMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // /ws/exec/{serviceId}/{action}
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length < 5) {
            session.close();
            return;
        }
        
        String serviceIdStr = parts[3];
        String action = parts[4];
        Integer serviceId = Integer.parseInt(serviceIdStr);

        log.info("Exec WebSocket connected for service: {}, action: {}", serviceId, action);
        
        startExec(session, serviceId, action);
    }

    private void startExec(WebSocketSession wsSession, Integer serviceId, String action) {
        executorService.submit(() -> {
            SshSessionHolder holder = new SshSessionHolder();
            sessionMap.put(wsSession.getId(), holder);

            DeploymentRecord deploymentRecord = null;
            String logFilePath = null;
            
            // 添加状态跟踪变量
            boolean isGitSuccess = true;
            boolean isMavenSuccess = true;
            boolean isDeploySuccess = true;
            boolean isStartSuccess = true;
            boolean isOverallSuccess = true;

            try {
                Optional<AppService> serviceOpt = appServiceRepository.findById(serviceId);
                if (serviceOpt.isEmpty()) {
                    sendMessage(wsSession, "Service not found");
                    wsSession.close();
                    return;
                }
                AppService service = serviceOpt.get();

                Optional<Server> serverOpt = serverRepository.findById(service.getServerId());
                if (serverOpt.isEmpty()) {
                    sendMessage(wsSession, "Server not found");
                    wsSession.close();
                    return;
                }
                Server server = serverOpt.get();
                
                Project project = projectRepository.findById(service.getProjectId()).orElse(null);
                if (project == null) {
                    sendMessage(wsSession, "Project not found");
                    wsSession.close();
                    return;
                }

                // 1. Connect SSH
                sendMessage(wsSession, ">>> Connecting to " + server.getIp() + "...");
                Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());
                holder.sshSession = sshSession;
                sendMessage(wsSession, ">>> Connected.");

                // Prepare Env
                String envVars = String.format("export JAVA_HOME=%s && export PATH=$JAVA_HOME/bin:$PATH && export M2_HOME=%s && export PATH=$M2_HOME/bin:$PATH", javaHome, mavenHome);
                String deployPath = service.getDeployPath();
                
                // 生成日志文件路径
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                String logDir = deployPath + "/p_log";
                logFilePath = logDir + "/" + timestamp + ".log";
                
                // 创建p_log目录（如果不存在）
                String createLogDirCmd = String.format("mkdir -p %s", logDir);
                executeCommand(wsSession, sshSession, createLogDirCmd);
                
                // 写入开始日志
                String startLog = "=== Deployment Start: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " ===\n";
                String writeStartLogCmd = String.format("echo '%s' > %s", startLog, logFilePath);
                executeCommand(wsSession, sshSession, writeStartLogCmd);

                // Create deployment record
                deploymentRecord = new DeploymentRecord();
                deploymentRecord.setProjectId(project.getId());
                deploymentRecord.setProjectName(project.getProjectName());
                deploymentRecord.setServerId(server.getId());
                deploymentRecord.setServerIp(server.getIp());
                deploymentRecord.setServerAlias(server.getAlias());
                deploymentRecord.setServiceId(service.getId());
                deploymentRecord.setServiceName("Service-" + service.getId()); // Use service ID as name
                deploymentRecord.setLogPath(logFilePath);
                deploymentRecord.setStatus(DeploymentStatus.IN_PROGRESS);
                deploymentRecord = deploymentRecordService.create(deploymentRecord);

                if ("deploy".equals(action)) {
                    // Git Pull/Clone
                    String gitMessage = ">>> Checking out source code...";
                    sendMessage(wsSession, gitMessage);
                    appendToLogFile(sshSession, logFilePath, gitMessage + "\n");
                    
                    String gitUrl = project.getGitUrl();
                    String branch = service.getGitBranch();
                    String gitCmd = String.format(
                        "if [ -d \"%s/source/.git\" ]; then cd \"%s/source\" && git checkout %s && git pull; else mkdir -p \"%s\" && cd \"%s\" && git clone -b %s %s source; fi",
                        deployPath, deployPath, branch, deployPath, deployPath, branch, gitUrl
                    );
                    isGitSuccess = executeCommandWithLog(wsSession, sshSession, gitCmd, logFilePath);
                    if (!isGitSuccess) {
                        sendMessage(wsSession, ">>> Git operation failed!");
                        isOverallSuccess = false;
                    }

                    // Maven Build
                    if (isOverallSuccess) {
                        String mavenMessage = ">>> Executing Maven build...";
                        sendMessage(wsSession, mavenMessage);
                        appendToLogFile(sshSession, logFilePath, mavenMessage + "\n");
                        
                        String mavenCmd = service.getMavenCmd();
                        String buildCmd = String.format("source /etc/profile && %s && cd %s/source && %s", envVars, deployPath, mavenCmd);
                        isMavenSuccess = executeCommandWithLog(wsSession, sshSession, buildCmd, logFilePath);
                        if (!isMavenSuccess) {
                            sendMessage(wsSession, ">>> Maven build failed!");
                            isOverallSuccess = false;
                        }
                    }

                    // Copy Jar
                    if (isOverallSuccess) {
                        String copyMessage = ">>> Deploying Jar...";
                        sendMessage(wsSession, copyMessage);
                        appendToLogFile(sshSession, logFilePath, copyMessage + "\n");
                        
                        // 备份现有jar文件
                        String backupMessage = ">>> Backing up existing Jar files...";
                        sendMessage(wsSession, backupMessage);
                        appendToLogFile(sshSession, logFilePath, backupMessage + "\n");
                        
                        // 执行备份操作
                        String backupCmd = String.format("bash -c '\n" +
                            "# 创建bak目录（如果不存在）\n" +
                            "mkdir -p %s/bak\n" +
                            "\n" +
                            "# 检查bak目录是否为空\n" +
                            "if [ -z \"$(ls -A %s/bak 2>/dev/null)\" ]; then\n" +
                            "    # 如果bak目录为空，直接复制部署目录下的jar文件到bak目录\n" +
                            "    echo \"Bak directory is empty, copying current jars...\"\n" +
                            "    cp -f %s/*.jar %s/bak/ 2>/dev/null || echo \"No jars found to backup\"\n" +
                            "else\n" +
                            "    # 如果bak目录不为空，按日期备份原有jar文件\n" +
                            "    echo \"Bak directory is not empty, backing up with timestamp...\"\n" +
                            "    timestamp=$(date +\"%%Y%%m%%d%%H%%M%%S\")\n" +
                            "    for jar in %s/bak/*.jar; do\n" +
                            "        if [ -f \"$jar\" ]; then\n" +
                            "            base_name=$(basename \"$jar\")\n" +
                            "            new_name=\"${base_name%%.jar}_${timestamp}.jar\"\n" +
                            "            cp -f \"$jar\" \"%s/bak/${new_name}\"\n" +
                            "            echo \"Backed up $base_name to $new_name\"\n" +
                            "        fi\n" +
                            "    done\n" +
                            "    # 复制当前部署目录下的jar文件到bak目录\n" +
                            "    cp -f %s/*.jar %s/bak/ 2>/dev/null || echo \"No jars found to backup\"\n" +
                            "fi\n" +
                            "'", deployPath, deployPath, deployPath, deployPath, deployPath, deployPath, deployPath, deployPath);
                        
                        boolean isBackupSuccess = executeCommandWithLog(wsSession, sshSession, backupCmd, logFilePath);
                        if (!isBackupSuccess) {
                            sendMessage(wsSession, ">>> Jar backup failed!");
                            isOverallSuccess = false;
                        } else {
                            // 复制新的jar文件到部署目录
                            String copyCmd = String.format("cp %s/source/target/*.jar %s/", deployPath, deployPath);
                            isDeploySuccess = executeCommandWithLog(wsSession, sshSession, copyCmd, logFilePath);
                            if (!isDeploySuccess) {
                                sendMessage(wsSession, ">>> Jar deployment failed!");
                                isOverallSuccess = false;
                            }
                        }
                    }

                    // Restart
                    if (isOverallSuccess) {
                        String restartMessage = ">>> Restarting service...";
                        sendMessage(wsSession, restartMessage);
                        appendToLogFile(sshSession, logFilePath, restartMessage + "\n");
                        
                        String startScript = service.getStartScript();
                        String restartCmd = String.format("source /etc/profile && %s && cd %s && sh %s", envVars, deployPath, startScript);
                        isStartSuccess = executeCommandWithLog(wsSession, sshSession, restartCmd, logFilePath);
                        if (!isStartSuccess) {
                            sendMessage(wsSession, ">>> Service restart failed!");
                            isOverallSuccess = false;
                        }
                    }
                    
                } else if ("restart".equals(action) || "start".equals(action)) {
                    String actionMessage = ">>> " + (action.equals("restart") ? "Restarting" : "Starting") + " service...";
                    sendMessage(wsSession, actionMessage);
                    appendToLogFile(sshSession, logFilePath, actionMessage + "\n");
                    
                    String startScript = service.getStartScript();
                    String restartCmd = String.format("source /etc/profile && %s && cd %s && sh %s", envVars, deployPath, startScript);
                    isStartSuccess = executeCommandWithLog(wsSession, sshSession, restartCmd, logFilePath);
                    if (!isStartSuccess) {
                        sendMessage(wsSession, ">>> Service " + action + " failed!");
                        isOverallSuccess = false;
                    }
                } else if ("rollback".equals(action)) {
                    String rollbackMessage = ">>> Rolling back service...";
                    sendMessage(wsSession, rollbackMessage);
                    appendToLogFile(sshSession, logFilePath, rollbackMessage + "\n");
                    
                    // 执行版本回退操作
                    String rollbackCmd = String.format("bash -c '\n" +
                        "# 查找部署目录下的jar文件\n" +
                        "deploy_jars=$(ls -1 %s/*.jar 2>/dev/null | head -1)\n" +
                        "if [ -z \"$deploy_jars\" ]; then\n" +
                        "    echo \"No jars found in deploy directory\"\n" +
                        "    exit 1\n" +
                        "fi\n" +
                        "\n" +
                        "# 获取jar文件名\n" +
                        "jar_name=$(basename \"$deploy_jars\")\n" +
                        "echo \"Found jar: $jar_name\"\n" +
                        "\n" +
                        "# 在bak目录中查找相同名字的文件\n" +
                        "bak_jar=%s/bak/$jar_name\n" +
                        "if [ ! -f \"$bak_jar\" ]; then\n" +
                        "    echo \"No backup found for $jar_name\"\n" +
                        "    exit 1\n" +
                        "fi\n" +
                        "\n" +
                        "# 将bak目录中的文件覆盖到部署目录\n" +
                        "echo \"Rolling back $jar_name from backup\"\n" +
                        "cp -f \"$bak_jar\" \"%s/\"\n" +
                        "echo \"Rollback completed\"\n" +
                        "'", deployPath, deployPath, deployPath);
                    
                    boolean isRollbackSuccess = executeCommandWithLog(wsSession, sshSession, rollbackCmd, logFilePath);
                    if (!isRollbackSuccess) {
                        sendMessage(wsSession, ">>> Rollback failed!");
                        isOverallSuccess = false;
                    } else {
                        // 重启服务
                        String restartMessage = ">>> Restarting service after rollback...";
                        sendMessage(wsSession, restartMessage);
                        appendToLogFile(sshSession, logFilePath, restartMessage + "\n");
                        
                        String startScript = service.getStartScript();
                        String restartCmd = String.format("source /etc/profile && %s && cd %s && sh %s", envVars, deployPath, startScript);
                        isStartSuccess = executeCommandWithLog(wsSession, sshSession, restartCmd, logFilePath);
                        if (!isStartSuccess) {
                            sendMessage(wsSession, ">>> Service restart failed after rollback!");
                            isOverallSuccess = false;
                        }
                    }
                }

                // Check Port
                if (service.getPort() != null) {
                    String portMessage = ">>> Checking port " + service.getPort() + "...";
                    sendMessage(wsSession, portMessage);
                    appendToLogFile(sshSession, logFilePath, portMessage + "\n");
                    
                    boolean isStarted = checkPortWithLog(wsSession, sshSession, service.getPort(), logFilePath);
                    if (isStarted && isOverallSuccess) {
                        String successMessage = ">>> Service started successfully (Port is listening).";
                        sendMessage(wsSession, successMessage);
                        appendToLogFile(sshSession, logFilePath, successMessage + "\n");
                        service.setRunStatus(RunStatus.NORMAL); // Normal
                        deploymentRecord.setStatus(DeploymentStatus.COMPLETED);
                    } else {
                        String failureMessage = ">>> Service failed to start (Port is not listening or previous step failed).";
                        sendMessage(wsSession, failureMessage);
                        appendToLogFile(sshSession, logFilePath, failureMessage + "\n");
                        service.setRunStatus(RunStatus.ABNORMAL); // Error
                        deploymentRecord.setStatus(DeploymentStatus.FAILED);
                    }
                    appServiceRepository.save(service);
                    deploymentRecordService.update(deploymentRecord);
                } else {
                    String warningMessage = ">>> Warning: Port not configured, skip health check.";
                    sendMessage(wsSession, warningMessage);
                    appendToLogFile(sshSession, logFilePath, warningMessage + "\n");
                    
                    if (isOverallSuccess) {
                        deploymentRecord.setStatus(DeploymentStatus.COMPLETED);
                    } else {
                        deploymentRecord.setStatus(DeploymentStatus.FAILED);
                    }
                    deploymentRecordService.update(deploymentRecord);
                }

                String doneMessage = ">>> Done.";
                sendMessage(wsSession, doneMessage);
                appendToLogFile(sshSession, logFilePath, doneMessage + "\n");
                
                // 写入结束日志
                String endLog = "=== Deployment End: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " ===\n";
                appendToLogFile(sshSession, logFilePath, endLog);

            } catch (Exception e) {
                log.error("Error executing command", e);
                String errorMessage = "ERROR: " + e.getMessage();
                sendMessage(wsSession, errorMessage);
                
                // 写入错误日志到文件
                if (logFilePath != null) {
                    try {
                        appendToLogFile(holder.sshSession, logFilePath, errorMessage + "\n");
                    } catch (Exception ignored) {}
                }
                
                // Update deployment record status to FAILED
                if (deploymentRecord != null) {
                    deploymentRecord.setStatus(DeploymentStatus.FAILED);
                    deploymentRecordService.update(deploymentRecord);
                }
            } finally {
                closeSshSession(wsSession.getId());
                try {
                    wsSession.close();
                } catch (Exception ignored) {}
            }
        });
    }

    private void executeCommand(WebSocketSession wsSession, Session sshSession, String command) throws Exception {
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
    
    private boolean executeCommandWithLog(WebSocketSession wsSession, Session sshSession, String command, String logFilePath) throws Exception {
        // 先将命令写入日志
        appendToLogFile(sshSession, logFilePath, "$ " + command + "\n");
        
        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setCommand(command);
        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        BufferedReader errReader = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8));
        
        String line;
        boolean isSuccess = true;
        
        while ((line = reader.readLine()) != null) {
            sendMessage(wsSession, line);
            appendToLogFile(sshSession, logFilePath, line + "\n");
            
            // 分析日志，识别成功/失败标识
            isSuccess = analyzeLogLine(line, isSuccess);
        }
        while ((line = errReader.readLine()) != null) {
             sendMessage(wsSession, line);
             appendToLogFile(sshSession, logFilePath, line + "\n");
             
             // 分析错误日志，识别失败标识
             isSuccess = analyzeLogLine(line, isSuccess);
        }
        
        channel.disconnect();
        return isSuccess;
    }
    
    private boolean analyzeLogLine(String line, boolean currentStatus) {
        // 分析日志行，识别成功/失败标识
        line = line.toLowerCase();
        
        // 识别maven打包失败
        if (line.contains("build failure")) {
            return false;
        }
        
        // 识别springboot启动失败
        if (line.contains("error starting applicationcontext") || 
            line.contains("exception in thread \"main\"")) {
            return false;
        }
        
        // 识别maven打包成功
        if (line.contains("build success")) {
            return true;
        }
        
        // 识别springboot启动成功
        if (line.contains("started application in") || 
            line.contains("tomcat started on port(s)")) {
            return true;
        }
        
        return currentStatus;
    }
    
    private boolean checkPort(WebSocketSession wsSession, Session sshSession, int port) {
         for (int i = 0; i < 30; i++) {
             try {
                 String cmd = String.format("netstat -tln | grep :%d", port);
                 ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                 channel.setCommand(cmd);
                 InputStream in = channel.getInputStream();
                 channel.connect();
                 
                 // Read output to determine if port is listening
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                 String output = "";
                 String line;
                 while ((line = reader.readLine()) != null) {
                     output += line;
                     sendMessage(wsSession, line);
                 }
                 channel.disconnect();
                 
                 if (StrUtil.isNotBlank(output)) {
                     return true;
                 }
                 Thread.sleep(1000);
             } catch (Exception e) {
                 // ignore
             }
         }
         return false;
    }
    
    private boolean checkPortWithLog(WebSocketSession wsSession, Session sshSession, int port, String logFilePath) {
         for (int i = 0; i < 30; i++) {
             try {
                 String cmd = String.format("netstat -tln | grep :%d", port);
                 ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                 channel.setCommand(cmd);
                 InputStream in = channel.getInputStream();
                 channel.connect();
                 
                 // Read output to determine if port is listening
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                 String output = "";
                 String line;
                 while ((line = reader.readLine()) != null) {
                     output += line;
                     sendMessage(wsSession, line);
                     appendToLogFile(sshSession, logFilePath, line + "\n");
                 }
                 channel.disconnect();
                 
                 if (StrUtil.isNotBlank(output)) {
                     return true;
                 }
                 Thread.sleep(1000);
             } catch (Exception e) {
                 // ignore
             }
         }
         return false;
    }
    
    private void appendToLogFile(Session sshSession, String logFilePath, String content) throws Exception {
        // 使用echo命令将内容追加到日志文件
        String safeContent = content.replace("'", "\\'"); // 转义单引号
        String cmd = String.format("echo '%s' >> %s", safeContent, logFilePath);
        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setCommand(cmd);
        channel.connect();
        channel.disconnect();
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.error("Error sending message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Exec WebSocket closed for session: {}", session.getId());
        closeSshSession(session.getId());
    }

    private void closeSshSession(String sessionId) {
        SshSessionHolder holder = sessionMap.remove(sessionId);
        if (holder != null) {
            if (holder.sshSession != null) {
                SshUtils.disconnect(holder.sshSession);
            }
        }
    }

    private static class SshSessionHolder {
        Session sshSession;
    }
}
