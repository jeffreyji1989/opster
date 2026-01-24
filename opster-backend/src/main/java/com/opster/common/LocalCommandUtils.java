package com.opster.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地命令执行工具类
 * 用于在本地系统执行命令并实时推送输出到WebSocket
 */
@Slf4j
public class LocalCommandUtils {

    /**
     * 执行本地命令并实时推送输出到WebSocket
     *
     * @param workDir 工作目录
     * @param command 命令（如：git pull, mvn clean package）
     * @param wsSession WebSocket会话
     * @return 执行是否成功
     */
    public static boolean executeCommand(Path workDir, String command, WebSocketSession wsSession) {
        return executeCommand(workDir, command, wsSession, null);
    }

    /**
     * 执行本地命令并实时推送输出到WebSocket（支持环境变量）
     *
     * @param workDir 工作目录
     * @param command 命令
     * @param wsSession WebSocket会话
     * @param envVars 环境变量数组（格式：["KEY=value", ...]
     * @return 执行是否成功
     */
    public static boolean executeCommand(Path workDir, String command, WebSocketSession wsSession, String[] envVars) {
        try {
            log.info("Executing command: {} in directory: {}", command, workDir);

            // 构建进程
            ProcessBuilder processBuilder = new ProcessBuilder();
            // 在Unix/Linux/Mac上使用bash，在Windows上使用cmd
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            if (isWindows) {
                processBuilder.command("cmd", "/c", command);
            } else {
                processBuilder.command("bash", "-c", command);
            }

            // 设置工作目录
            processBuilder.directory(workDir.toFile());

            // 设置环境变量
            if (envVars != null && envVars.length > 0) {
                for (String envVar : envVars) {
                    String[] parts = envVar.split("=", 2);
                    if (parts.length == 2) {
                        processBuilder.environment().put(parts[0], parts[1]);
                    }
                }
            }

            // 合并错误流和标准输出流
            processBuilder.redirectErrorStream(true);

            // 启动进程
            Process process = processBuilder.start();

            // 实时读取输出
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String line;
            boolean isSuccess = true;
            List<String> outputLines = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
                sendMessage(wsSession, line);

                // 分析日志，识别成功/失败标识
                isSuccess = analyzeLogLine(line, isSuccess);
            }

            // 等待进程结束
            int exitCode = process.waitFor();
            log.info("Command exit code: {}", exitCode);

            // 如果退出码不为0，标记为失败
            if (exitCode != 0) {
                isSuccess = false;
                sendMessage(wsSession, ">>> Command failed with exit code: " + exitCode);
            }

            return isSuccess;

        } catch (Exception e) {
            log.error("Error executing command: {}", command, e);
            sendMessage(wsSession, ">>> Error executing command: " + e.getMessage());
            return false;
        }
    }

    /**
     * 执行命令并返回输出（不推送到WebSocket）
     *
     * @param workDir 工作目录
     * @param command 命令
     * @return 命令输出
     */
    public static CommandResult executeCommandQuietly(Path workDir, String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            if (isWindows) {
                processBuilder.command("cmd", "/c", command);
            } else {
                processBuilder.command("bash", "-c", command);
            }

            processBuilder.directory(workDir.toFile());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            return new CommandResult(exitCode == 0, output.toString(), exitCode);

        } catch (Exception e) {
            log.error("Error executing command quietly: {}", command, e);
            return new CommandResult(false, e.getMessage(), -1);
        }
    }

    /**
     * 检查目录是否存在
     *
     * @param dirPath 目录路径
     * @return 是否存在
     */
    public static boolean directoryExists(Path dirPath) {
        return dirPath != null && dirPath.toFile().exists() && dirPath.toFile().isDirectory();
    }

    /**
     * 创建目录（包括不存在的父目录）
     *
     * @param dirPath 目录路径
     * @return 是否创建成功
     */
    public static boolean createDirectories(Path dirPath) {
        try {
            if (!directoryExists(dirPath)) {
                java.nio.file.Files.createDirectories(dirPath);
                log.info("Created directory: {}", dirPath);
            }
            return true;
        } catch (Exception e) {
            log.error("Error creating directory: {}", dirPath, e);
            return false;
        }
    }

    /**
     * 分析日志行，识别成功/失败标识
     *
     * @param line 日志行
     * @param currentStatus 当前状态
     * @return 分析后的状态
     */
    private static boolean analyzeLogLine(String line, boolean currentStatus) {
        String lowerLine = line.toLowerCase();

        // 识别maven打包失败
        if (lowerLine.contains("build failure") ||
            lowerLine.contains("compilation failure") ||
            lowerLine.contains("error")) {
            return false;
        }

        // 识别npm打包失败
        if (lowerLine.contains("npm err!") ||
            lowerLine.contains("elface")) {
            return false;
        }

        // 识别maven打包成功
        if (lowerLine.contains("build success") ||
            lowerLine.contains("building success")) {
            return true;
        }

        // 识别npm打包成功
        if (lowerLine.contains("built in") ||
            lowerLine.contains("webpack compiled successfully")) {
            return true;
        }

        return currentStatus;
    }

    /**
     * 发送消息到WebSocket
     *
     * @param session WebSocket会话
     * @param message 消息内容
     */
    private static void sendMessage(WebSocketSession session, String message) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.error("Error sending message to WebSocket", e);
        }
    }

    /**
     * 命令执行结果类
     */
    public static class CommandResult {
        private final boolean success;
        private final String output;
        private final int exitCode;

        public CommandResult(boolean success, String output, int exitCode) {
            this.success = success;
            this.output = output;
            this.exitCode = exitCode;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutput() {
            return output;
        }

        public int getExitCode() {
            return exitCode;
        }
    }
}
