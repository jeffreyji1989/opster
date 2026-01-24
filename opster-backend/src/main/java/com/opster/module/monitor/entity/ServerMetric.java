package com.opster.module.monitor.entity;

import com.opster.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 服务器监控指标实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "server_metric")
public class ServerMetric extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 服务器ID
     */
    @Column(name = "server_id", nullable = false)
    private Integer serverId;

    /**
     * CPU使用率 (百分比, 0-100)
     */
    @Column(name = "cpu_usage")
    private Double cpuUsage;

    /**
     * 内存总量 (MB)
     */
    @Column(name = "memory_total")
    private Long memoryTotal;

    /**
     * 内存已用 (MB)
     */
    @Column(name = "memory_used")
    private Long memoryUsed;

    /**
     * 内存使用率 (百分比, 0-100)
     */
    @Column(name = "memory_usage")
    private Double memoryUsage;

    /**
     * 磁盘使用详情 (JSON格式)
     */
    @Column(name = "disk_usage_json", columnDefinition = "TEXT")
    private String diskUsageJson;

    /**
     * 记录时间 (默认创建时间，但为了索引优化可单独存)
     */
    @Column(name = "record_time")
    private LocalDateTime recordTime;
}