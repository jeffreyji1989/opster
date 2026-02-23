package com.opster.module.git.entity;

import com.opster.common.BaseEntity;
import com.opster.common.enums.Status;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Git 账号实体类
 * 用于管理多个 Git 账号，支持 HTTPS 和 SSH 两种认证方式
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "git_account")
public class GitAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 账号名称
     */
    @Column(name = "account_name", nullable = false)
    private String accountName;

    /**
     * Git 平台 (gitee/gitlab/github)
     */
    @Column(name = "git_platform", nullable = false)
    private String gitPlatform;

    /**
     * 描述
     */
    @Column(name = "description")
    private String description;

    /**
     * Git 用户名（HTTPS 认证使用）
     */
    @Column(name = "git_username")
    private String gitUsername;

    /**
     * Git 密码（HTTPS 认证使用，加密存储）
     * 序列化时不返回该字段
     */
    @Column(name = "git_password")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String gitPassword;

    /**
     * 认证方式: 0-HTTPS 1-SSH
     */
    @Column(name = "auth_type", columnDefinition = "INTEGER DEFAULT 0")
    private Integer authType;

    /**
     * SSH 私钥路径（SSH 认证使用，如 ~/.ssh/id_rsa）
     */
    @Column(name = "ssh_key_path")
    private String sshKeyPath;

    /**
     * SSH 私钥密码（可选，如果私钥有密码）
     */
    @Column(name = "ssh_key_passphrase")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String sshKeyPassphrase;

    /**
     * 状态: 0-禁用 1-启用
     */
    @Column(name = "status")
    @Enumerated(EnumType.ORDINAL)
    private Status status;
}
