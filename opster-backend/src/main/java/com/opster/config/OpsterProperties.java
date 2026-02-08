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
     * SSH 连接池配置
     */
    private SshPoolConfig sshPool = new SshPoolConfig();

    /**
     * 文件传输配置
     */
    private TransferConfig transfer = new TransferConfig();

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

    /**
     * SSH 连接池配置
     */
    @Data
    public static class SshPoolConfig {
        /**
         * 是否启用连接池
         */
        private Boolean enabled = true;

        /**
         * 每个服务器最大连接数
         */
        private Integer maxConnectionsPerServer = 2;

        /**
         * 保活间隔（毫秒）
         */
        private Integer keepAliveInterval = 30000;

        /**
         * 连接超时（毫秒）
         */
        private Integer connectionTimeout = 30000;

        /**
         * Session 超时（毫秒）
         */
        private Integer sessionTimeout = 300000;

        /**
         * 保活失败最大次数
         */
        private Integer serverAliveCountMax = 3;
    }

    /**
     * 文件传输配置
     */
    @Data
    public static class TransferConfig {
        /**
         * 缓冲区大小（字节）
         */
        private Integer bufferSize = 1048576; // 1MB

        /**
         * 分块大小（字节）
         */
        private Integer chunkSize = 5242880; // 5MB

        /**
         * 最大重试次数
         */
        private Integer maxRetries = 3;

        /**
         * 重试延迟（毫秒），逗号分隔
         */
        private String retryDelays = "0,2000,5000";

        /**
         * 进度推送间隔（百分比）
         */
        private Integer progressInterval = 10;

        /**
         * 是否优先使用 rsync
         */
        private Boolean preferRsync = true;

        /**
         * rsync 超时（毫秒）
         */
        private Integer rsyncTimeout = 600000;

        /**
         * 传输前检查配置
         */
        private PreCheckConfig preCheck = new PreCheckConfig();
    }

    /**
     * 传输前检查配置
     */
    @Data
    public static class PreCheckConfig {
        /**
         * 是否启用传输前检查
         */
        private Boolean enabled = true;

        /**
         * 是否检查磁盘空间
         */
        private Boolean checkDiskSpace = true;

        /**
         * 是否检查服务器负载
         */
        private Boolean checkServerLoad = true;

        /**
         * 最小剩余空间比例
         */
        private Double minFreeSpaceRatio = 0.2;

        /**
         * 最大平均负载
         */
        private Double maxLoadAverage = 8.0;
    }
}
