package com.opster.common;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * SSH 工具类
 */
@Slf4j
public class SshUtils {

    /**
     * 使用 SSH 密钥对连接（优先使用公钥认证）
     *
     * @param host 主机地址
     * @param port 端口
     * @param user 用户名
     * @param privateKey 私钥（PEM 格式，加密存储）
     * @param passphrase 私钥密码（可选，加密存储）
     * @return SSH Session
     * @throws Exception 连接异常
     */
    public static Session connectWithKey(String host, int port, String user,
                                         String privateKey, String passphrase) throws Exception {
        JSch jsch = new JSch();

        // 解密私钥
        String decryptedPrivateKey = SecurityUtils.decrypt(privateKey);
        log.info("SSH 密钥认证连接 - 主机：{}, 端口：{}, 用户：{}, 私钥长度：{}",
                 host, port, user,
                 decryptedPrivateKey != null ? decryptedPrivateKey.length() : 0);

        // 创建临时文件存储私钥（JSch 0.1.55 需要从文件读取）
        java.nio.file.Path tempKeyFile = java.nio.file.Files.createTempFile("ssh_key_", ".pem");
        java.nio.file.Files.write(tempKeyFile, decryptedPrivateKey.getBytes(StandardCharsets.UTF_8));
        java.nio.file.Files.setPosixFilePermissions(tempKeyFile, 
            java.util.Set.of(
                java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
            ));

        try {
            if (passphrase != null && !passphrase.isEmpty()) {
                // 使用 passphrase 解密私钥
                String decryptedPassphrase = SecurityUtils.decrypt(passphrase);
                jsch.addIdentity(tempKeyFile.toString(), decryptedPassphrase);
                log.info("使用加密私钥连接");
            } else {
                // 私钥无 passphrase
                jsch.addIdentity(tempKeyFile.toString());
                log.info("使用无保护私钥连接");
            }
        } catch (Exception e) {
            // 添加失败时清理文件
            java.nio.file.Files.deleteIfExists(tempKeyFile);
            throw e;
        }
        // 注意：临时文件不立即删除，JSch 会在内部加载密钥
        // 稍后由 JVM 垃圾回收或系统临时文件清理机制处理

        Session session = jsch.getSession(user, host, port);

        // 配置 SSH 连接参数
        Properties config = new Properties();
        // 禁用严格主机密钥检查
        config.put("StrictHostKeyChecking", "no");
        // 设置认证方式（优先使用公钥认证）
        config.put("PreferredAuthentications", "publickey,password");

        session.setConfig(config);
        session.setServerAliveInterval(30000); // 30 秒保活间隔
        session.setServerAliveCountMax(3);    // 最多 3 次保活失败

        log.info("开始 SSH 密钥认证连接：{}:{}", host, port);
        try {
            session.connect();
            log.info("SSH 密钥认证成功：{}:{}", host, port);
        } catch (Exception e) {
            log.error("SSH 密钥认证失败：{}:{}, 错误：{}", host, port, e.getMessage(), e);
            throw e;
        }

        return session;
    }

    /**
     * 使用 SSH 密钥对连接（从文件读取私钥）
     *
     * @param host 主机地址
     * @param port 端口
     * @param user 用户名
     * @param privateKeyPath 私钥文件路径
     * @param passphrase 私钥密码（可选）
     * @return SSH Session
     * @throws Exception 连接异常
     */
    public static Session connectWithKeyFile(String host, int port, String user,
                                             String privateKeyPath, String passphrase) throws Exception {
        JSch jsch = new JSch();

        // 从文件读取私钥
        String privateKey = Files.readString(Paths.get(privateKeyPath));

        byte[] privateKeyBytes = privateKey.getBytes(StandardCharsets.UTF_8);

        if (passphrase != null && !passphrase.isEmpty()) {
            jsch.addIdentity("identity", privateKeyBytes,
                           null,
                           passphrase.getBytes(StandardCharsets.UTF_8));
        } else {
            jsch.addIdentity("identity", privateKeyBytes);
        }

        Session session = jsch.getSession(user, host, port);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "publickey,password");

