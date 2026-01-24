package com.opster.module.deployment.entity;

import com.opster.common.BaseEntity;
import com.opster.common.enums.DeploymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部署记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "deployment_record")
public class DeploymentRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 项目ID
     */
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    /**
     * 项目名称
     */
    @Column(name = "project_name", nullable = false)
    private String projectName;

    /**
     * 服务器ID
     */
    @Column(name = "server_id", nullable = false)
    private Integer serverId;

    /**
     * 服务器IP
     */
    @Column(name = "server_ip", nullable = false)
    private String serverIp;

    /**
     * 服务器别名
     */
    @Column(name = "server_alias")
    private String serverAlias;

    /**
     * 服务ID
     */
    @Column(name = "service_id", nullable = false)
    private Integer serviceId;

    /**
     * 服务名称
     */
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    /**
     * 部署状态
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private DeploymentStatus status;

    /**
     * 部署日志路径
     */
    @Column(name = "log_path", columnDefinition = "VARCHAR(255)")
    private String logPath;
}