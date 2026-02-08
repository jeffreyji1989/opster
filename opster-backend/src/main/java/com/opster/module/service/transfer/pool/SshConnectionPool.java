package com.opster.module.service.transfer.pool;

import com.jcraft.jsch.Session;
import com.opster.common.SshUtils;
import com.opster.config.OpsterProperties;
import com.opster.module.server.entity.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * SSH 连接池
 * 管理 SSH Session 的创建、复用和清理
 */
@Slf4j
@Component
public class SshConnectionPool {

    private final OpsterProperties opsterProperties;
    private final ConcurrentHashMap<ConnectionKey, LinkedBlockingQueue<Session>> pool = new ConcurrentHashMap<>();

    public SshConnectionPool(OpsterProperties opsterProperties) {
        this.opsterProperties = opsterProperties;
        log.info("SSH 连接池初始化 - 启用: {}, 每服务器最大连接数: {}",
            opsterProperties.getSshPool().getEnabled(),
            opsterProperties.getSshPool().getMaxConnectionsPerServer());
    }

    /**
     * 获取 SSH Session
     *
     * @param server 服务器信息
     * @return SSH Session
     * @throws Exception 获取失败时抛出异常
     */
    public Session borrowObject(Server server) throws Exception {
        if (!opsterProperties.getSshPool().getEnabled()) {
            // 连接池未启用，每次创建新连接
            return createNewSession(server);
        }

        ConnectionKey key = new ConnectionKey(server.getIp(), 22, server.getUsername());
        LinkedBlockingQueue<Session> sessions = pool.get(key);

        // 尝试从池中获取可用连接
        if (sessions != null) {
            Session session = sessions.poll();
            if (session != null && SshUtils.isConnected(session)) {
                log.debug("从连接池获取连接: {}", key);
                return session;
            }
        }

        // 没有可用连接，创建新连接
        log.info("创建新的 SSH 连接: {}", key);
        return createNewSession(server);
    }

    /**
     * 归还 SSH Session
     *
     * @param server 服务器信息
     * @param session SSH Session
     */
    public void returnObject(Server server, Session session) {
        if (!opsterProperties.getSshPool().getEnabled() || session == null) {
            // 连接池未启用，直接断开连接
            if (session != null) {
                session.disconnect();
            }
            return;
        }

        ConnectionKey key = new ConnectionKey(server.getIp(), 22, server.getUsername());
        int maxConnections = opsterProperties.getSshPool().getMaxConnectionsPerServer();

        pool.computeIfAbsent(key, k -> new LinkedBlockingQueue<>(maxConnections));

        LinkedBlockingQueue<Session> sessions = pool.get(key);

        // 检查连接是否仍然有效
        if (!SshUtils.isConnected(session)) {
            log.warn("连接已失效，不归还到池中: {}", key);
            session.disconnect();
            return;
        }

        // 尝试归还到池中
        if (sessions.size() < maxConnections) {
            if (sessions.offer(session)) {
                log.debug("归还连接到池: {}, 当前池大小: {}", key, sessions.size());
            } else {
                log.warn("连接池已满，关闭连接: {}", key);
                session.disconnect();
            }
        } else {
            log.debug("连接池已满，关闭连接: {}", key);
            session.disconnect();
        }
    }

    /**
     * 创建新的 SSH Session
     */
    private Session createNewSession(Server server) throws Exception {
        int connectionTimeout = opsterProperties.getSshPool().getConnectionTimeout();
        int sessionTimeout = opsterProperties.getSshPool().getSessionTimeout();

        return SshUtils.connect(
            server.getIp(),
            22,
            server.getUsername(),
            server.getPassword(),
            connectionTimeout,
            sessionTimeout
        );
    }

    /**
     * 定时清理失效连接（每5分钟执行一次）
     */
    @Scheduled(fixedRate = 300000)
    public void evictIdleConnections() {
        if (!opsterProperties.getSshPool().getEnabled()) {
            return;
        }

        log.debug("开始清理失效的 SSH 连接...");
        int removedCount = 0;

        for (ConnectionKey key : pool.keySet()) {
            LinkedBlockingQueue<Session> sessions = pool.get(key);
            if (sessions == null) continue;

            sessions.removeIf(session -> {
                if (!SshUtils.isConnected(session)) {
                    log.debug("移除失效连接: {}", key);
                    session.disconnect();
                    return true;
                }
                return false;
            });

            removedCount += sessions.size();
        }

        log.debug("连接池清理完成，当前连接数: {}", removedCount);
    }

    /**
     * 应用关闭时清理所有连接
     */
    @PreDestroy
    public void closeAll() {
        log.info("关闭所有 SSH 连接...");
        int totalCount = 0;

        for (ConnectionKey key : pool.keySet()) {
            LinkedBlockingQueue<Session> sessions = pool.get(key);
            if (sessions == null) continue;

            for (Session session : sessions) {
                try {
                    session.disconnect();
                    totalCount++;
                } catch (Exception e) {
                    log.warn("关闭连接失败: {}", key, e);
                }
            }
        }

        pool.clear();
        log.info("已关闭 {} 个 SSH 连接", totalCount);
    }

    /**
     * 获取连接池统计信息
     */
    public PoolStatistics getStatistics() {
        int totalConnections = 0;
        int activeConnections = 0;
        int idleConnections = 0;

        for (LinkedBlockingQueue<Session> sessions : pool.values()) {
            totalConnections += sessions.size();
            idleConnections += sessions.size();
        }

        return new PoolStatistics(
            pool.size(),
            totalConnections,
            activeConnections,
            idleConnections
        );
    }

    /**
     * 连接池统计信息
     */
    public record PoolStatistics(
        int poolSize,           // 连接池大小（服务器数量）
        int totalConnections,   // 总连接数
        int activeConnections,  // 活跃连接数
        int idleConnections     // 空闲连接数
    ) {}
}
