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
     * 构建相关配置
     */
    private BuildConfig build = new BuildConfig();

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
}