        session.setConfig(config);
        session.setServerAliveInterval(30000);
        session.setServerAliveCountMax(3);

        log.info("开始 SSH 密钥认证连接（文件）: {}:{}", host, port);
        session.connect();
        log.info("SSH 密钥认证成功（文件）: {}:{}", host, port);

        return session;
    }

    /**
     * 获取 SSH Session（使用默认配置）
     */
    public static Session connect(String host, int port, String user, String password) throws Exception {
        return connect(host, port, user, password, 30000, 300000);
    }

    /**
     * 获取 SSH Session（使用自定义配置）
     *
     * @param host 主机地址
     * @param port 端口
     * @param user 用户名
     * @param password 密码（加密）
     * @param connectionTimeout 连接超时（毫秒）
     * @param sessionTimeout Session 超时（毫秒）
     */
    public static Session connect(String host, int port, String user, String password,
                                 int connectionTimeout, int sessionTimeout) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);

        // 解密密码
        String decryptedPassword = SecurityUtils.decrypt(password);

        // 输出调试信息（不输出实际密码，只输出长度和格式）
        log.info("SSH连接信息 - 主机: {}, 端口: {}, 用户: {}, 原始密码长度: {}, 解密后密码长度: {}",
                 host, port, user,
                 password != null ? password.length() : 0,
                 decryptedPassword != null ? decryptedPassword.length() : 0);

        // 检测密码是否可能是未正确解密的格式
        if (decryptedPassword != null && decryptedPassword.length() == 32 &&
            decryptedPassword.matches("[0-9a-fA-F]{32}")) {
            log.error("警告：密码可能是MD5哈希格式，无法用于SSH认证！请重新设置服务器密码。");
            log.error("服务器 {}: {} 使用的密码可能是旧格式，需要在管理界面中重新输入正确的密码", host, user);
        }

        session.setPassword(decryptedPassword);

        // 配置 SSH 连接参数
        Properties config = new Properties();
        // 禁用严格主机密钥检查
        config.put("StrictHostKeyChecking", "no");
        // 设置认证方式（优先使用密码认证）
        config.put("PreferredAuthentications", "password,publickey,keyboard-interactive");

        session.setConfig(config);
        session.setTimeout(connectionTimeout);
        session.setServerAliveInterval(30000); // 30秒保活间隔
        session.setServerAliveCountMax(3);    // 最多3次保活失败

        log.info("开始连接 SSH 服务器: {}:{}, 连接超时: {}ms, Session超时: {}ms", host, port, connectionTimeout, sessionTimeout);
        try {
            session.connect();
            log.info("SSH 连接成功: {}:{}", host, port);
        } catch (Exception e) {
            log.error("SSH 连接失败: {}:{}, 错误: {}", host, port, e.getMessage(), e);
            throw e;
        }

        return session;
    }

    /**
     * 检查 Session 是否连接且可用
     *
     * @param session SSH 会话
     * @return true 如果连接可用，否则 false
     */
    public static boolean isConnected(Session session) {
        if (session == null || !session.isConnected()) {
            return false;
        }

        try {
            // 尝试执行一个简单命令来验证连接
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("echo OK");
            InputStream in = channel.getInputStream();
            channel.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String response = reader.readLine();
            channel.disconnect();
            return "OK".equals(response);
        } catch (Exception e) {
            log.warn("SSH 连接检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 执行命令并返回输出
     */
    public static String exec(Session session, String command) throws Exception {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("Session is not connected");
        }

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();

        channel.connect();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                output.append("ERROR: ").append(line).append("\n");
            }
        } finally {
            channel.disconnect();
        }

        return output.toString();
    }

    /**
     * 关闭 Session
     */
    public static void disconnect(Session session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
