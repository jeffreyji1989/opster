package com.opster.module.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 启动脚本 DTO
 */
@Data
public class StartScriptDTO {

    private Integer id;

    /**
     * 脚本名称
     */
    private String name;

    /**
     * 脚本描述
     */
    private String description;

    /**
     * 服务端口号
     */
    private Integer port;

    /**
     * JVM 参数
     */
    private String jvmArgs;

    /**
     * Java 命令前的命令
     */
    private String preJavaCmd;

    /**
     * Java 命令后的命令
     */
    private String postJavaCmd;

    /**
     * 当前生效的版本 ID
     */
    private Integer currentVersionId;

    /**
     * 是否为默认脚本：0-否 1-是
     */
    private Integer isDefault;

    /**
     * 当前版本的脚本内容
     */
    private String scriptContent;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新人
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
