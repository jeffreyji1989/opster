package com.opster.handler;

import cn.hutool.json.JSONUtil;
import com.opster.module.monitor.entity.ServerMetricDTO;
import com.opster.module.monitor.service.ServerMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 服务器监控 WebSocket 处理器
 * 协议: 客户端发送 {"serverId": 1} 开启监控
 */
@Component
public class MonitorWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MonitorWebSocketHandler.class);

    @Autowired
    private ServerMonitorService serverMonitorService;

    // Store scheduled tasks for each session
    private final Map<String, ScheduledFuture<?>> sessionTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Monitor WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Monitor received: {}", payload);

        // Cancel existing task for this session if any (switching servers)
        stopMonitoring(session.getId());

        try {
            Integer serverId = Integer.parseInt(JSONUtil.parseObj(payload).getStr("serverId"));
            
            // Start new monitoring task
            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (!session.isOpen()) {
                        stopMonitoring(session.getId());
                        return;
                    }

                    ServerMetricDTO metric = serverMonitorService.getCurrentMetric(serverId);
                    
                    // Send to client
                    session.sendMessage(new TextMessage(JSONUtil.toJsonStr(metric)));
                    
                    // Async save snapshot (optional, maybe every 10th time or separate logic)
                    // For now, let's just push real-time data. 
                    // History saving should be a separate backend job to avoid relying on user viewing the page.
                    
                } catch (Exception e) {
                    log.error("Error sending monitor data", e);
                    try {
                        session.sendMessage(new TextMessage("{\"error\": \"" + e.getMessage() + "\"}"));
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }, 0, 3, TimeUnit.SECONDS); // 3 seconds interval

            sessionTasks.put(session.getId(), task);

        } catch (Exception e) {
            session.sendMessage(new TextMessage("{\"error\": \"Invalid format\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Monitor WebSocket closed: {}", session.getId());
        stopMonitoring(session.getId());
    }

    private void stopMonitoring(String sessionId) {
        ScheduledFuture<?> task = sessionTasks.remove(sessionId);
        if (task != null) {
            task.cancel(true);
        }
    }
}
