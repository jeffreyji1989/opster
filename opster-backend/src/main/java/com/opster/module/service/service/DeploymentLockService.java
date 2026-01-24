package com.opster.module.service.service;

/**
 * 部署锁服务接口
 * 用于防止同一服务被多人同时部署
 */
public interface DeploymentLockService {

    /**
     * 尝试获取锁
     * 如果服务已被锁定，返回null；否则创建锁并返回lockId
     *
     * @param serviceId 服务ID
     * @return 锁ID（UUID），如果获取失败则返回null
     */
    String tryLock(Integer serviceId);

    /**
     * 释放锁
     *
     * @param lockId 锁ID
     */
    void unlock(String lockId);

    /**
     * 强制释放服务的锁
     *
     * @param serviceId 服务ID
     */
    void unlockByServiceId(Integer serviceId);

    /**
     * 检查服务是否被锁定
     *
     * @param serviceId 服务ID
     * @return 是否被锁定
     */
    boolean isLocked(Integer serviceId);

    /**
     * 清理所有过期的锁
     *
     * @return 清理的锁数量
     */
    int cleanExpiredLocks();

    /**
     * 延长锁的有效期
     *
     * @param lockId 锁ID
     * @param additionalMinutes 延长的分钟数
     * @return 是否延长成功
     */
    boolean extendLock(String lockId, int additionalMinutes);
}
