package com.opster.module.service.entity;

import com.opster.common.BaseEntity;
import jakarta.persistence.*;

/**
 * 服务实体类
 */
@Entity
@Table(name = "service")
public class AppService extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 服务器ID
     */
    @Column(name = "server_id", nullable = false)
    private Integer serverId;

    /**
     * 项目ID
     */
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    /**
     * 项目git分支
     */
    @Column(name = "git_branch")
    private String gitBranch;

    /**
     * 部署路径
     */
    @Column(name = "deploy_path")
    private String deployPath;

    /**
     * 环境: 生产/测试
     */
    @Column(name = "env")
    private String env;

    /**
     * 服务端口号
     */
    @Column(name = "port")
    private Integer port;

    /**
     * 日志路径
     */
    @Column(name = "log_path")
    private String logPath;

    /**
     * 服务状态: 0-未启动 1-正常 2-异常
     */
    @Column(name = "status")
    private Integer status;

    /**
     * 是否启用: 0-禁用 1-启用
     */
    @Column(name = "enabled", nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private Integer enabled;

    /**
     * maven命令
     */
    @Column(name = "maven_cmd")
    private String mavenCmd;

    /**
     * 服务启动脚本
     */
    @Column(name = "start_script")
    private String startScript;

    /**
     * 监控地址
     */
    @Column(name = "monitor_url")
    private String monitorUrl;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public String getMavenCmd() {
        return mavenCmd;
    }

    public void setMavenCmd(String mavenCmd) {
        this.mavenCmd = mavenCmd;
    }

    public String getStartScript() {
        return startScript;
    }

    public void setStartScript(String startScript) {
        this.startScript = startScript;
    }

    public String getMonitorUrl() {
        return monitorUrl;
    }

    public void setMonitorUrl(String monitorUrl) {
        this.monitorUrl = monitorUrl;
    }
}
