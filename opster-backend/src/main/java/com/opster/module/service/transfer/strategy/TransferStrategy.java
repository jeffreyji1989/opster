package com.opster.module.service.transfer.strategy;

import com.jcraft.jsch.Session;
import org.springframework.web.socket.WebSocketSession;

import java.nio.file.Path;

/**
 * 文件传输策略接口
 * 定义不同的文件传输实现（SCP、Rsync等）
 */
public interface TransferStrategy {

    /**
     * 传输文件到远程服务器
     *
     * @param localFile 本地文件路径
     * @param sshSession SSH 会话
     * @param remotePath 远程路径
     * @param wsSession WebSocket 会话（用于进度推送）
     * @throws Exception 传输失败时抛出异常
     */
    void transfer(Path localFile, Session sshSession, String remotePath,
                 WebSocketSession wsSession) throws Exception;

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getName();

    /**
     * 检查策略是否可用
     *
     * @param sshSession SSH 会话
     * @return true 如果可用，否则 false
     */
    default boolean isAvailable(Session sshSession) {
        return true;
    }
}
