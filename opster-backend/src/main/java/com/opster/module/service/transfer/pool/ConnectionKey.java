package com.opster.module.service.transfer.pool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH 连接键
 * 用于标识一个唯一的 SSH 连接（主机 + 端口 + 用户名）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionKey {
    private String host;
    private int port;
    private String username;

    @Override
    public String toString() {
        return username + "@" + host + ":" + port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConnectionKey that = (ConnectionKey) obj;
        return port == that.port &&
            host.equals(that.host) &&
            username.equals(that.username);
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        result = 31 * result + username.hashCode();
        return result;
    }
}
