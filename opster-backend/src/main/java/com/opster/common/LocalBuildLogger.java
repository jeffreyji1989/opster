package com.opster.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 本地构建日志记录器
 * 同时写入本地日志文件和WebSocket推送
 */
@Slf4j
public class LocalBuildLogger {

    private final Path logFile;
    private final WebSocketSession wsSession;
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 构造函数
     *
     * @param logFile 日志文件路径
     * @param wsSession WebSocket会话
     */
    public LocalBuildLogger(Path logFile, WebSocketSession wsSession) {
        this.logFile = logFile;
        this.wsSession = wsSession;
    }

    /**
     * 记录普通日志
     *
     * @param message 日志消息
     */
    public void log(String message) {
        log(LogLevel.INFO, message);
    }

    /**
     * 记录指定级别的日志
     *
     * @param level 日志级别
     * @param message 日志消息
     */
    public void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(timestampFormatter);
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

        // 1. 写入本地日志文件
        writeToFile(logMessage);

        // 2. 推送到WebSocket
        sendToWebSocket(message);
    }

    /**
     * 记录INFO级别日志
     *
     * @param message 日志消息
     */
    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    /**
     * 记录ERROR级别日志
     *
     * @param message 日志消息
     */
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    /**
     * 记录WARN级别日志
     *
     * @param message 日志消息
     */
    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    /**
     * 记录带异常的ERROR日志
     *
     * @param message 日志消息
     * @param e 异常对象
     */
    public void error(String message, Exception e) {
        log(LogLevel.ERROR, message + " - " + e.getMessage());
        if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
            log(LogLevel.ERROR, "    at " + e.getStackTrace()[0].toString());
        }
    }

    /**
     * 写入日志文件
     *
     * @param message 日志消息
     */
    private void writeToFile(String message) {
        try {
            // 确保日志文件目录存在
            if (logFile != null) {
                Path parentDir = logFile.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }

                // 追加写入日志文件
                Files.writeString(
                    logFile,
                    message + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                );
            }
        } catch (IOException e) {
            log.error("Error writing to log file: {}", logFile, e);
        }
    }

    /**
     * 发送消息到WebSocket
     *
     * @param message 消息内容
     */
    private void sendToWebSocket(String message) {
        try {
            if (wsSession != null && wsSession.isOpen()) {
                wsSession.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.error("Error sending message to WebSocket", e);
        }
    }

    /**
     * 获取日志文件路径
     *
     * @return 日志文件路径
     */
    public Path getLogFile() {
        return logFile;
    }

    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        INFO("INFO"),
        WARN("WARN"),
        ERROR("ERROR");

        private final String level;

        LogLevel(String level) {
            this.level = level;
        }

        @Override
        public String toString() {
            return level;
        }
    }

    /**
     * 创建日志记录器的工厂方法
     *
     * @param logDir 日志目录
     * @param fileName 日志文件名
     * @param wsSession WebSocket会话
     * @return 日志记录器实例
     */
    public static LocalBuildLogger create(String logDir, String fileName, WebSocketSession wsSession) {
        try {
            // 确保日志目录存在
            Path dirPath = Paths.get(logDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 创建日志文件路径
            Path logFile = dirPath.resolve(fileName);

            return new LocalBuildLogger(logFile, wsSession);
        } catch (Exception e) {
            log.error("Error creating LocalBuildLogger", e);
            // 如果创建失败，返回一个没有文件记录的实例
            return new LocalBuildLogger(null, wsSession);
        }
    }
}
