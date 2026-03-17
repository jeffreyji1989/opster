package com.opster.module.service.entity;

import com.opster.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 配置文件版本实体类
 * 用于存储配置文件的修改历史和部署状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "config_file_version")
public class ConfigFileVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 关联子项目 ID（应用层关联，无外键约束）
     */
    @Column(name = "sub_project_id", nullable = false)
    private Integer subProjectId;

    /**
     * 配置文件名
     * 如：application.yml, application-dev.yml
     */
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    /**
     * 配置文件内容
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 版本标签
     * 如：v1.0.0, stable, canary
     */
    @Column(name = "version_tag", length = 50)
    private String versionTag;

    /**
     * 版本描述
     */
    @Column(name = "version_description", length = 500)
    private String versionDescription;

    /**
     * Git 提交哈希（如果从 Git 同步）
     */
    @Column(name = "git_commit_hash", length = 100)
    private String gitCommitHash;

    /**
     * 是否已部署：0-未部署 1-已部署
     */
    @Column(name = "is_deployed", columnDefinition = "INTEGER DEFAULT 0")
    private Boolean isDeployed = false;

    /**
     * 部署时间
     */
    @Column(name = "deploy_time")
    private LocalDateTime deployTime;
}
