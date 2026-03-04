package com.opster.module.service.service.impl;

import com.opster.module.service.service.GitOperationLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Git 操作锁服务实现类
 * 使用信号量实现，确保相同 Git URL 的操作串行执行
 */
@Slf4j
@Service
public class GitOperationLockServiceImpl implements GitOperationLockService {

    /**
     * Git URL 到信号量的映射
     * 每个 Git URL 对应一个信号量，初始许可为 1（表示同一时间只能有一个线程操作）
     */
    private final ConcurrentHashMap<String, Semaphore> gitLocks = new ConcurrentHashMap<>();

    /**
     * 锁 ID 到 Git URL 的映射（用于释放锁时查找对应的信号量）
     */
    private final ConcurrentHashMap<String, String> lockIdToGitUrl = new ConcurrentHashMap<>();

    /**
     * Git URL 到等待线程数的映射
     */
    private final ConcurrentHashMap<String, Integer> waitingCounts = new ConcurrentHashMap<>();

    /**
     * 获取锁的超时时间（分钟）
     */
    private static final int LOCK_TIMEOUT_MINUTES = 30;

    @Override
    public String acquireLock(String gitUrl) {
        return acquireLock(gitUrl, null);
    }

    /**
     * 获取 Git 操作锁（带日志记录器）
     *
     * @param gitUrl Git 仓库地址
     * @param logger 日志记录器（可选）
     * @return 锁 ID，用于释放锁
     */
    public String acquireLock(String gitUrl, com.opster.common.LocalBuildLogger logger) {
        if (gitUrl == null || gitUrl.isEmpty()) {
            return null;
        }

        // 获取或创建该 Git URL 对应的信号量
        Semaphore semaphore = gitLocks.computeIfAbsent(gitUrl, k -> new Semaphore(1));

        // 检查是否需要等待
        boolean needWait = semaphore.availablePermits() == 0;
        if (needWait) {
            int waitingCount = getWaitingCount(gitUrl);
            String message = String.format("Git 仓库 [%s] 正在被其他服务使用，正在排队等待（前面还有 %d 个任务）...",
                maskGitUrl(gitUrl), waitingCount);
            log.info(message);
            if (logger != null) {
                logger.info(">>> " + message);
            }
        }

        // 增加等待计数
        waitingCounts.merge(gitUrl, 1, Integer::sum);

        String lockId = UUID.randomUUID().toString();

        try {
            String message = needWait ?
                String.format("等待获取 Git 操作锁: %s", maskGitUrl(gitUrl)) :
                String.format("尝试获取 Git 操作锁: %s", maskGitUrl(gitUrl));
            log.info("{}, lockId={}", message, lockId);
            if (logger != null) {
                logger.info(">>> " + message);
            }

            // 尝试获取许可，最多等待 30 分钟
            boolean acquired = semaphore.tryAcquire(LOCK_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            if (acquired) {
                // 记录锁 ID 与 Git URL 的映射
                lockIdToGitUrl.put(lockId, gitUrl);
                String successMsg = String.format("成功获取 Git 操作锁: %s", maskGitUrl(gitUrl));
                log.info("{}, lockId={}", successMsg, lockId);
                if (logger != null) {
                    logger.info(">>> " + successMsg);
                }
                return lockId;
            } else {
                String timeoutMsg = String.format("获取 Git 操作锁超时（等待了 %d 分钟）: %s",
                    LOCK_TIMEOUT_MINUTES, maskGitUrl(gitUrl));
                log.warn(timeoutMsg);
                if (logger != null) {
                    logger.error(">>> " + timeoutMsg);
                }
                return null;
            }
        } catch (InterruptedException e) {
            String errorMsg = String.format("获取 Git 操作锁被中断: %s", maskGitUrl(gitUrl));
            log.error(errorMsg, e);
            if (logger != null) {
                logger.error(">>> " + errorMsg);
            }
            Thread.currentThread().interrupt();
            return null;
        } finally {
            // 减少等待计数
            waitingCounts.merge(gitUrl, -1, (old, delta) -> old + delta > 0 ? old + delta : 0);
        }
    }

    @Override
    public void releaseLock(String lockId) {
        releaseLock(lockId, null);
    }

    /**
     * 释放 Git 操作锁（带日志记录器）
     *
     * @param lockId 锁 ID
     * @param logger 日志记录器（可选）
     */
    public void releaseLock(String lockId, com.opster.common.LocalBuildLogger logger) {
        if (lockId == null) {
            return;
        }

        // 根据 lockId 查找对应的 Git URL
        String gitUrl = lockIdToGitUrl.remove(lockId);
        if (gitUrl == null) {
            log.warn("未找到锁 ID 对应的 Git URL: lockId={}", lockId);
            return;
        }

        // 获取对应的信号量并释放许可
        Semaphore semaphore = gitLocks.get(gitUrl);
        if (semaphore != null) {
            semaphore.release();
            String message = String.format("释放 Git 操作锁: %s", maskGitUrl(gitUrl));
            log.info("{}, lockId={}", message, lockId);
            if (logger != null) {
                logger.info(">>> " + message);
            }
        }

        // 清理：如果没有等待的线程，移除信号量（减少内存占用）
        Integer waitingCount = waitingCounts.get(gitUrl);
        if (waitingCount == null || waitingCount == 0) {
            // 检查信号量是否还有被占用的许可
            if (semaphore != null && semaphore.availablePermits() > 0) {
                // 可以安全移除
                gitLocks.remove(gitUrl, semaphore);
                waitingCounts.remove(gitUrl);
                log.debug("清理 Git 锁: gitUrl={}", maskGitUrl(gitUrl));
            }
        }
    }

    @Override
    public boolean isLocked(String gitUrl) {
        if (gitUrl == null || gitUrl.isEmpty()) {
            return false;
        }

        Semaphore semaphore = gitLocks.get(gitUrl);
        if (semaphore == null) {
            return false;
        }

        // 如果可用许可为 0，表示已被锁定
        return semaphore.availablePermits() == 0;
    }

    @Override
    public int getWaitingCount(String gitUrl) {
        if (gitUrl == null || gitUrl.isEmpty()) {
            return 0;
        }

        Integer count = waitingCounts.get(gitUrl);
        return count != null ? count : 0;
    }

    /**
     * 遮蔽 Git URL 中的敏感信息（用于日志输出）
     */
    private String maskGitUrl(String gitUrl) {
        if (gitUrl == null) {
            return "null";
        }

        // 遮蔽 URL 中的用户名密码
        // 例如：https://user:pass@git.example.com/repo.git -> https://***:***@git.example.com/repo.git
        try {
            if (gitUrl.contains("@") && (gitUrl.startsWith("http://") || gitUrl.startsWith("https://"))) {
                int protocolEnd = gitUrl.indexOf("://") + 3;
                int atIndex = gitUrl.indexOf("@");
                if (atIndex > protocolEnd) {
                    String protocol = gitUrl.substring(0, protocolEnd);
                    String rest = gitUrl.substring(atIndex + 1);
                    return protocol + "***:***@" + rest;
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }

        return gitUrl;
    }
}