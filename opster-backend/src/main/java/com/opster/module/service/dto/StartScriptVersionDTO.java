package com.opster.module.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 启动脚本版本 DTO
 */
@Data
public class StartScriptVersionDTO {

    private Integer id;

    /**
     * 关联脚本 ID
     */
    private Integer scriptId;

    /**
     * 版本号
     */
    private String versionNo;

    /**
     * 版本描述
     */
    private String versionDescription;

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
     * 生成的完整脚本内容
     */
    private String scriptContent;

    /**
     * 是否为当前激活版本：0-否 1-是
     */
    private Integer isActive;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
