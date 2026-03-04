package com.opster.module.service.service.impl;

import com.opster.module.service.service.DeploymentLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 部署锁服务实现类
 * 基于内存实现锁，避免 SQLite 数据库高并发写入时的锁冲突
 * 注意：此实现为单机锁，不支持分布式部署
 */
@Slf4j
@Service
public class DeploymentLockServiceImpl implements DeploymentLockService {

    /**
     * 锁信息内部类
     */
    private static class LockInfo {
        String lockId;
        Integer serviceId;
        LocalDateTime createTime;
        LocalDateTime expireTime;

        LockInfo(String lockId, Integer serviceId, LocalDateTime createTime, LocalDateTime expireTime) {
            this.lockId = lockId;
            this.serviceId = serviceId;
            this.createTime = createTime;
            this.expireTime = expireTime;
        }
    }

    /**
     * 服务ID到锁信息的映射
     */
    private final ConcurrentHashMap<Integer, LockInfo> serviceLocks = new ConcurrentHashMap<>();

    /**
     * 锁ID到服务ID的映射（用于快速查找）
     */
    private final ConcurrentHashMap<String, Integer> lockIdToServiceId = new ConcurrentHashMap<>();

    /**
     * 默认锁超时时间（分钟）
     */
    private static final int DEFAULT_LOCK_TIMEOUT_MINUTES = 30;

    @Override
    public String tryLock(Integer serviceId) {
        // 先清理过期锁
        cleanExpiredLocks();

        // 尝试获取锁
        String lockId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusMinutes(DEFAULT_LOCK_TIMEOUT_MINUTES);
        LockInfo lockInfo = new LockInfo(lockId, serviceId, now, expireTime);

        // 使用 putIfAbsent 原子操作，确保线程安全
        LockInfo existingLock = serviceLocks.putIfAbsent(serviceId, lockInfo);

        if (existingLock != null) {
            // 锁已被占用
            log.warn("服务 {} 已被锁定，lockId: {}", serviceId, existingLock.lockId);
            return null;
        }

        // 记录 lockId 到 serviceId 的映射
        lockIdToServiceId.put(lockId, serviceId);
        log.info("成功获取服务 {} 的部署锁，lockId: {}", serviceId, lockId);
        return lockId;
    }

    @Override
    public void unlock(String lockId) {
        if (lockId == null || lockId.trim().isEmpty()) {
            log.warn("lockId 为空，无法释放锁");
            return;
        }

        // 根据 lockId 查找对应的 serviceId
        Integer serviceId = lockIdToServiceId.remove(lockId);
        if (serviceId == null) {
            log.warn("未找到 lockId 对应的服务: {}", lockId);
            return;
        }

        // 移除锁（只有当 lockId 匹配时才移除，防止误删其他线程的锁）
        LockInfo lockInfo = serviceLocks.get(serviceId);
        if (lockInfo != null && lockId.equals(lockInfo.lockId)) {
            serviceLocks.remove(serviceId);
            log.info("成功释放服务 {} 的部署锁，lockId: {}", serviceId, lockId);
        } else {
            log.warn("锁已过期或被其他线程持有，lockId: {}", lockId);
        }
    }

    @Override
    public void unlockByServiceId(Integer serviceId) {
        if (serviceId == null) {
            log.warn("serviceId 为空，无法释放锁");
            return;
        }

        LockInfo lockInfo = serviceLocks.remove(serviceId);
        if (lockInfo != null) {
            lockIdToServiceId.remove(lockInfo.lockId);
            log.info("强制释放服务 {} 的部署锁，lockId: {}", serviceId, lockInfo.lockId);
        } else {
            log.warn("服务 {} 未被锁定", serviceId);
        }
    }

    @Override
    public boolean isLocked(Integer serviceId) {
        if (serviceId == null) {
            return false;
        }

        // 先清理过期锁
        cleanExpiredLocks();

        return serviceLocks.containsKey(serviceId);
    }

    @Override
    public int cleanExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        int cleanedCount = 0;

        Iterator<Map.Entry<Integer, LockInfo>> iterator = serviceLocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, LockInfo> entry = iterator.next();
            LockInfo lockInfo = entry.getValue();

            if (lockInfo.expireTime.isBefore(now)) {
                // 锁已过期，移除
                iterator.remove();
                lockIdToServiceId.remove(lockInfo.lockId);
                cleanedCount++;
                log.info("清理过期锁: serviceId={}, lockId={}", lockInfo.serviceId, lockInfo.lockId);
            }
        }

        if (cleanedCount > 0) {
            log.info("清理了 {} 个过期的部署锁", cleanedCount);
        }

        return cleanedCount;
    }

    @Override
    public boolean extendLock(String lockId, int additionalMinutes) {
        if (lockId == null || lockId.trim().isEmpty()) {
            log.warn("lockId 为空，无法延长锁");
            return false;
        }

        Integer serviceId = lockIdToServiceId.get(lockId);
        if (serviceId == null) {
            log.warn("未找到 lockId 对应的服务: {}", lockId);
            return false;
        }

        LockInfo lockInfo = serviceLocks.get(serviceId);
        if (lockInfo == null || !lockId.equals(lockInfo.lockId)) {
            log.warn("锁不存在或已被替换，lockId: {}", lockId);
            return false;
        }

        // 延长锁的有效期
        LockInfo newLockInfo = new LockInfo(
            lockId,
            serviceId,
            lockInfo.createTime,
            lockInfo.expireTime.plusMinutes(additionalMinutes)
        );
        serviceLocks.put(serviceId, newLockInfo);

        log.info("延长锁 {} {} 分钟，新过期时间: {}", lockId, additionalMinutes, newLockInfo.expireTime);
        return true;
    }

    /**
     * 获取当前所有锁的数量（用于监控）
     */
    public int getLockCount() {
        return serviceLocks.size();
    }

    /**
     * 获取当前所有被锁定的服务ID（用于监控）
     */
    public java.util.Set<Integer> getLockedServiceIds() {
        return new java.util.HashSet<>(serviceLocks.keySet());
    }
}
