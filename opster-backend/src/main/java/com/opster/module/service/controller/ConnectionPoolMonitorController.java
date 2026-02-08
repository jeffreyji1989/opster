package com.opster.module.service.controller;

import com.opster.module.service.transfer.pool.SshConnectionPool;
import com.opster.module.service.transfer.pool.SshConnectionPool.PoolStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连接池监控控制器
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class ConnectionPoolMonitorController {

    private final SshConnectionPool sshConnectionPool;

    /**
     * 获取连接池统计信息
     */
    @GetMapping("/connection-pool")
    public PoolStatistics getConnectionPoolStatistics() {
        return sshConnectionPool.getStatistics();
    }
}
