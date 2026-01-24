package com.opster.module.service.service;

import com.jcraft.jsch.Session;
import org.springframework.web.socket.WebSocketSession;

import java.nio.file.Path;

/**
 * 文件传输服务接口
 * 负责通过SCP上传文件到远程服务器
 */
public interface FileTransferService {

    /**
     * 通过SCP上传文件（带进度推送）
     *
     * @param localFile 本地文件路径
     * @param sshSession SSH会话
     * @param remotePath 远程路径
     * @param wsSession WebSocket会话，用于推送进度
     * @throws Exception 上传过程中发生的异常
     */
    void uploadFile(Path localFile, Session sshSession, String remotePath,
                   WebSocketSession wsSession) throws Exception;

    /**
     * 上传并解压zip文件（用于前端项目）
     *
     * @param localZip 本地zip文件路径
     * @param sshSession SSH会话
     * @param remoteDir 远程目录路径
     * @param wsSession WebSocket会话，用于推送进度
     * @throws Exception 上传和解压过程中发生的异常
     */
    void uploadAndExtractZip(Path localZip, Session sshSession,
                            String remoteDir, WebSocketSession wsSession) throws Exception;

    /**
     * 上传文件（带重试机制）
     *
     * @param localFile 本地文件路径
     * @param sshSession SSH会话
     * @param remotePath 远程路径
     * @param maxRetries 最大重试次数
     * @param wsSession WebSocket会话
     * @return 是否上传成功
     */
    boolean uploadFileWithRetry(Path localFile, Session sshSession, String remotePath,
                               int maxRetries, WebSocketSession wsSession);

    /**
     * 计算文件上传进度百分比
     *
     * @param uploadedSize 已上传字节数
     * @param totalSize 总字节数
     * @return 进度百分比（0-100）
     */
    int calculateProgress(long uploadedSize, long totalSize);
}
