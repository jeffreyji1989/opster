package com.opster.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 本地部署日志记录器
 * 同时将日志输出到本地文件和 WebSocket（可选）
 */
@Slf4j
public class LocalDeploymentLogger implements AutoCloseable {

    private final Path logFile;
    private final WebSocketSession wsSession;
    private PrintWriter writer;
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 构造函数（带 WebSocket）
     * @param projectCode 项目编码
     * @param deployPath 本地部署根目录
     * @param wsSession WebSocket会话
     */
    public LocalDeploymentLogger(String projectCode, String deployPath, WebSocketSession wsSession) {
        this.wsSession = wsSession;
        this.logFile = createLogFile(projectCode, deployPath);
        this.writer = createWriter();
    }

    /**
     * 构造函数（不带 WebSocket - 后台异步模式）
     * @param projectCode 项目编码
     * @param deployPath 本地部署根目录
     */
    public LocalDeploymentLogger(String projectCode, String deployPath) {
        this.wsSession = null;
        this.logFile = createLogFile(projectCode, deployPath);
        this.writer = createWriter();
    }

    /**
     * 创建日志文件
     */
    private Path createLogFile(String projectCode, String deployPath) {
        try {
            // 创建日志目录：{deployPath}/{projectCode}/p_log/
            Path logDir = Paths.get(deployPath, projectCode, "p_log");
            Files.createDirectories(logDir);

            // 生成日志文件名：yyyyMMddHHmmss.log
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String fileName = timestamp + ".log";

            Path logFile = logDir.resolve(fileName);
            log.info("创建部署日志文件: {}", logFile);

            return logFile;
        } catch (Exception e) {
            log.error("创建日志文件失败", e);
            throw new RuntimeException("创建日志文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建日志文件写入器
     */
    private PrintWriter createWriter() {
        try {
            return new PrintWriter(Files.newBufferedWriter(logFile, StandardCharsets.UTF_8), true);
        } catch (Exception e) {
            log.error("创建日志写入器失败", e);
            throw new RuntimeException("创建日志写入器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 记录日志（同时写入文件和 WebSocket）
     * @param message 日志消息
     */
    public void log(String message) {
        String timestamp = LocalDateTime.now().format(timestampFormatter);
        String logLine = String.format("[%s] %s", timestamp, message);

        // 写入文件
        if (writer != null) {
            writer.println(logLine);
        }

        // 推送到 WebSocket（不带时间戳，保持原有显示格式）
        sendMessage(wsSession, message);
    }

    /**
     * 发送消息到 WebSocket（如果可用）
     */
    private void sendMessage(WebSocketSession session, String message) {
        if (session == null) {
            return; // 无 WebSocket 模式，跳过
        }
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.error("发送消息到 WebSocket 失败", e);
        }
    }

    /**
     * 获取日志文件路径
     */
    public Path getLogFilePath() {
        return logFile;
    }

    /**
     * 刷新日志写入器
     */
    public void flush() {
        if (writer != null) {
            writer.flush();
        }
    }

    /**
     * 关闭日志记录器
     */
    @Override
    public void close() {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
}
