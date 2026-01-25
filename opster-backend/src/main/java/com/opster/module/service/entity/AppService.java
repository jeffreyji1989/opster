package com.opster.module.service.entity;

import com.opster.common.BaseEntity;
import com.opster.common.enums.RunStatus;
import com.opster.common.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
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
     * 对应项目的 Git 仓库地址
     */
    @Column(name = "repo_git_url")
    private String repoGitUrl;

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
     * 运行状态: 0-未启动 1-正常 2-异常
     */
    @Column(name = "run_status")
    @Enumerated(EnumType.ORDINAL)
    private RunStatus runStatus;

    /**
     * 是否启用: 0-禁用 1-启用
     */
    @Column(name = "status", nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    @Enumerated(EnumType.ORDINAL)
    private Status status;

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

    /**
     * 仓库类型: 0-前端 1-后端 2-管理后台 3-移动端
     */
    @Column(name = "repository_type", columnDefinition = "INTEGER DEFAULT 1")
    private Integer repositoryType;

    /**
     * 前端构建命令（如：npm run build）
     */
    @Column(name = "build_cmd")
    private String buildCmd;

    /**
     * 项目路径（相对Git仓库的子目录路径）
     * 例如：opster-backend、opster-frontend
     */
    @Column(name = "project_path")
    private String projectPath;
}