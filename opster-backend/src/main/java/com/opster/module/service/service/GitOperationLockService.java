package com.opster.module.service.service;

/**
 * Git 操作锁服务接口
 * 用于防止多个服务同时操作同一个 Git 仓库
 * 当多个服务共享相同的 Git URL 时，确保 Git 操作（pull/clone）串行执行，避免锁文件冲突
 */
public interface GitOperationLockService {

    /**
     * 尝试获取 Git 操作锁
     * 如果 Git URL 已被其他线程锁定，则阻塞等待
     *
     * @param gitUrl Git 仓库地址
     * @return 锁 ID，用于释放锁
     */
    String acquireLock(String gitUrl);

    /**
     * 尝试获取 Git 操作锁（带日志记录器）
     * 如果 Git URL 已被其他线程锁定，则阻塞等待并记录排队信息
     *
     * @param gitUrl Git 仓库地址
     * @param logger 日志记录器（可选，用于记录排队状态）
     * @return 锁 ID，用于释放锁
     */
    String acquireLock(String gitUrl, com.opster.common.LocalBuildLogger logger);

    /**
     * 释放 Git 操作锁
     *
     * @param lockId 锁 ID
     */
    void releaseLock(String lockId);

    /**
     * 释放 Git 操作锁（带日志记录器）
     *
     * @param lockId 锁 ID
     * @param logger 日志记录器（可选）
     */
    void releaseLock(String lockId, com.opster.common.LocalBuildLogger logger);

    /**
     * 检查 Git URL 是否被锁定
     *
     * @param gitUrl Git 仓库地址
     * @return 是否被锁定
     */
    boolean isLocked(String gitUrl);

    /**
     * 获取当前等待指定 Git URL 的线程数
     *
     * @param gitUrl Git 仓库地址
     * @return 等待的线程数
     */
    int getWaitingCount(String gitUrl);
}