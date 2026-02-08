package com.opster.module.service.service;

import java.util.concurrent.CompletableFuture;
import org.springframework.web.socket.WebSocketSession;

/**
 * 部署编排服务接口
 * 负责协调整个部署流程，包括本地打包、文件传输、远程部署、健康检查等
 */
public interface DeploymentOrchestrationService {

    /**
     * 执行完整部署流程（本地打包 + 远程部署）- WebSocket 模式
     *
     * 流程：
     * 1. 获取部署锁
     * 2. 本地打包（Maven/npm）
     * 3. 连接远程服务器
     * 4. 上传打包产物
     * 5. 备份当前版本
     * 6. 部署新版本
     * 7. 重启服务
     * 8. 健康检查
     * 9. 释放部署锁
     *
     * @param serviceId 服务ID
     * @param wsSession WebSocket会话
     */
    void executeDeployment(Integer serviceId, WebSocketSession wsSession);

    /**
     * 异步执行完整部署流程（本地打包 + 远程部署）- 后台模式
     * 日志保存到本地文件，不通过 WebSocket 推送
     *
     * @param serviceId 服务ID
     * @return 部署记录ID的Future
     */
    CompletableFuture<Integer> executeDeploymentAsync(Integer serviceId);

    /**
     * 执行重启（不打包，仅重启远程服务）- WebSocket 模式
     *
     * 流程：
     * 1. 连接远程服务器
     * 2. 执行重启脚本
     * 3. 健康检查
     *
     * @param serviceId 服务ID
     * @param wsSession WebSocket会话
     */
    void executeRestart(Integer serviceId, WebSocketSession wsSession);

    /**
     * 执行回滚
     *
     * 流程：
     * 1. 获取部署锁
     * 2. 连接远程服务器
     * 3. 从备份目录恢复上一版本
     * 4. 重启服务
     * 5. 健康检查
     * 6. 释放部署锁
     *
     * @param serviceId 服务ID
     * @param wsSession WebSocket会话
     */
    void executeRollback(Integer serviceId, WebSocketSession wsSession);

    /**
     * 执行启动（不打包，仅启动远程服务）
     *
     * @param serviceId 服务ID
     * @param wsSession WebSocket会话
     */
    void executeStart(Integer serviceId, WebSocketSession wsSession);

    /**
     * 停止服务
     *
     * @param serviceId 服务ID
     * @param wsSession WebSocket会话
     */
    void executeStop(Integer serviceId, WebSocketSession wsSession);

    /**
     * 回退到指定版本 - WebSocket 模式
     *
     * 流程：
     * 1. 获取部署锁
     * 2. 根据recordId获取目标版本记录
     * 3. 连接远程服务器
     * 4. 回退前先备份当前版本（防止回退失败）
     * 5. 恢复目标版本的jar文件
     * 6. 重启服务
     * 7. 健康检查
     * 8. 创建回退记录（标记 is_rollback = true）
     * 9. 释放部署锁
     *
     * @param recordId 部署记录ID
     * @param wsSession WebSocket会话
     */
    void rollbackToSpecificVersion(Integer recordId, WebSocketSession wsSession);

    /**
     * 异步回退到指定版本 - 后台模式
     * 日志保存到本地文件，不通过 WebSocket 推送
     *
     * @param recordId 部署记录ID
     * @return 新的回退记录ID的Future
     */
    CompletableFuture<Integer> rollbackToSpecificVersionAsync(Integer recordId);
}
