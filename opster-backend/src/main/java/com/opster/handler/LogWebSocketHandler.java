package com.opster.handler;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.opster.common.SshUtils;
import com.opster.module.deployment.entity.DeploymentRecord;
import com.opster.module.deployment.repository.DeploymentRecordRepository;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.repository.AppServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
public class LogWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LogWebSocketHandler.class);

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private DeploymentRecordRepository deploymentRecordRepository;

    private final Map<String, SshSessionHolder> sessionMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri().getPath();
        log.info("WebSocket connected with path: {}", path);
        
        // 检查路径格式
        if (path.contains("/deployment/")) {
            // /ws/log/deployment/{deploymentId}
            String deploymentIdStr = path.substring(path.lastIndexOf('/') + 1);
            Integer deploymentId = Integer.parseInt(deploymentIdStr);
            startDeploymentLogTail(session, deploymentId);
        } else {
            // /ws/log/{serviceId}
            String serviceIdStr = path.substring(path.lastIndexOf('/') + 1);
            Integer serviceId = Integer.parseInt(serviceIdStr);
            startServiceLogTail(session, serviceId);
        }
    }

    private void startServiceLogTail(WebSocketSession wsSession, Integer serviceId) {
        executorService.submit(() -> {
            SshSessionHolder holder = new SshSessionHolder();
            sessionMap.put(wsSession.getId(), holder);

            try {
                Optional<AppService> serviceOpt = appServiceRepository.findById(serviceId);
                if (serviceOpt.isEmpty()) {
                    wsSession.sendMessage(new TextMessage("Service not found"));
                    wsSession.close();
                    return;
                }
                AppService service = serviceOpt.get();

                Optional<Server> serverOpt = serverRepository.findById(service.getServerId());
                if (serverOpt.isEmpty()) {
                    wsSession.sendMessage(new TextMessage("Server not found"));
                    wsSession.close();
                    return;
                }
                Server server = serverOpt.get();

                if (service.getLogPath() == null || service.getLogPath().isEmpty()) {
                    wsSession.sendMessage(new TextMessage("Log path not configured"));
                    wsSession.close();
                    return;
                }

                // Connect SSH
                Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());
                holder.sshSession = sshSession;

                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                // tail -100f command
                channel.setCommand("tail -100f " + service.getLogPath());
                InputStream in = channel.getInputStream();
                channel.connect();
                holder.channel = channel;

                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null && wsSession.isOpen()) {
                    wsSession.sendMessage(new TextMessage(line));
                }

            } catch (Exception e) {
                log.error("Error tailing service log", e);
                try {
                    if (wsSession.isOpen()) {
                        wsSession.sendMessage(new TextMessage("Error: " + e.getMessage()));
                    }
                } catch (Exception ignored) {}
            } finally {
                closeSshSession(wsSession.getId());
            }
        });
    }

    private void startDeploymentLogTail(WebSocketSession wsSession, Integer deploymentId) {
        executorService.submit(() -> {
            SshSessionHolder holder = new SshSessionHolder();
            sessionMap.put(wsSession.getId(), holder);

            try {
                Optional<DeploymentRecord> recordOpt = deploymentRecordRepository.findById(deploymentId);
                if (recordOpt.isEmpty()) {
                    wsSession.sendMessage(new TextMessage("Deployment record not found"));
                    wsSession.close();
                    return;
                }
                DeploymentRecord record = recordOpt.get();

                if (record.getLogPath() == null || record.getLogPath().isEmpty()) {
                    wsSession.sendMessage(new TextMessage("Log path not configured"));
                    wsSession.close();
                    return;
                }

                // 获取服务器信息
                Optional<Server> serverOpt = serverRepository.findById(record.getServerId());
                if (serverOpt.isEmpty()) {
                    wsSession.sendMessage(new TextMessage("Server not found"));
                    wsSession.close();
                    return;
                }
                Server server = serverOpt.get();

                wsSession.sendMessage(new TextMessage("=== Deployment Log ==="));
                wsSession.sendMessage(new TextMessage("Log Path: " + record.getLogPath()));
                wsSession.sendMessage(new TextMessage("Project: " + record.getProjectName()));
                wsSession.sendMessage(new TextMessage("Server: " + record.getServerIp() + " (" + record.getServerAlias() + ")"));
                wsSession.sendMessage(new TextMessage("Service: " + record.getServiceName()));
                wsSession.sendMessage(new TextMessage("Status: " + (record.getStatus() != null ? record.getStatus().getName() : "Unknown")));
                wsSession.sendMessage(new TextMessage("=== Log Content (tail -100f) ===\n"));

                // Connect SSH
                Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());
                holder.sshSession = sshSession;

                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                // tail -100f command
                channel.setCommand("tail -100f " + record.getLogPath());
                InputStream in = channel.getInputStream();
                channel.connect();
                holder.channel = channel;

                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null && wsSession.isOpen()) {
                    wsSession.sendMessage(new TextMessage(line));
                }

            } catch (Exception e) {
                log.error("Error tailing deployment log", e);
                try {
                    if (wsSession.isOpen()) {
                        wsSession.sendMessage(new TextMessage("Error: " + e.getMessage()));
                    }
                } catch (Exception ignored) {}
            } finally {
                closeSshSession(wsSession.getId());
                try {
                    if (wsSession.isOpen()) {
                        wsSession.close();
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket closed for session: {}", session.getId());
        closeSshSession(session.getId());
    }

    private void closeSshSession(String sessionId) {
        SshSessionHolder holder = sessionMap.remove(sessionId);
        if (holder != null) {
            if (holder.channel != null) {
                holder.channel.disconnect();
            }
            if (holder.sshSession != null) {
                SshUtils.disconnect(holder.sshSession);
            }
        }
    }

    private static class SshSessionHolder {
        Session sshSession;
        ChannelExec channel;
    }
}
