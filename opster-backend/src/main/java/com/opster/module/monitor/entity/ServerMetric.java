package com.opster.module.monitor.entity;

import com.opster.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 服务器监控指标实体
 */
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Long getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(Long memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public Long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(Long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public String getDiskUsageJson() {
        return diskUsageJson;
    }

    public void setDiskUsageJson(String diskUsageJson) {
        this.diskUsageJson = diskUsageJson;
    }

    public LocalDateTime getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(LocalDateTime recordTime) {
        this.recordTime = recordTime;
    }
}
