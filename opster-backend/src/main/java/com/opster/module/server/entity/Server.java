package com.opster.module.server.entity;

import com.opster.common.BaseEntity;
import com.opster.common.enums.Status;
import jakarta.persistence.*;

/**
 * 服务器实体类
 */
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getDeployedCount() {
        return deployedCount;
    }

    public void setDeployedCount(Integer deployedCount) {
        this.deployedCount = deployedCount;
    }
}
