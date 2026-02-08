package com.opster.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 * 用于发版、批量发版和定时发版等异步任务
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    // ==================== 单个服务部署线程池配置 ====================

    @Value("${opster.async.deployment.core-pool-size:4}")
    private int deploymentCorePoolSize;

    @Value("${opster.async.deployment.max-pool-size:16}")
    private int deploymentMaxPoolSize;

    @Value("${opster.async.deployment.queue-capacity:100}")
    private int deploymentQueueCapacity;

    // ==================== 批量部署线程池配置 ====================

    @Value("${opster.async.batch-deployment.core-pool-size:2}")
    private int batchDeploymentCorePoolSize;

    @Value("${opster.async.batch-deployment.max-pool-size:8}")
    private int batchDeploymentMaxPoolSize;

    @Value("${opster.async.batch-deployment.queue-capacity:50}")
    private int batchDeploymentQueueCapacity;

    // ==================== 定时部署线程池配置 ====================

    @Value("${opster.async.scheduled-deployment.core-pool-size:2}")
    private int scheduledDeploymentCorePoolSize;

    @Value("${opster.async.scheduled-deployment.max-pool-size:4}")
    private int scheduledDeploymentMaxPoolSize;

    @Value("${opster.async.scheduled-deployment.queue-capacity:20}")
    private int scheduledDeploymentQueueCapacity;

    /**
     * 单个服务部署线程池
     * Bean名称: deploymentExecutor
     * 核心线程数: 4（可配置）
     * 最大线程数: 16（可配置）
     * 队列容量: 100（可配置）
     * 拒绝策略: CallerRunsPolicy（调用者运行）
     */
    @Bean(name = "deploymentExecutor")
    public ThreadPoolTaskExecutor deploymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(deploymentCorePoolSize);
        executor.setMaxPoolSize(deploymentMaxPoolSize);
        executor.setQueueCapacity(deploymentQueueCapacity);
        executor.setThreadNamePrefix("deployment-");
        // 拒绝策略：由调用线程处理该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最大等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 批量部署线程池
     * Bean名称: batchDeploymentExecutor
     * 核心线程数: 2（可配置）
     * 最大线程数: 8（可配置）
     * 队列容量: 50（可配置）
     * 拒绝策略: CallerRunsPolicy（调用者运行）
     */
    @Bean(name = "batchDeploymentExecutor")
    public ThreadPoolTaskExecutor batchDeploymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(batchDeploymentCorePoolSize);
        executor.setMaxPoolSize(batchDeploymentMaxPoolSize);
        executor.setQueueCapacity(batchDeploymentQueueCapacity);
        executor.setThreadNamePrefix("batch-deployment-");
        // 拒绝策略：由调用线程处理该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最大等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 定时部署线程池
     * Bean名称: scheduledDeploymentExecutor
     * 核心线程数: 2（可配置）
     * 最大线程数: 4（可配置）
     * 队列容量: 20（可配置）
     * 拒绝策略: CallerRunsPolicy（调用者运行）
     */
    @Bean(name = "scheduledDeploymentExecutor")
    public ThreadPoolTaskExecutor scheduledDeploymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(scheduledDeploymentCorePoolSize);
        executor.setMaxPoolSize(scheduledDeploymentMaxPoolSize);
        executor.setQueueCapacity(scheduledDeploymentQueueCapacity);
        executor.setThreadNamePrefix("scheduled-deployment-");
        // 拒绝策略：由调用线程处理该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最大等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
