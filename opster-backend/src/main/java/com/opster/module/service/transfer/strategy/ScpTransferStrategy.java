package com.opster.module.service.transfer.strategy;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.opster.config.OpsterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SCP 传输策略（优化版）
 * 支持增强的错误处理、进度推送、连接保活等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScpTransferStrategy implements TransferStrategy {

    private final OpsterProperties opsterProperties;

    @Override
    public void transfer(Path localFile, Session sshSession, String remotePath,
                        WebSocketSession wsSession) throws Exception {
        int maxRetries = opsterProperties.getTransfer().getMaxRetries();
        String[] retryDelays = opsterProperties.getTransfer().getRetryDelays().split(",");

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                log.info("SCP 传输尝试 {}/{}: 文件={}, 目标={}", attempt + 1, maxRetries, localFile, remotePath);
                sendMessage(wsSession, ">>> 开始 SCP 传输 (尝试 " + (attempt + 1) + "/" + maxRetries + ")");

                performScpUpload(localFile, sshSession, remotePath, wsSession);

                sendMessage(wsSession, ">>> SCP 传输完成");
                log.info("SCP 传输成功: {} -> {}", localFile, remotePath);
                return;

            } catch (Exception e) {
                log.warn("SCP 传输尝试 {} 失败: {}", attempt + 1, e.getMessage(), e);

                if (attempt < maxRetries - 1) {
                    // 计算重试延迟
                    long delay = attempt < retryDelays.length ?
                        Long.parseLong(retryDelays[attempt]) : 5000;

                    sendMessage(wsSession, ">>> 传输失败，" + delay + "ms 后重试...");
                    Thread.sleep(delay);

                    // 验证并重新连接
                    if (!sshSession.isConnected()) {
                        log.info("SSH 连接断开，尝试重新连接...");
                        sendMessage(wsSession, ">>> SSH 连接断开，正在重新连接...");
                        throw new Exception("SSH 连接断开，需要重建 Session", e);
                    }
                } else {
                    sendMessage(wsSession, ">>> SCP 传输失败，已达到最大重试次数");
                    throw new Exception("SCP 传输失败: " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "SCP";
    }

    /**
     * 执行 SCP 上传
     */
    private void performScpUpload(Path localFile, Session sshSession, String remotePath,
                                 WebSocketSession wsSession) throws Exception {
        String command = "scp -t " + remotePath;
        ChannelExec channel = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            // 检查本地文件
            if (!Files.exists(localFile)) {
                throw new Exception("本地文件不存在: " + localFile);
            }

            long fileSize = Files.size(localFile);
            int bufferSize = opsterProperties.getTransfer().getBufferSize();
            int progressInterval = opsterProperties.getTransfer().getProgressInterval();

            sendMessage(wsSession, ">>> 文件大小: " + formatFileSize(fileSize));
            log.info("开始 SCP 上传: {} ({} bytes) 到 {}", localFile.getFileName(), fileSize, remotePath);

            // 打开 SCP 通道
            channel = (ChannelExec) sshSession.openChannel("exec");
            channel.setCommand(command);

            // 获取输入输出流
            out = channel.getOutputStream();
            in = channel.getInputStream();

            channel.connect();

            // 检查服务器响应
            checkAck(in);

            // 发送文件信息
            String fileName = localFile.getFileName().toString();
            String fileInfo = "C0644 " + fileSize + " " + fileName + "\n";
            out.write(fileInfo.getBytes());
            out.flush();

            // 检查服务器响应
            checkAck(in);

            // 发送文件内容
            FileInputStream fis = new FileInputStream(localFile.toFile());
            byte[] buffer = new byte[bufferSize];
            long totalUploaded = 0;
            int lastProgress = 0;
            long startTime = System.currentTimeMillis();

            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush(); // 立即刷新，确保数据发送
                totalUploaded += bytesRead;

                // 计算并推送进度
                int progress = (int) ((totalUploaded * 100) / fileSize);
                if (progress >= lastProgress + progressInterval || progress == 100) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double speed = (totalUploaded / 1024.0 / 1024.0) / (elapsed / 1000.0); // MB/s
                    sendMessage(wsSession, ">>> 上传进度: " + progress + "% (" +
                        String.format("%.2f", speed) + " MB/s)");
                    lastProgress = progress;
                }
            }

            fis.close();
            out.flush();

            // 发送结束标志
            buffer[0] = 0;
            out.write(buffer, 0, 1);
            out.flush();

            // 检查服务器响应
            checkAck(in);

            long elapsed = System.currentTimeMillis() - startTime;
            double avgSpeed = (fileSize / 1024.0 / 1024.0) / (elapsed / 1000.0);
            sendMessage(wsSession, ">>> 上传完成，平均速度: " + String.format("%.2f", avgSpeed) + " MB/s");
            log.info("SCP 上传完成: {} -> {}, 耗时: {}ms, 平均速度: {:.2f} MB/s",
                localFile, remotePath, elapsed, avgSpeed);

        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception ignored) {}
            }
            if (in != null) {
                try { in.close(); } catch (Exception ignored) {}
            }
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /**
     * 检查 SCP 服务器的 ACK 响应
     */
    private void checkAck(InputStream in) throws Exception {
        int b = in.read();
        if (b == 0) {
            return; // 成功
        } else if (b == -1) {
            throw new Exception("SCP connection lost - 无法读取服务器响应");
        } else if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = in.read()) != '\n') {
                sb.append((char) c);
            }
            throw new Exception("SCP error: " + sb.toString());
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 发送消息到 WebSocket
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.error("Error sending message to WebSocket", e);
        }
    }
}
