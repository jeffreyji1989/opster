package com.opster.module.service.schedule;

import com.opster.module.service.service.ServiceHttpMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 服务HTTP监控定时任务
 * 每分钟执行一次监控检查，每天凌晨3点清理历史数据
 */
@Component
public class ServiceHttpMonitorScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ServiceHttpMonitorScheduler.class);

    @Autowired
    private ServiceHttpMonitorService serviceHttpMonitorService;

    /**
     * 定时监控所有服务
     * 每分钟的第0秒执行
     * Cron表达式: 秒 分 时 日 月 周
     * 0 * * * * ? = 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scheduledMonitor() {
        logger.info("开始执行服务HTTP监控定时任务");
        try {
            serviceHttpMonitorService.checkAllServices();
            logger.info("服务HTTP监控定时任务执行完成");
        } catch (Exception e) {
            logger.error("服务HTTP监控定时任务执行失败", e);
        }
    }

    /**
     * 定期清理历史数据
     * 每天凌晨3点执行
     * Cron表达式: 0 0 3 * * ? = 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledCleanup() {
        logger.info("开始清理服务监控历史数据");
        try {
            serviceHttpMonitorService.cleanupOldRecords(7);  // 保留7天数据
            logger.info("服务监控历史数据清理完成");
        } catch (Exception e) {
            logger.error("服务监控历史数据清理失败", e);
        }
    }
}