package com.opster.handler;

import com.opster.module.service.service.DeploymentOrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ExecWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private DeploymentOrchestrationService deploymentOrchestrationService;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 支持两种路由模式:
        // 1. /ws/exec/{serviceId}/{action} - 现有模式
        // 2. /ws/exec/rollback-to-version/{recordId} - 新模式
        String path = session.getUri().getPath();
        String[] parts = path.split("/");

        if (parts.length < 5) {
            session.close();
            return;
        }

        String actionOrSpecial = parts[3];

        // 检查是否是特殊路由 "rollback-to-version"
        if ("rollback-to-version".equals(actionOrSpecial) && parts.length >= 5) {
            String recordIdStr = parts[4];
            Integer recordId = Integer.parseInt(recordIdStr);
            log.info("Exec WebSocket connected for rollback-to-version: {}", recordId);
            startRollbackToVersion(session, recordId);
        } else {
            // 原有逻辑: /ws/exec/{serviceId}/{action}
            String serviceIdStr = parts[3];
            String action = parts[4];
            Integer serviceId = Integer.parseInt(serviceIdStr);
            log.info("Exec WebSocket connected for service: {}, action: {}", serviceId, action);
            startExec(session, serviceId, action);
        }
    }

    private void startExec(WebSocketSession wsSession, Integer serviceId, String action) {
        executorService.submit(() -> {
            try {
                switch (action) {
                    case "deploy":
                        deploymentOrchestrationService.executeDeployment(serviceId, wsSession);
                        break;
                    case "restart":
                        deploymentOrchestrationService.executeRestart(serviceId, wsSession);
                        break;
                    case "start":
                        deploymentOrchestrationService.executeStart(serviceId, wsSession);
                        break;
                    case "stop":
                        deploymentOrchestrationService.executeStop(serviceId, wsSession);
                        break;
                    case "rollback":
                        deploymentOrchestrationService.executeRollback(serviceId, wsSession);
                        break;
                    default:
                        sendMessage(wsSession, "Unknown action: " + action);
                }
            } catch (Exception e) {
                log.error("Error executing action: {} for service: {}", action, serviceId, e);
                sendMessage(wsSession, "ERROR: " + e.getMessage());
            } finally {
                try {
                    if (wsSession.isOpen()) {
                        wsSession.close();
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void startRollbackToVersion(WebSocketSession wsSession, Integer recordId) {
        executorService.submit(() -> {
            try {
                deploymentOrchestrationService.rollbackToSpecificVersion(recordId, wsSession);
            } catch (Exception e) {
                log.error("Error rollback to version: {}", recordId, e);
                sendMessage(wsSession, "ERROR: " + e.getMessage());
            } finally {
                try {
                    if (wsSession.isOpen()) {
                        wsSession.close();
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.error("Error sending message to WebSocket", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Exec WebSocket closed for session: {}", session.getId());
    }
}
