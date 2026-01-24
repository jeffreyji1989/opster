package com.opster.handler;

import com.opster.module.deployment.entity.DeploymentRecord;
import com.opster.module.deployment.repository.DeploymentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LogWebSocketHandler.class);

    @Autowired
    private DeploymentRecordRepository deploymentRecordRepository;

    private final Map<String, FileTailTask> taskMap = new ConcurrentHashMap<>();
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
            // /ws/log/{serviceId} - 暂时保留但标记为不支持
            session.sendMessage(new TextMessage("Service log viewing via WebSocket is not supported. Please use deployment logs."));
            session.close();
        }
    }

    private void startDeploymentLogTail(WebSocketSession wsSession, Integer deploymentId) {
        executorService.submit(() -> {
            boolean shouldCloseConnection = true; // 标记是否应该关闭连接

            try {
                Optional<DeploymentRecord> recordOpt = deploymentRecordRepository.findById(deploymentId);
                if (recordOpt.isEmpty()) {
                    wsSession.sendMessage(new TextMessage("部署记录不存在"));
                    wsSession.close();
                    return;
                }
                DeploymentRecord record = recordOpt.get();

                if (record.getLogPath() == null || record.getLogPath().isEmpty()) {
                    wsSession.sendMessage(new TextMessage("日志路径未配置"));
                    wsSession.close();
                    return;
                }

                String logFilePath = record.getLogPath();
                File logFile = new File(logFilePath);

                // 发送日志信息头
                wsSession.sendMessage(new TextMessage("=== 部署日志 ==="));
                wsSession.sendMessage(new TextMessage("项目: " + record.getProjectName()));
                wsSession.sendMessage(new TextMessage("服务器: " + record.getServerIp() + " (" + record.getServerAlias() + ")"));
                wsSession.sendMessage(new TextMessage("服务: " + record.getServiceName()));
                wsSession.sendMessage(new TextMessage("状态: " + (record.getStatus() != null ? record.getStatus().getName() : "未知")));
                wsSession.sendMessage(new TextMessage("日志路径: " + logFilePath));
                wsSession.sendMessage(new TextMessage("=== 日志内容 ===\n"));

                // 检查文件是否存在
                if (!logFile.exists()) {
                    wsSession.sendMessage(new TextMessage("日志文件不存在: " + logFilePath));
                    wsSession.close();
                    return;
                }

                // 如果部署已完成或失败，直接读取全部内容
                if (record.getStatus().getName().equals("完成") || record.getStatus().getName().equals("失败")) {
                    readAllContent(wsSession, logFile);
                    // 读取完成后关闭连接
                } else {
                    // 如果部署进行中，启动实时跟踪（类似 tail -f）
                    startTailFile(wsSession, logFile, deploymentId);
                    shouldCloseConnection = false; // 实时跟踪模式下不关闭连接，由 FileTailTask 管理
                }

            } catch (Exception e) {
                log.error("Error tailing deployment log", e);
                try {
                    if (wsSession.isOpen()) {
                        wsSession.sendMessage(new TextMessage("读取日志失败: " + e.getMessage()));
                    }
                } catch (Exception ignored) {}
            } finally {
                // 只有在非实时跟踪模式下才关闭连接
                if (shouldCloseConnection) {
                    try {
                        if (wsSession.isOpen()) {
                            wsSession.close();
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    /**
     * 读取全部日志内容（用于已完成的部署）
     */
    private void readAllContent(WebSocketSession wsSession, File logFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && wsSession.isOpen()) {
                wsSession.sendMessage(new TextMessage(line));
            }
            wsSession.sendMessage(new TextMessage("\n>>> 日志读取完成"));
        } catch (Exception e) {
            log.error("Error reading log file", e);
            try {
                wsSession.sendMessage(new TextMessage("读取日志文件失败: " + e.getMessage()));
            } catch (Exception ignored) {}
        }
    }

    /**
     * 实时跟踪日志文件（类似 tail -f，用于进行中的部署）
     */
    private void startTailFile(WebSocketSession wsSession, File logFile, Integer deploymentId) {
        FileTailTask task = new FileTailTask(wsSession, logFile);
        taskMap.put(wsSession.getId(), task);
        executorService.submit(task);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket closed for session: {}", session.getId());
        stopTailFile(session.getId());
    }

    /**
     * 停止文件跟踪任务
     */
    private void stopTailFile(String sessionId) {
        FileTailTask task = taskMap.remove(sessionId);
        if (task != null) {
            task.stop();
        }
    }

    /**
     * 文件跟踪任务（类似 tail -f）
     */
    private static class FileTailTask implements Runnable {
        private final WebSocketSession wsSession;
        private final File logFile;
        private volatile boolean running = true;
        private long lastPosition = 0;

        public FileTailTask(WebSocketSession wsSession, File logFile) {
            this.wsSession = wsSession;
            this.logFile = logFile;
        }

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            try {
                // 先读取已有内容
                if (logFile.exists()) {
                    try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                        long fileSize = raf.length();
                        // 从当前位置开始读取
                        raf.seek(lastPosition);

                        String line;
                        while ((line = raf.readLine()) != null && running && wsSession.isOpen()) {
                            wsSession.sendMessage(new TextMessage(new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)));
                        }

                        lastPosition = raf.getFilePointer();
                    }
                }

                // 实时跟踪新内容
                while (running && wsSession.isOpen()) {
                    try {
                        Thread.sleep(500); // 每500毫秒检查一次

                        if (!logFile.exists()) {
                            continue;
                        }

                        long currentSize = logFile.length();
                        if (currentSize > lastPosition) {
                            try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                                raf.seek(lastPosition);

                                String line;
                                while ((line = raf.readLine()) != null && running && wsSession.isOpen()) {
                                    wsSession.sendMessage(new TextMessage(new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)));
                                }

                                lastPosition = raf.getFilePointer();
                            }
                        } else if (currentSize < lastPosition) {
                            // 文件被重新创建或截断，从头开始
                            lastPosition = 0;
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // 循环结束后，关闭连接
                if (wsSession.isOpen()) {
                    try {
                        wsSession.sendMessage(new TextMessage("\n>>> 日志跟踪结束"));
                        wsSession.close();
                    } catch (Exception e) {
                        log.error("Error closing WebSocket session", e);
                    }
                }
            } catch (Exception e) {
                log.error("Error in file tail task", e);
                try {
                    if (wsSession.isOpen()) {
                        wsSession.sendMessage(new TextMessage("日志跟踪出错: " + e.getMessage()));
                        wsSession.close();
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
