package com.opster.common;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * SSH 工具类
 */
@Slf4j
public class SshUtils {

    /**
     * 获取 SSH Session
     */
    public static Session connect(String host, int port, String user, String password) throws Exception {
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

        // 配置 SSH 连接参数（使用最基本的配置，避免兼容性问题）
        Properties config = new Properties();
        // 禁用严格主机密钥检查
        config.put("StrictHostKeyChecking", "no");

        // 设置认证方式（优先使用密码认证）
        config.put("PreferredAuthentications", "password,publickey,keyboard-interactive");

        session.setConfig(config);
        session.setTimeout(30000); // 30s timeout

        log.info("开始连接 SSH 服务器: {}:{}", host, port);
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
