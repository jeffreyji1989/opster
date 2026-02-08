package com.opster.module.service.service.impl;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.opster.module.service.service.FileTransferService;
import com.opster.module.service.transfer.strategy.ScpTransferStrategy;
import com.opster.module.service.transfer.strategy.TransferStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件传输服务实现类
 * 整合传输策略、连接池、检查器等组件
 * 使用策略模式委托具体的传输逻辑给 TransferStrategy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTransferServiceImpl implements FileTransferService {

    private final ScpTransferStrategy scpTransferStrategy;

    /**
     * 默认重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 3;

    @Override
    public void uploadFile(Path localFile, Session sshSession, String remotePath,
                          WebSocketSession wsSession) throws Exception {
        log.info("开始文件传输: {} -> {}", localFile, remotePath);

        // 选择传输策略（目前仅支持 SCP）
        TransferStrategy strategy = selectStrategy(wsSession);
        strategy.transfer(localFile, sshSession, remotePath, wsSession);

        log.info("文件传输完成: {} -> {}", localFile, remotePath);
    }

    @Override
    public void uploadAndExtractZip(Path localZip, Session sshSession,
                                   String remoteDir, WebSocketSession wsSession) throws Exception {
        try {
            // 1. 上传 zip 文件
            String remoteZipPath = remoteDir + "/" + localZip.getFileName().toString();
            sendMessage(wsSession, ">>> 开始上传 zip 文件: " + localZip.getFileName());
            uploadFile(localZip, sshSession, remoteZipPath, wsSession);

            // 2. 解压 zip 文件
            sendMessage(wsSession, ">>> 开始解压 zip 文件...");
            String unzipCommand = String.format("cd %s && unzip -o %s && rm -f %s",
                remoteDir, remoteZipPath, remoteZipPath);
            executeRemoteCommand(sshSession, unzipCommand, wsSession);

            sendMessage(wsSession, ">>> zip 文件上传并解压完成");

        } catch (Exception e) {
            log.error("Failed to upload and extract zip file", e);
            sendMessage(wsSession, ">>> zip 文件上传或解压失败: " + e.getMessage());
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
                if (!Files.exists(localFile)) {
                    sendMessage(wsSession, ">>> 错误：本地文件不存在: " + localFile);
                    return false;
                }

                long fileSize = Files.size(localFile);
                sendMessage(wsSession, ">>> 开始上传文件: " + localFile.getFileName() +
                    " (" + formatFileSize(fileSize) + ")");

                uploadFile(localFile, sshSession, remotePath, wsSession);

                sendMessage(wsSession, ">>> 文件上传完成");
                return true;

            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.warn("Upload attempt {} failed for file: {}", attempt, localFile, e);

                if (attempt < maxRetries) {
                    sendMessage(wsSession, ">>> 上传失败，正在重试 (" + attempt + "/" + maxRetries + ")...");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

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
     * 选择传输策略
     * 目前仅支持 SCP，未来可扩展 SFTP、FTP 等
     *
     * @param wsSession WebSocket 会话
     * @return 传输策略
     */
    private TransferStrategy selectStrategy(WebSocketSession wsSession) {
        sendMessage(wsSession, ">>> 使用 SCP 传输策略");
        return scpTransferStrategy;
    }

    /**
     * 执行远程命令
     * 用于执行 unzip 等远程操作命令
     *
     * @param sshSession SSH 会话
     * @param command 要执行的命令
     * @param wsSession WebSocket 会话，用于推送输出
     * @throws Exception 执行失败时抛出异常
     */
    private void executeRemoteCommand(Session sshSession, String command, WebSocketSession wsSession)
            throws Exception {
        ChannelExec channel = null;
        java.io.InputStream in = null;

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
     * 将字节数转换为人类可读的格式
     *
     * @param size 文件大小（字节）
     * @return 格式化后的字符串
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
     *
     * @param session WebSocket 会话
     * @param message 要发送的消息
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
