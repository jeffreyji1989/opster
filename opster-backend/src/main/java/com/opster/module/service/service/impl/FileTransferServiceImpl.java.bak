package com.opster.module.service.service.impl;

import com.jcraft.jsch.*;
import com.opster.module.service.service.FileTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件传输服务实现类
 * 通过SCP协议上传文件到远程服务器
 */
@Slf4j
@Service
public class FileTransferServiceImpl implements FileTransferService {

    /**
     * 默认重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 上传缓冲区大小（1MB）
     */
    private static final int BUFFER_SIZE = 1024 * 1024;

    /**
     * 进度推送间隔（每10%推送一次）
     */
    private static final int PROGRESS_INTERVAL = 10;

    @Override
    public void uploadFile(Path localFile, Session sshSession, String remotePath,
                          WebSocketSession wsSession) throws Exception {
        uploadFileWithRetry(localFile, sshSession, remotePath, DEFAULT_MAX_RETRIES, wsSession);
    }

    @Override
    public void uploadAndExtractZip(Path localZip, Session sshSession,
                                   String remoteDir, WebSocketSession wsSession) throws Exception {
        try {
            // 1. 上传zip文件
            String remoteZipPath = remoteDir + "/" + localZip.getFileName().toString();
            sendMessage(wsSession, ">>> 开始上传zip文件: " + localZip.getFileName());
            uploadFile(localZip, sshSession, remoteZipPath, wsSession);

            // 2. 解压zip文件
            sendMessage(wsSession, ">>> 开始解压zip文件...");
            String unzipCommand = String.format("cd %s && unzip -o %s && rm -f %s",
                remoteDir, remoteZipPath, remoteZipPath);
            executeRemoteCommand(sshSession, unzipCommand, wsSession);

            sendMessage(wsSession, ">>> zip文件上传并解压完成");

        } catch (Exception e) {
            log.error("Failed to upload and extract zip file", e);
            sendMessage(wsSession, ">>> zip文件上传或解压失败: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean uploadFileWithRetry(Path localFile, Session sshSession, String remotePath,
                                      int maxRetries, WebSocketSession wsSession) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                // 检查本地文件是否存在
                if (!Files.exists(localFile)) {
                    sendMessage(wsSession, ">>> 错误：本地文件不存在: " + localFile);
                    return false;
                }

                long fileSize = Files.size(localFile);
                sendMessage(wsSession, ">>> 开始上传文件: " + localFile.getFileName() + " (" + formatFileSize(fileSize) + ")");

                // 执行SCP上传
                performScpUpload(localFile, sshSession, remotePath, wsSession);

                sendMessage(wsSession, ">>> 文件上传完成");
                return true;

            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.warn("Upload attempt {} failed for file: {}", attempt, localFile, e);

                if (attempt < maxRetries) {
                    sendMessage(wsSession, ">>> 上传失败，正在重试 (" + attempt + "/" + maxRetries + ")...");
                    try {
                        Thread.sleep(2000); // 等待2秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 所有重试都失败
        sendMessage(wsSession, ">>> 上传失败，已达到最大重试次数: " + maxRetries);
        if (lastException != null) {
            sendMessage(wsSession, ">>> 错误信息: " + lastException.getMessage());
        }
        return false;
    }

    @Override
    public int calculateProgress(long uploadedSize, long totalSize) {
        if (totalSize == 0) {
            return 100;
        }
        return (int) ((uploadedSize * 100) / totalSize);
    }

    /**
     * 执行SCP上传
     */
    private void performScpUpload(Path localFile, Session sshSession, String remotePath,
                                 WebSocketSession wsSession) throws Exception {
        String command = "scp -t " + remotePath;
        ChannelExec channel = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            // 打开SCP通道
            channel = (ChannelExec) sshSession.openChannel("exec");
            channel.setCommand(command);

            // 获取输入输出流
            out = channel.getOutputStream();
            in = channel.getInputStream();

            channel.connect();

            // 检查服务器响应
            checkAck(in);

            // 发送文件信息
            long fileSize = Files.size(localFile);
            String fileName = localFile.getFileName().toString();
            String fileInfo = "C0644 " + fileSize + " " + fileName + "\n";
            out.write(fileInfo.getBytes());
            out.flush();

            // 检查服务器响应
            checkAck(in);

            // 发送文件内容
            FileInputStream fis = new FileInputStream(localFile.toFile());
            byte[] buffer = new byte[BUFFER_SIZE];
            long totalUploaded = 0;
            int lastProgress = 0;

            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalUploaded += bytesRead;

                // 计算并推送进度
                int progress = calculateProgress(totalUploaded, fileSize);
                if (progress >= lastProgress + PROGRESS_INTERVAL || progress == 100) {
                    sendMessage(wsSession, ">>> 上传进度: " + progress + "%");
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
     * 检查SCP服务器的ACK响应
     */
    private void checkAck(InputStream in) throws Exception {
        int b = in.read();
        if (b == 0) {
            return; // 成功
        } else if (b == -1) {
            throw new Exception("SCP connection lost");
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
     * 执行远程命令
     */
    private void executeRemoteCommand(Session sshSession, String command, WebSocketSession wsSession)
            throws Exception {
        ChannelExec channel = null;
        InputStream in = null;

        try {
            channel = (ChannelExec) sshSession.openChannel("exec");
            channel.setCommand(command);

            in = channel.getInputStream();
            channel.connect();

            // 读取命令输出
            java.util.Scanner scanner = new java.util.Scanner(in);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                sendMessage(wsSession, line);
            }
            scanner.close();

            // 等待命令执行完成
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    // ignore
                }
            }

        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ignored) {}
            }
            if (channel != null) {
                channel.disconnect();
            }
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
     * 发送消息到WebSocket
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
