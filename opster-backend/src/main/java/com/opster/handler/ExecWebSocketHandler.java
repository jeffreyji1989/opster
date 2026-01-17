package com.opster.handler;

import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.opster.common.SshUtils;
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
                
                String cmdToExec = "";

                if ("compile-restart".equals(action)) {
                    // Git Pull/Clone
                    sendMessage(wsSession, ">>> Checking out source code...");
                    String gitUrl = project.getGitUrl();
                    String branch = service.getGitBranch();
                    String gitCmd = String.format(
                        "if [ -d \"%s/source/.git\" ]; then cd \"%s/source\" && git checkout %s && git pull; else mkdir -p \"%s\" && cd \"%s\" && git clone -b %s %s source; fi",
                        deployPath, deployPath, branch, deployPath, deployPath, branch, gitUrl
                    );
                    executeCommand(wsSession, sshSession, gitCmd);

                    // Maven Build
                    sendMessage(wsSession, ">>> Executing Maven build...");
                    String mavenCmd = service.getMavenCmd();
                    String buildCmd = String.format("source /etc/profile && %s && cd %s/source && %s", envVars, deployPath, mavenCmd);
                    executeCommand(wsSession, sshSession, buildCmd);

                    // Copy Jar
                    sendMessage(wsSession, ">>> Deploying Jar...");
                    String copyCmd = String.format("cp %s/source/target/*.jar %s/", deployPath, deployPath);
                    executeCommand(wsSession, sshSession, copyCmd);

                    // Restart
                    sendMessage(wsSession, ">>> Restarting service...");
                    String startScript = service.getStartScript();
                    String restartCmd = String.format("source /etc/profile && %s && cd %s && sh %s", envVars, deployPath, startScript);
                    executeCommand(wsSession, sshSession, restartCmd);
                    
                } else if ("restart".equals(action) || "start".equals(action)) {
                    sendMessage(wsSession, ">>> " + (action.equals("restart") ? "Restarting" : "Starting") + " service...");
                    String startScript = service.getStartScript();
                    String restartCmd = String.format("source /etc/profile && %s && cd %s && sh %s", envVars, deployPath, startScript);
                    executeCommand(wsSession, sshSession, restartCmd);
                }

                // Check Port
                if (service.getPort() != null) {
                    sendMessage(wsSession, ">>> Checking port " + service.getPort() + "...");
                    boolean isStarted = checkPort(wsSession, sshSession, service.getPort());
                    if (isStarted) {
                        sendMessage(wsSession, ">>> Service started successfully (Port is listening).");
                        service.setStatus(1); // Normal
                    } else {
                        sendMessage(wsSession, ">>> Service failed to start (Port is not listening).");
                        service.setStatus(2); // Error
                    }
                    appServiceRepository.save(service);
                } else {
                    sendMessage(wsSession, ">>> Warning: Port not configured, skip health check.");
                }

                sendMessage(wsSession, ">>> Done.");

            } catch (Exception e) {
                log.error("Error executing command", e);
                sendMessage(wsSession, "ERROR: " + e.getMessage());
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
