package com.opster.config;

import com.opster.module.service.service.BatchDeploymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时清理配置
 * 定时清理已完成的批量任务
 */
@Slf4j
@Component
public class ScheduledCleanupConfig {

    @Autowired
    private BatchDeploymentService batchDeploymentService;

    /**
     * 定时清理已完成的批量任务
     * 每天凌晨2点执行一次
     * Cron表达式: 秒 分 时 日 月 周
     * 0 0 2 * * ? = 每天凌晨2点
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupCompletedBatchTasks() {
        try {
            log.info("开始清理已完成的批量任务...");
            batchDeploymentService.cleanupCompletedTasks();
            log.info("清理已完成批量任务完成");
        } catch (Exception e) {
            log.error("清理已完成批量任务失败", e);
        }
    }
}
