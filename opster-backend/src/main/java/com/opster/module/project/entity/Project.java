package com.opster.module.project.entity;

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
     * 项目编号
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

    /**
     * 项目 Git 仓库列表（JSON 格式存储）
     */
    @Column(name = "repositories", columnDefinition = "TEXT")
    @Convert(converter = RepositoriesConverter.class)
    private List<RepositoryDTO> repositories;

    /**
     * 项目监控地址
     */
    @Column(name = "monitor_url")
    private String monitorUrl;

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