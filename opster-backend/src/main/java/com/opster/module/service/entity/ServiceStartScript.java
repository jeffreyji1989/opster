package com.opster.module.service.entity;

import com.opster.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务启动脚本实体类（主表）
 * 用于管理启动脚本配置，支持版本控制
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "service_start_script")
public class ServiceStartScript extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 脚本名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 脚本描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 服务端口号
     */
    @Column(name = "port")
    private Integer port;

    /**
     * JVM 参数（如：-Dspring.profiles.active=dev -Xmx512m）
     */
    @Column(name = "jvm_args", length = 1000)
    private String jvmArgs;

    /**
     * Java 命令执行前的命令（如：fuser -k 8089/tcp）
     */
    @Column(name = "pre_java_cmd", length = 1000)
    private String preJavaCmd;

    /**
     * Java 命令执行后的命令（如：curl http://localhost:8080/health）
     */
    @Column(name = "post_java_cmd", length = 1000)
    private String postJavaCmd;

    /**
     * 当前生效的版本 ID
     */
    @Column(name = "current_version_id")
    private Integer currentVersionId;

    /**
     * 是否为默认脚本：0-否 1-是
     */
    @Column(name = "is_default", columnDefinition = "INTEGER DEFAULT 0")
    private Integer isDefault;

    /**
     * 删除标志：0-未删除 1-已删除
     */
    @Column(name = "del_flag", columnDefinition = "INTEGER DEFAULT 0")
    private Integer delFlag;
}
