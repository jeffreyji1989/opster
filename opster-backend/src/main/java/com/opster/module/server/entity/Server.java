package com.opster.module.server.entity;

import com.opster.common.BaseEntity;
import com.opster.common.enums.Status;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务器实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "server")
public class Server extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * IP地址
     */
    @Column(name = "ip", nullable = false)
    private String ip;

    /**
     * 别名
     */
    @Column(name = "alias")
    private String alias;

    /**
     * 用户名
     */
    @Column(name = "username")
    private String username;

    /**
     * 密码
     */
    @Column(name = "password")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * 分组
     */
    @Column(name = "group_name")
    private String groupName;

    /**
     * 环境: 生产/测试
     */
    @Column(name = "env")
    private String env;

    /**
     * 状态: 0-禁用 1-启用
     */
    @Column(name = "status")
    @Enumerated(EnumType.ORDINAL)
    private Status status;

    /**
     * 部署项目个数
     */
    @Column(name = "deployed_count")
    private Integer deployedCount;
}