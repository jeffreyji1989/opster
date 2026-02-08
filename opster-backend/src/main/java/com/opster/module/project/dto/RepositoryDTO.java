package com.opster.module.project.dto;

import com.opster.common.enums.RepositoryType;
import lombok.Data;

/**
 * 仓库信息数据传输对象
 */
@Data
public class RepositoryDTO {
    /**
     * 仓库类型
     */
    private RepositoryType type;

    /**
     * Git 仓库地址
     */
    private String gitUrl;

    /**
     * 项目部署路径
     */
    private String projectPath;

    /**
     * 仓库描述
     */
    private String description;

    /**
     * 仓库别名
     * 用于标识和区分不同的仓库，在发版时会作为服务别名使用
     */
    private String alias;
}