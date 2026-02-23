package com.opster.module.project.entity;

import com.opster.common.BaseEntity;
import com.opster.common.enums.RunStatus;
import com.opster.common.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 子项目实体类
 * 支持一个项目包含多个子项目（前端、后端、移动端等），每个子项目独立配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sub_project")
public class SubProject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ========== 基本信息 ==========

    /**
     * 关联项目 ID（应用层关联，无外键约束）
     */
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    /**
     * 子项目名称
     */
    @Column(name = "sub_project_name", nullable = false)
    private String subProjectName;

    /**
     * 项目类型: frontend/backend/mobile/admin
     */
    @Column(name = "project_type")
    private String projectType;

    /**
     * 服务别名（用于构建部署目录）
     */
    @Column(name = "service_alias")
    private String serviceAlias;

    // ========== Git 仓库信息 ==========

    /**
     * 关联 Git 账号 ID（应用层关联，无外键约束）
     */
    @Column(name = "git_account_id")
    private Integer gitAccountId;

    /**
     * Git 仓库地址
     */
    @Column(name = "git_url", nullable = false)
    private String gitUrl;

    /**
     * Git 分支名称
     */
    @Column(name = "git_branch")
    private String gitBranch;

    /**
     * 项目相对路径（相对 Git 仓库根目录）
     */
    @Column(name = "project_path")
    private String projectPath;

    // ========== 部署配置 ==========

    /**
     * 部署服务器 ID（应用层关联，无外键约束）
     */
    @Column(name = "server_id")
    private Integer serverId;

    /**
     * 部署路径
     */
    @Column(name = "deploy_path")
    private String deployPath;

    /**
     * 服务端口号
     */
    @Column(name = "port")
    private Integer port;

    // ========== 构建配置 ==========

    /**
     * 构建命令（支持多行命令）
     */
    @Column(name = "build_command", columnDefinition = "TEXT")
    private String buildCommand;

    /**
     * 运行版本（Node 版本或 JDK 版本）
     */
    @Column(name = "run_version")
    private String runVersion;

    // ========== 配置文件（JSON 格式存储）==========

    /**
     * 配置文件列表（JSON 格式）
     * 存储格式: [{"filename":"application.yml","content":"server:\n  port: 8080"},{"filename":"config.properties","content":"app.name=opster"}]
     */
    @Column(name = "config_files", columnDefinition = "TEXT")
    private String configFiles;

    // ========== 执行脚本 ==========

    /**
     * 启动脚本（支持多行命令）
     */
    @Column(name = "start_script", columnDefinition = "TEXT")
    private String startScript;

    /**
     * 停止脚本（支持多行命令）
     */
    @Column(name = "stop_script", columnDefinition = "TEXT")
    private String stopScript;

    /**
     * 重启脚本（支持多行命令）
     */
    @Column(name = "restart_script", columnDefinition = "TEXT")
    private String restartScript;

    // ========== 监控配置 ==========

    /**
     * 监控地址
     */
    @Column(name = "monitor_url")
    private String monitorUrl;

    /**
     * 日志路径
     */
    @Column(name = "log_path")
    private String logPath;

    // ========== 状态信息 ==========

    /**
     * 运行状态: 0-未启动 1-正常 2-异常
     */
    @Column(name = "run_status")
    @Enumerated(EnumType.ORDINAL)
    private RunStatus runStatus;

    /**
     * 启用状态: 0-禁用 1-启用
     */
    @Column(name = "status")
    @Enumerated(EnumType.ORDINAL)
    private Status status;

    /**
     * 上次部署时间
     */
    @Column(name = "last_deploy_time")
    private LocalDateTime lastDeployTime;
}
