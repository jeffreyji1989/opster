package com.opster.module.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配置文件 DTO
 * 用于传输配置文件信息
 */
@Data
public class ConfigFileDTO {

    /**
     * 配置文件 ID（如果是从版本历史加载）
     */
    private Integer id;

    /**
     * 配置文件名
     */
    private String filename;

    /**
     * 配置文件内容
     */
    private String content;

    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;

    /**
     * 是否已部署
     */
    private Boolean isDeployed;

    /**
     * 版本数量
     */
    private Integer versionCount;

    /**
     * 版本标签
     */
    private String versionTag;

    /**
     * 版本描述
     */
    private String versionDescription;
}
