package com.opster.module.project.entity;

import com.opster.common.BaseEntity;
import jakarta.persistence.*;

/**
 * 项目实体类
 */
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
     * 项目git地址
     */
    @Column(name = "git_url")
    private String gitUrl;

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
    private Integer status;

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

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
