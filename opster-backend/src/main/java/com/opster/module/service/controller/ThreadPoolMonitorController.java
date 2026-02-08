package com.opster.module.service.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控控制器
 * 提供线程池状态查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
public class ThreadPoolMonitorController {

    @Autowired
    @Qualifier("deploymentExecutor")
    private ThreadPoolTaskExecutor deploymentTaskExecutor;

    @Autowired
    @Qualifier("batchDeploymentExecutor")
    private ThreadPoolTaskExecutor batchDeploymentTaskExecutor;

    @Autowired
    @Qualifier("scheduledDeploymentExecutor")
    private ThreadPoolTaskExecutor scheduledDeploymentTaskExecutor;

    /**
     * 查询所有线程池状态
     */
    @GetMapping("/thread-pools")
    public Map<String, Object> getThreadPoolsStatus() {
        Map<String, Object> result = new HashMap<>();

        result.put("deploymentExecutor", getThreadPoolInfo("deploymentExecutor", deploymentTaskExecutor));
        result.put("batchDeploymentExecutor", getThreadPoolInfo("batchDeploymentExecutor", batchDeploymentTaskExecutor));
        result.put("scheduledDeploymentExecutor", getThreadPoolInfo("scheduledDeploymentExecutor", scheduledDeploymentTaskExecutor));

        return result;
    }

    /**
     * 获取单个线程池的详细信息
     */
    private Map<String, Object> getThreadPoolInfo(String name, ThreadPoolTaskExecutor taskExecutor) {
        Map<String, Object> info = new HashMap<>();

        if (taskExecutor == null) {
            info.put("error", "线程池不存在");
            return info;
        }

        // 获取底层的 ThreadPoolExecutor
        ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();

        info.put("name", name);
        info.put("corePoolSize", executor.getCorePoolSize());
        info.put("maxPoolSize", executor.getMaximumPoolSize());
        info.put("currentPoolSize", executor.getPoolSize());
        info.put("activeCount", executor.getActiveCount());
        info.put("taskCount", executor.getTaskCount());
        info.put("completedTaskCount", executor.getCompletedTaskCount());
        info.put("queueSize", executor.getQueue().size());
        info.put("queueRemainingCapacity", executor.getQueue().remainingCapacity());
        info.put("isShutdown", executor.isShutdown());
        info.put("isTerminated", executor.isTerminated());
        info.put("isTerminating", executor.isTerminating());

        // 计算线程池使用率
        double usageRate = 0;
        if (executor.getMaximumPoolSize() > 0) {
            usageRate = (double) executor.getActiveCount() / executor.getMaximumPoolSize() * 100;
        }
        info.put("usageRate", String.format("%.2f%%", usageRate));

        // 计算队列使用率
        double queueUsageRate = 0;
        int queueCapacity = executor.getQueue().size() + executor.getQueue().remainingCapacity();
        if (queueCapacity > 0) {
            queueUsageRate = (double) executor.getQueue().size() / queueCapacity * 100;
        }
        info.put("queueUsageRate", String.format("%.2f%%", queueUsageRate));

        return info;
    }
}
