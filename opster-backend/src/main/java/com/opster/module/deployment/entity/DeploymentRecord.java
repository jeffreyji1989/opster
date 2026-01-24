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

    /**
     * 备份文件路径
     */
    @Column(name = "backup_file_path", length = 500)
    private String backupFilePath;

    /**
     * 版本描述
     */
    @Column(name = "version_description", length = 500)
    private String versionDescription;

    /**
     * Git提交哈希
     */
    @Column(name = "git_commit_hash", length = 100)
    private String gitCommitHash;

    /**
     * 备份文件大小（字节）
     */
    @Column(name = "backup_file_size")
    private Long backupFileSize;

    /**
     * 版本标签
     */
    @Column(name = "version_tag", length = 50)
    private String versionTag;

    /**
     * 是否为回退记录
     */
    @Column(name = "is_rollback")
    private Boolean isRollback = false;

    /**
     * 回退源记录ID
     */
    @Column(name = "rollback_from_id")
    private Integer rollbackFromId;
}