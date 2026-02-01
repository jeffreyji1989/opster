package com.opster.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Opster 配置属性类
 * 用于从 application.yml 中读取 opster 前缀的配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "opster")
public class OpsterProperties {

    /**
     * 本地部署根目录
     * 所有项目的源码、打包产物、日志都存储在此目录下
     */
    private String deployPath;

    /**
     * Maven 主目录
     * 用于本地 Maven 构建
     */
    private String mavenHome;

    /**
     * Java 主目录
     * 用于本地 Java 命令执行
     */
    private String javaHome;

    /**
     * 加密密钥
     */
    private String encryptKey;

    /**
     * JDK 多版本配置
     */
    private JdkConfig jdk = new JdkConfig();

    /**
     * 构建相关配置
     */
    private BuildConfig build = new BuildConfig();

    /**
     * Node.js 版本管理配置
     */
    private NodeJsConfig nodejs = new NodeJsConfig();

    /**
     * 构建配置内部类
     */
    @Data
    public static class BuildConfig {
        /**
         * 构建超时时间（分钟）
         * 默认 30 分钟
         */
        private int timeoutMinutes = 30;

        /**
         * 保留版本数量
         * 默认保留最近 5 个版本的打包产物
         */
        private int keepVersions = 5;
    }

    /**
     * Node.js 配置内部类
     */
    @Data
    public static class NodeJsConfig {
        /**
         * 是否启用 Node.js 版本管理
         */
        private Boolean enabled = true;

        /**
         * nvm 安装路径
         */
        private String nvmPath = System.getProperty("user.home") + "/.nvm";

        /**
         * 是否自动安装未安装的版本
         */
        private Boolean autoInstall = true;

        /**
         * 安装超时时间（秒）
         */
        private Integer installTimeout = 300;
    }

    /**
     * JDK 配置内部类
     */
    @Data
    public static class JdkConfig {
        /**
         * JDK 8 安装路径
         */
        private String jdk8;

        /**
         * JDK 17 安装路径
         */
        private String jdk17;
    }
}
