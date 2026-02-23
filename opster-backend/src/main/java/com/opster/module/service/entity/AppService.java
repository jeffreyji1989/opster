package com.opster.module.service.entity;

import com.opster.common.BaseEntity;
import com.opster.common.enums.RunStatus;
import com.opster.common.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

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
     * 子项目ID（关联到 sub_project 表）
     * 注：新增字段，用于关联子项目
     */
    @Column(name = "sub_project_id")
    private Integer subProjectId;

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
     * 监控地址
     */
    @Column(name = "monitor_url")
    private String monitorUrl;

    /**
     * 服务启动脚本
     */
    @Column(name = "start_script")
    private String startScript;

    /**
     * 启动脚本是否已上传到服务器
     * 0-未上传 1-已上传
     */
    @Column(name = "script_uploaded", columnDefinition = "INTEGER DEFAULT 0")
    private Integer scriptUploaded;

    /**
     * 上次发版时间
     */
    @Column(name = "last_deploy_time")
    private LocalDateTime lastDeployTime;

    // ========== 新增字段 - 支持一个 Git 地址配置多个部署服务 ==========

    /**
     * 服务名称（用于区分同一 Git 仓库的多个部署服务）
     */
    @Column(name = "service_name", length = 100)
    private String serviceName;

    /**
     * 服务类型: 0-前端 1-后端 2-管理后台 3-移动端
     * 注：原字段名 repository_type 重命名为 service_type
     */
    @Column(name = "service_type", columnDefinition = "INTEGER DEFAULT 1")
    private Integer serviceType;

    /**
     * 源码目录（系统自动计算，只读）
     * 计算公式：opster.deploy-path + projectCode + "source"
     * 同一 Git 仓库的多个服务共享同一个源码目录
     */
    @Column(name = "source_path", length = 500)
    private String sourcePath;

    /**
     * 编译目录（用户手动填写）
     * 每个部署服务有独立的编译目录
     */
    @Column(name = "compile_path", length = 500)
    private String compilePath;

    /**
     * 部署路径（用户手动填写）
     * 每个部署服务有独立的部署路径
     * 例如：/var/opster/eip
     */
    @Column(name = "deploy_path", length = 500)
    private String deployPath;

    /**
     * 编译脚本（统一 maven 命令和构建命令）
     * 后端：mvn clean package -DskipTests
     * 前端：npm install && npm run build
     */
    @Column(name = "build_script", columnDefinition = "TEXT")
    private String buildScript;

    // ========== 以下字段已迁移到 SubProject，暂时保留以兼容现有功能 ==========
    // @Deprecated

    /**
     * 对应项目的 Git 仓库地址
     * @deprecated 请使用 SubProject.gitUrl
     */
    @Deprecated
    @Column(name = "repo_git_url")
    private String repoGitUrl;

    /**
     * 项目git分支
     * @deprecated 请使用 SubProject.gitBranch
     */
    @Deprecated
    @Column(name = "git_branch")
    private String gitBranch;

    /**
     * maven命令
     * @deprecated 请使用 SubProject.buildCommand
     */
    @Deprecated
    @Column(name = "maven_cmd")
    private String mavenCmd;

    /**
     * 项目路径（相对Git仓库的子目录路径）
     * 例如：opster-backend、opster-frontend
     * @deprecated 请使用 SubProject.projectPath
     */
    @Deprecated
    @Column(name = "project_path")
    private String projectPath;

    /**
     * 仓库类型: 0-前端 1-后端 2-管理后台 3-移动端
     * @deprecated 请使用 SubProject.projectType
     */
    @Deprecated
    @Column(name = "repository_type", columnDefinition = "INTEGER DEFAULT 1")
    private Integer repositoryType;

    /**
     * 前端构建命令（如：npm run build）
     * @deprecated 请使用 SubProject.buildCommand
     */
    @Deprecated
    @Column(name = "build_cmd")
    private String buildCmd;

    /**
     * 运行时版本号
     * 前端项目：Node.js 版本，格式：v18.17.0、v20.10.0（手动填写）
     * 后端项目：JDK 版本，可选值：jdk8、jdk17（下拉选择）
     * @deprecated 请使用 SubProject.runVersion
     */
    @Deprecated
    @Column(name = "node_version", length = 20)
    private String nodeVersion;

    /**
     * 服务别名（用于构建部署目录结构）
     * 例如：backend、frontend、admin
     * 用于形成目录结构：{deployPath}/{projectCode}/{serviceAlias}/
     * @deprecated 请使用 SubProject.serviceAlias
     */
    @Deprecated
    @Column(name = "service_alias", length = 100)
    private String serviceAlias;
}
