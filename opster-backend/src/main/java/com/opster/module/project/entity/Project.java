package com.opster.module.project.entity;

import com.opster.common.BaseEntity;
import com.opster.common.enums.Status;
import com.opster.module.project.convert.RepositoriesConverter;
import com.opster.module.project.dto.RepositoryDTO;
import jakarta.persistence.*;

import java.util.List;

/**
 * 项目实体类
 */
@Data
@Entity
@Table(name = "project")
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    public void setProjectOwner(String projectOwner) {
        this.projectOwner = projectOwner;
    }

    public List<RepositoryDTO> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepositoryDTO> repositories) {
        this.repositories = repositories;
    }

    public String getMonitorUrl() {
        return monitorUrl;
    }

    public void setMonitorUrl(String monitorUrl) {
        this.monitorUrl = monitorUrl;
    }

    public String getBusinessLine() {
        return businessLine;
    }

    public void setBusinessLine(String businessLine) {
        this.businessLine = businessLine;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
