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
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TerminalWebSocketHandler.class);

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Autowired
    private ServerRepository serverRepository;

    private final Map<String, SshSessionHolder> sessionMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // /ws/terminal/{serviceId}
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            session.close();
            return;
        }
        
        String serviceIdStr = parts[3];
        Integer serviceId = Integer.parseInt(serviceIdStr);

        log.info("Terminal WebSocket connected for service: {}", serviceId);
        
        startTerminal(session, serviceId);
    }

    private void startTerminal(WebSocketSession wsSession, Integer serviceId) {
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

                // Connect SSH
                sendMessage(wsSession, ">>> Connecting to " + server.getIp() + "...");
                // SshUtils.connect 内部会自动解密密码
                Session sshSession = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());
                holder.sshSession = sshSession;
                
                // Create persistent shell channel
                com.jcraft.jsch.ChannelShell shellChannel = (com.jcraft.jsch.ChannelShell) sshSession.openChannel("shell");
                shellChannel.setPty(true);
                shellChannel.setPtyType("xterm");
                shellChannel.connect();
                
                // Get input and output streams
                java.io.InputStream shellInput = shellChannel.getInputStream();
                java.io.OutputStream shellOutput = shellChannel.getOutputStream();
                
                holder.shellChannel = shellChannel;
                holder.shellOutput = shellOutput;
                holder.shellInput = shellInput;
                
                // Start reading from shell input
                startShellReader(wsSession, holder);
                
                sendMessage(wsSession, ">>> Connected. Ready for commands.\r\n");

            } catch (Exception e) {
                log.error("Error starting terminal", e);
                sendMessage(wsSession, "ERROR: " + e.getMessage() + "\r\n");
                try {
                    wsSession.close();
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * Start reading from shell input stream
     */
    private void startShellReader(WebSocketSession wsSession, SshSessionHolder holder) {
        executorService.submit(() -> {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while (holder.shellChannel != null && holder.shellChannel.isConnected() && (bytesRead = holder.shellInput.read(buffer)) != -1) {
                    String output = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    sendMessage(wsSession, output);
                }
            } catch (Exception e) {
                if (!(e instanceof java.io.IOException && "Stream closed".equals(e.getMessage()))) {
                    log.error("Error reading from shell", e);
                    sendMessage(wsSession, "ERROR: " + e.getMessage() + "\r\n");
                }
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String data = message.getPayload();
        SshSessionHolder holder = sessionMap.get(session.getId());
        
        if (holder == null || holder.sshSession == null || !holder.sshSession.isConnected()) {
            sendMessage(session, "ERROR: SSH session not connected");
            return;
        }

        executorService.submit(() -> {
            try {
                // Handle terminal resize
                if (data.startsWith("\u0000")) {
                    // Terminal resize request
                    handleTerminalResize(data, holder);
                } else {
                    // Regular command - use persistent shell channel
                    if (holder.shellChannel != null && holder.shellChannel.isConnected() && holder.shellOutput != null) {
                        // Send data to shell channel
                        holder.shellOutput.write(data.getBytes(StandardCharsets.UTF_8));
                        holder.shellOutput.flush();
                    } else {
                        sendMessage(session, "ERROR: Shell channel not connected");
                    }
                }
            } catch (Exception e) {
                log.error("Error handling terminal message", e);
                sendMessage(session, "ERROR: " + e.getMessage());
            }
        });
    }

    /**
     * Handle terminal resize request
     */
    private void handleTerminalResize(String data, SshSessionHolder holder) throws Exception {
        // Parse resize request
        // Format: \u0000{cols},{rows}\u0000
        String resizeData = data.substring(1, data.length() - 1);
        String[] parts = resizeData.split(",");
        if (parts.length == 2) {
            int cols = Integer.parseInt(parts[0]);
            int rows = Integer.parseInt(parts[1]);
            log.info("Terminal resize: {}x{}", cols, rows);
            
            // Resize terminal if needed
            // This would require a PTY channel, which we don't have yet
            // For now, just log the resize request
        }
    }

    private void executeCommand(WebSocketSession wsSession, Session sshSession, String command) throws Exception {
        // Use PTY channel for interactive terminal support
        com.jcraft.jsch.ChannelShell channel = (com.jcraft.jsch.ChannelShell) sshSession.openChannel("shell");
        
        // Set terminal type
        channel.setPty(true);
        channel.setPtyType("xterm");
        
        // Get input and output streams
        java.io.InputStream in = channel.getInputStream();
        java.io.OutputStream out = channel.getOutputStream();
        
        // Connect channel
        channel.connect();
        
        // Send command
        out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
        
        // Read output
        byte[] buffer = new byte[1024];
        int bytesRead;
        while (channel.isConnected() && (bytesRead = in.read(buffer)) != -1) {
            String output = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            sendMessage(wsSession, output);
        }
        
        // Disconnect channel
        channel.disconnect();
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                // Use binary message for better performance with xterm.js
                session.sendMessage(new org.springframework.web.socket.BinaryMessage(message.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            log.error("Error sending message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Terminal WebSocket closed for session: {}", session.getId());
        closeSshSession(session.getId());
    }

    private void closeSshSession(String sessionId) {
        SshSessionHolder holder = sessionMap.remove(sessionId);
        if (holder != null) {
            // Close shell channel
            if (holder.shellChannel != null && holder.shellChannel.isConnected()) {
                try {
                    if (holder.shellOutput != null) {
                        holder.shellOutput.close();
                    }
                    if (holder.shellInput != null) {
                        holder.shellInput.close();
                    }
                    holder.shellChannel.disconnect();
                } catch (Exception e) {
                    log.error("Error closing shell channel", e);
                }
            }
            // Close SSH session
            if (holder.sshSession != null) {
                SshUtils.disconnect(holder.sshSession);
            }
        }
    }

    private static class SshSessionHolder {
        Session sshSession;
        com.jcraft.jsch.ChannelShell shellChannel;
        java.io.OutputStream shellOutput;
        java.io.InputStream shellInput;
    }
}
