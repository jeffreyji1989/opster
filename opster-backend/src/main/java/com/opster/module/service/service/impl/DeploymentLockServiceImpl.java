package com.opster.module.service.service.impl;

import com.opster.module.service.entity.DeploymentLock;
import com.opster.module.service.repository.DeploymentLockRepository;
import com.opster.module.service.service.DeploymentLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 部署锁服务实现类
 * 基于数据库实现分布式锁，防止并发部署冲突
 */
@Slf4j
@Service
public class DeploymentLockServiceImpl implements DeploymentLockService {

    @Autowired
    private DeploymentLockRepository lockRepository;

    /**
     * 默认锁超时时间（分钟）
     */
    private static final int DEFAULT_LOCK_TIMEOUT_MINUTES = 30;

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public String tryLock(Integer serviceId) {
        // 1. 检查是否已有锁
        DeploymentLock existingLock = lockRepository.findByServiceId(serviceId);
        if (existingLock != null) {
            log.warn("Service {} is already locked with lockId: {}", serviceId, existingLock.getLockId());
            return null; // 锁已被占用
        }

        // 2. 创建新锁
        DeploymentLock lock = new DeploymentLock();
        lock.setServiceId(serviceId);
        lock.setLockId(UUID.randomUUID().toString());
        lock.setCreateTime(LocalDateTime.now());
        lock.setExpireTime(LocalDateTime.now().plusMinutes(DEFAULT_LOCK_TIMEOUT_MINUTES));

        try {
            lockRepository.save(lock);
            log.info("Successfully acquired lock for service {} with lockId: {}", serviceId, lock.getLockId());
            return lock.getLockId();
        } catch (Exception e) {
            log.error("Failed to acquire lock for service: {}", serviceId, e);
            // 重新抛出异常，让事务正确回滚
            throw new RuntimeException("获取部署锁失败", e);
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void unlock(String lockId) {
        if (lockId == null || lockId.trim().isEmpty()) {
            log.warn("LockId is null or empty, cannot unlock");
            return;
        }

        try {
            DeploymentLock lock = lockRepository.findByLockId(lockId);
            if (lock != null) {
                Integer serviceId = lock.getServiceId();
                lockRepository.deleteByLockId(lockId);
                log.info("Successfully released lock for service {} with lockId: {}", serviceId, lockId);
            } else {
                log.warn("Lock not found for lockId: {}", lockId);
            }
        } catch (Exception e) {
            log.error("Failed to release lock for lockId: {}", lockId, e);
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void unlockByServiceId(Integer serviceId) {
        try {
            DeploymentLock lock = lockRepository.findByServiceId(serviceId);
            if (lock != null) {
                lockRepository.delete(lock);
                log.info("Forcefully released lock for service {}", serviceId);
            } else {
                log.warn("No lock found for service: {}", serviceId);
            }
        } catch (Exception e) {
            log.error("Failed to forcefully release lock for service: {}", serviceId, e);
        }
    }

    @Override
    public boolean isLocked(Integer serviceId) {
        try {
            // 先清理过期锁（在独立事务中执行）
            try {
                cleanExpiredLocks();
            } catch (Exception e) {
                log.warn("Failed to clean expired locks, continuing with lock check: {}", e.getMessage());
            }

            DeploymentLock lock = lockRepository.findByServiceId(serviceId);
            return lock != null;
        } catch (Exception e) {
            log.error("Failed to check lock status for service: {}", serviceId, e);
            return false;
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public int cleanExpiredLocks() {
        try {
            int deletedCount = lockRepository.deleteExpiredLocks(LocalDateTime.now());
            if (deletedCount > 0) {
                log.info("Cleaned {} expired locks", deletedCount);
            }
            return deletedCount;
        } catch (Exception e) {
            log.error("Failed to clean expired locks", e);
            return 0;
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public boolean extendLock(String lockId, int additionalMinutes) {
        try {
            DeploymentLock lock = lockRepository.findByLockId(lockId);
            if (lock == null) {
                log.warn("Lock not found for lockId: {}", lockId);
                return false;
            }

            // 延长锁的有效期
            lock.setExpireTime(lock.getExpireTime().plusMinutes(additionalMinutes));
            lockRepository.save(lock);

            log.info("Extended lock {} by {} minutes", lockId, additionalMinutes);
            return true;

        } catch (Exception e) {
            log.error("Failed to extend lock for lockId: {}", lockId, e);
            return false;
        }
    }
}
