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
public class SshUtils {

    /**
     * 获取 SSH Session
     */
    public static Session connect(String host, int port, String user, String password) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout(30000); // 30s timeout
        session.connect();
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
