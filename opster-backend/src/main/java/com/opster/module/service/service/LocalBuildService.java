package com.opster.module.service.service;

import com.opster.common.enums.RepositoryType;
import org.springframework.web.socket.WebSocketSession;

import java.nio.file.Path;

/**
 * 本地打包服务接口
 * 负责在本地执行Git拉取、Maven/npm构建、产物归档等操作
 */
public interface LocalBuildService {

    /**
     * 执行本地打包（完整流程）
     *
     * @param projectCode 项目编码
     * @param repositoryType 仓库类型（BACKEND/FRONTEND/ADMIN/MOBILE）
     * @param gitUrl Git仓库地址
     * @param gitBranch Git分支名称
     * @param buildCmd 构建命令（Maven或npm命令）
     * @param projectPath 项目路径（相对Git仓库的子目录路径），例如：opster-backend
     * @param wsSession WebSocket会话，用于实时推送日志
     * @param username Git认证用户名（可选）
     * @param password Git认证密码（可选）
     * @param nodeVersion Node.js 版本号（可选），仅前端项目有效
     * @return 打包产物的本地路径（jar文件或zip文件）
     * @throws Exception 打包过程中发生的异常
     */
    Path buildArtifact(String projectCode,
                      RepositoryType repositoryType,
                      String gitUrl,
                      String gitBranch,
                      String buildCmd,
                      String projectPath,
                      WebSocketSession wsSession,
                      String username,
                      String password,
                      String nodeVersion) throws Exception;

    /**
     * 执行本地Maven打包
     *
     * @param projectCode 项目编码
     * @param gitUrl Git仓库地址
     * @param gitBranch Git分支名称
     * @param mavenCmd Maven构建命令
     * @param projectPath 项目路径（相对Git仓库的子目录路径），例如：opster-backend
     * @param wsSession WebSocket会话
     * @param username Git认证用户名（可选）
     * @param password Git认证密码（可选）
     * @param jdkVersion JDK版本（可选）：jdk8、jdk17
     * @return 打包产物的本地路径（jar文件）
     * @throws Exception 打包过程中发生的异常
     */
    Path buildMavenArtifact(String projectCode,
                           String gitUrl,
                           String gitBranch,
                           String mavenCmd,
                           String projectPath,
                           WebSocketSession wsSession,
                           String username,
                           String password,
                           String jdkVersion) throws Exception;

    /**
     * 执行本地npm打包
     *
     * @param projectCode 项目编码
     * @param gitUrl Git仓库地址
     * @param gitBranch Git分支名称
     * @param buildCmd 构建命令（如：npm run build）
     * @param projectPath 项目路径（相对Git仓库的子目录路径），例如：opster-frontend
     * @param wsSession WebSocket会话
     * @param username Git认证用户名（可选）
     * @param password Git认证密码（可选）
     * @param nodeVersion Node.js 版本号（可选），格式：v18.17.0
     * @return 打包产物的本地路径（zip文件）
     * @throws Exception 打包过程中发生的异常
     */
    Path buildNpmArtifact(String projectCode,
                         String gitUrl,
                         String gitBranch,
                         String buildCmd,
                         String projectPath,
                         WebSocketSession wsSession,
                         String username,
                         String password,
                         String nodeVersion) throws Exception;

    /**
     * 清理旧版本的打包产物
     *
     * @param projectCode 项目编码
     * @param keepVersions 保留版本数量
     */
    void cleanupOldArtifacts(String projectCode, int keepVersions);

    /**
     * 获取项目源码目录路径
     *
     * @param projectCode 项目编码
     * @return 源码目录路径
     */
    Path getSourceDir(String projectCode);

    /**
     * 获取项目产物归档目录路径
     *
     * @param projectCode 项目编码
     * @return 产物归档目录路径
     */
    Path getArtifactsDir(String projectCode);
}
