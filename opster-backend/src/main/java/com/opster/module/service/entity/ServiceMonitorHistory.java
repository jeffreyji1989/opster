package com.opster.module.service.entity;

import com.opster.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 服务监控历史记录实体
 * 用于记录HTTP监控检查的结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "service_monitor_history")
public class ServiceMonitorHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 服务ID
     */
    @Column(name = "service_id", nullable = false)
    private Integer serviceId;

    /**
     * 项目ID
     */
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    /**
     * 服务器ID
     */
    @Column(name = "server_id", nullable = false)
    private Integer serverId;

    /**
     * 项目名称（冗余字段，便于查询）
     */
    @Column(name = "project_name")
    private String projectName;

    /**
     * 服务名称（冗余字段，便于查询）
     */
    @Column(name = "service_name")
    private String serviceName;

    /**
     * 服务器IP（冗余字段，便于查询）
     */
    @Column(name = "server_ip")
    private String serverIp;

    /**
     * 端口号（冗余字段，便于查询）
     */
    @Column(name = "port")
    private Integer port;

    /**
     * 监控URL（冗余字段，便于查询）
     */
    @Column(name = "monitor_url")
    private String monitorUrl;

    /**
     * 监控状态: 0-异常 1-正常
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    /**
     * 响应耗时（毫秒）
     */
    @Column(name = "response_time")
    private Integer responseTime;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 记录时间
     */
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;
}