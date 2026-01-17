package com.opster.handler;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.opster.common.SshUtils;
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

    private final Map<String, SshSessionHolder> sessionMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri().getPath();
        // /ws/log/{serviceId}
        String serviceIdStr = path.substring(path.lastIndexOf('/') + 1);
        Integer serviceId = Integer.parseInt(serviceIdStr);

        log.info("WebSocket connected for service: {}", serviceId);
        
        startLogTail(session, serviceId);
    }

    private void startLogTail(WebSocketSession wsSession, Integer serviceId) {
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
                log.error("Error tailing log", e);
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
