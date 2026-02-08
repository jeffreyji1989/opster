package com.opster.module.schedule.entity;

import com.opster.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时发版任务服务关联实体
 * 用于支持一个定时发版任务包含多个服务
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "scheduled_deployment_services")
public class ScheduledDeploymentServiceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 任务ID（关联到 scheduled_deployment 表）
     */
    @Column(name = "task_id", nullable = false)
    private Integer taskId;

    /**
     * 服务ID
     */
    @Column(name = "service_id", nullable = false)
    private Integer serviceId;

    /**
     * 项目名称（冗余字段，便于查询）
     */
    @Column(name = "project_name")
    private String projectName;

    /**
     * 服务器IP（冗余字段，便于查询）
     */
    @Column(name = "server_ip")
    private String serverIp;

    /**
     * 服务器别名（冗余字段，便于查询）
     */
    @Column(name = "server_alias")
    private String serverAlias;

    /**
     * 部署状态: 0-待执行 1-成功 2-失败
     */
    @Column(name = "deploy_status", columnDefinition = "INTEGER DEFAULT 0")
    private Integer deployStatus;

    /**
     * 部署记录ID（关联到 deployment_record 表）
     */
    @Column(name = "deployment_record_id")
    private Integer deploymentRecordId;
}
