package com.opster.module.service.entity;

import com.opster.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务启动脚本版本实体类（版本表）
 * 用于记录启动脚本的历史版本
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "service_start_script_version")
public class ServiceStartScriptVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 关联脚本 ID
     */
    @Column(name = "script_id", nullable = false)
    private Integer scriptId;

    /**
     * 版本号（如：v1.0.0, v1.0.1）
     */
    @Column(name = "version_no", nullable = false, length = 20)
    private String versionNo;

    /**
     * 版本描述
     */
    @Column(name = "version_description", length = 500)
    private String versionDescription;

    /**
     * 服务端口号
     */
    @Column(name = "port", nullable = false)
    private Integer port;

    /**
     * JVM 参数
     */
    @Column(name = "jvm_args", length = 1000)
    private String jvmArgs;

    /**
     * Java 命令执行前的命令
     */
    @Column(name = "pre_java_cmd", length = 1000)
    private String preJavaCmd;

    /**
     * Java 命令执行后的命令
     */
    @Column(name = "post_java_cmd", length = 1000)
    private String postJavaCmd;

    /**
     * 生成的完整脚本内容
     */
    @Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
    private String scriptContent;

    /**
     * 是否为当前激活版本：0-否 1-是
     */
    @Column(name = "is_active", columnDefinition = "INTEGER DEFAULT 0")
    private Integer isActive;

    /**
     * 删除标志：0-未删除 1-已删除
     */
    @Column(name = "del_flag", columnDefinition = "INTEGER DEFAULT 0")
    private Integer delFlag;
}
