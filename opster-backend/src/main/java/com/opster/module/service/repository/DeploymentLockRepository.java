package com.opster.module.service.repository;

import com.opster.module.service.entity.DeploymentLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部署锁Repository
 */
@Repository
public interface DeploymentLockRepository extends JpaRepository<DeploymentLock, Integer> {

    /**
     * 根据服务ID查找锁
     *
     * @param serviceId 服务ID
     * @return 部署锁实体
     */
    DeploymentLock findByServiceId(Integer serviceId);

    /**
     * 根据锁ID查找锁
     *
     * @param lockId 锁ID
     * @return 部署锁实体
     */
    DeploymentLock findByLockId(String lockId);

    /**
     * 查找所有已过期的锁
     *
     * @param currentTime 当前时间
     * @return 已过期的锁列表
     */
    @Query("SELECT dl FROM DeploymentLock dl WHERE dl.expireTime < :currentTime")
    List<DeploymentLock> findExpiredLocks(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 删除所有已过期的锁
     *
     * @param currentTime 当前时间
     * @return 删除的锁数量
     */
    @Modifying
    @Query("DELETE FROM DeploymentLock dl WHERE dl.expireTime < :currentTime")
    int deleteExpiredLocks(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 删除指定锁ID的锁
     *
     * @param lockId 锁ID
     */
    void deleteByLockId(String lockId);
}
