package com.opster.module.service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部署锁实体类
 * 用于防止同一服务被多人同时部署
 */
@Data
@Entity
@Table(name = "deployment_lock", uniqueConstraints = {
    @UniqueConstraint(columnNames = "service_id")
})
public class DeploymentLock {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 服务ID
     */
    @Column(name = "service_id", nullable = false)
    private Integer serviceId;

    /**
     * 锁ID（UUID）
     */
    @Column(name = "lock_id", nullable = false, length = 50)
    private String lockId;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 过期时间
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;
}
