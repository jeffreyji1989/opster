package com.opster.module.project.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opster.common.BaseEntity;
import com.opster.common.enums.Status;
import com.opster.module.project.convert.RepositoriesConverter;
import com.opster.module.project.dto.RepositoryDTO;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 项目实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "project")
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 项目 Git 仓库列表（JSON 格式存储）
     * @deprecated 请使用 SubProject 管理子项目
     */
    @Deprecated
    @Column(name = "repositories", columnDefinition = "TEXT")
    @Convert(converter = RepositoriesConverter.class)
    private List<RepositoryDTO> repositories;

    /**
     * 部署根目录
     * 注：原字段名 deploy_path 已重命名为 deploy_root_path
     */
    @Column(name = "project_code", length = 50)
    private String projectCode;

    /**
     * 项目名称
     */
    @Column(name = "project_name", nullable = false)
    private String projectName;

    /**
     * 项目负责人
     */
    @Column(name = "project_owner")
    private String projectOwner;

//    /**
//     * 项目 Git 仓库列表（JSON 格式存储）
//     * 已迁移到 SubProject，暂时保留以兼容
//     */
//    @Deprecated
//    @Column(name = "repositories", columnDefinition = "TEXT")
//    @Convert(converter = RepositoriesConverter.class)
//    private List<RepositoryDTO> repositories;

    /**
     * Git 认证用户名（用于 HTTP/HTTPS 认证）
     * 已迁移到 SubProject，暂时保留以兼容
     */
    @Deprecated
    @Column(name = "git_username")
    private String gitUsername;

    /**
     * Git 认证密码（用于 HTTP/HTTPS 认证）
     * 已迁移到 SubProject，暂时保留以兼容
     */
    @Deprecated
    @Column(name = "git_password")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String gitPassword;

    /**
     * 部署根目录
     * 注：原字段名 deploy_path 已重命名为 deploy_root_path
     */
    @Column(name = "deploy_root_path")
    private String deployRootPath;

    /**
     * 业务线名称
     */
    @Column(name = "business_line")
    private String businessLine;

    /**
     * 状态: 0-禁用 1-启用
     */
    @Column(name = "status")
    @Enumerated(EnumType.ORDINAL)
    private Status status;
}
