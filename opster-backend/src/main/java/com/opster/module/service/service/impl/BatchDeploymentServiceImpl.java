package com.opster.module.service.service.impl;

import com.opster.module.service.entity.AppService;
import com.opster.module.service.repository.AppServiceRepository;
import com.opster.module.service.service.BatchDeploymentService;
import com.opster.module.service.service.DeploymentOrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 批量发版服务实现类
 * 支持批量并发发版、进度查询和取消功能
 */
@Slf4j
@Service
public class BatchDeploymentServiceImpl implements BatchDeploymentService {

    @Autowired
    private DeploymentOrchestrationService deploymentOrchestrationService;

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Autowired
    @Qualifier("batchDeploymentExecutor")
    private ThreadPoolTaskExecutor batchDeploymentExecutor;

    /**
     * 批量任务状态存储
     * Key: batchTaskId
     * Value: BatchTaskInfo
     */
    private final ConcurrentHashMap<String, BatchTaskInfo> taskStore = new ConcurrentHashMap<>();

    /**
     * 默认并发数
     */
    private static final int DEFAULT_CONCURRENCY = 5;

    /**
     * 任务清理阈值（小时）
     * 超过此时长的已完成任务将被清理
     */
    private static final int CLEANUP_THRESHOLD_HOURS = 24;

    @Override
    public BatchDeploymentResult submitBatchDeployment(List<Integer> serviceIds, Integer concurrency) {
        // 生成批量任务ID
        String batchTaskId = "batch-" + System.currentTimeMillis();

        // 设置并发数
        int actualConcurrency = concurrency != null && concurrency > 0 ? concurrency : DEFAULT_CONCURRENCY;

        // 创建批量任务信息
        BatchTaskInfo taskInfo = new BatchTaskInfo();
        taskInfo.batchTaskId = batchTaskId;
        taskInfo.serviceIds = serviceIds;
        taskInfo.totalCount = serviceIds.size();
        taskInfo.concurrency = actualConcurrency;
        taskInfo.status = "RUNNING";
        taskInfo.startTime = System.currentTimeMillis();
        taskInfo.serviceStatusMap = new ConcurrentHashMap<>();

        // 初始化各服务状态
        for (Integer serviceId : serviceIds) {
            ServiceTaskStatus status = new ServiceTaskStatus();
            status.setServiceId(serviceId);
            status.setStatus("PENDING");
            status.setStartTime(null);
            status.setEndTime(null);
            taskInfo.serviceStatusMap.put(serviceId, status);
        }

        taskStore.put(batchTaskId, taskInfo);

        log.info("提交批量发版任务: {}, 服务数: {}, 并发数: {}", batchTaskId, serviceIds.size(), actualConcurrency);

        // 提交到线程池异步执行批量发版
        batchDeploymentExecutor.execute(() -> executeBatchDeployment(taskInfo));

        // 立即返回结果
        BatchDeploymentResult result = new BatchDeploymentResult();
        result.setBatchTaskId(batchTaskId);
        result.setMessage("批量发版任务已提交，任务ID: " + batchTaskId);
        return result;
    }

    @Override
    public BatchProgress getBatchProgress(String batchTaskId) {
        BatchTaskInfo taskInfo = taskStore.get(batchTaskId);
        if (taskInfo == null) {
            return null;
        }

        BatchProgress progress = new BatchProgress();
        progress.setBatchTaskId(taskInfo.batchTaskId);
        progress.setTotalServices(taskInfo.totalCount);
        progress.setStartTime(taskInfo.startTime);
        progress.setEndTime(taskInfo.endTime);
        progress.setStatus(taskInfo.status);

        // 统计各状态服务数
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger pending = new AtomicInteger(0);
        AtomicInteger running = new AtomicInteger(0);
        AtomicInteger cancelled = new AtomicInteger(0);

        List<ServiceTaskStatus> tasks = new ArrayList<>();
        for (ServiceTaskStatus status : taskInfo.serviceStatusMap.values()) {
            tasks.add(status);
            switch (status.getStatus()) {
                case "COMPLETED":
                    completed.incrementAndGet();
                    break;
                case "FAILED":
                    failed.incrementAndGet();
                    break;
                case "PENDING":
                    pending.incrementAndGet();
                    break;
                case "RUNNING":
                    running.incrementAndGet();
                    break;
                case "CANCELLED":
                    cancelled.incrementAndGet();
                    break;
            }
        }

        progress.setCompletedServices(completed.get());
        progress.setFailedServices(failed.get());
        progress.setPendingServices(pending.get());
        progress.setRunningServices(running.get());
        progress.setCancelledServices(cancelled.get());
        progress.setTasks(tasks);

        return progress;
    }

    @Override
    public boolean cancelBatchDeployment(String batchTaskId) {
        BatchTaskInfo taskInfo = taskStore.get(batchTaskId);
        if (taskInfo == null) {
            log.warn("批量任务不存在: {}", batchTaskId);
            return false;
        }

        if ("COMPLETED".equals(taskInfo.status) || "FAILED".equals(taskInfo.status) || "CANCELLED".equals(taskInfo.status)) {
            log.warn("批量任务已结束，无法取消: {}", batchTaskId);
            return false;
        }

        // 取消所有待处理的任务
        int cancelledCount = 0;
        for (ServiceTaskStatus status : taskInfo.serviceStatusMap.values()) {
            if ("PENDING".equals(status.getStatus())) {
                status.setStatus("CANCELLED");
                status.setMessage("任务已取消");
                status.setEndTime(System.currentTimeMillis());
                cancelledCount++;
            }
        }

        // 如果所有任务都已完成或取消，更新任务状态
        if (isAllTasksFinished(taskInfo)) {
            taskInfo.status = "CANCELLED";
            taskInfo.endTime = System.currentTimeMillis();
        }

        log.info("批量任务已取消 {} 个待处理任务: {}", cancelledCount, batchTaskId);
        return true;
    }

    @Override
    public void cleanupCompletedTasks() {
        long now = System.currentTimeMillis();
        long threshold = CLEANUP_THRESHOLD_HOURS * 60 * 60 * 1000L;

        Iterator<Map.Entry<String, BatchTaskInfo>> iterator = taskStore.entrySet().iterator();
        int cleanedCount = 0;

        while (iterator.hasNext()) {
            Map.Entry<String, BatchTaskInfo> entry = iterator.next();
            BatchTaskInfo taskInfo = entry.getValue();

            // 只清理已完成的任务
            if (("COMPLETED".equals(taskInfo.status) || "FAILED".equals(taskInfo.status) || "CANCELLED".equals(taskInfo.status))
                    && taskInfo.endTime != null
                    && (now - taskInfo.endTime) > threshold) {
                iterator.remove();
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.info("清理了 {} 个已完成的批量任务", cleanedCount);
        }
    }

    /**
     * 执行批量发版（核心逻辑）
     * 修复：移除 future.get() 阻塞等待，改用异步回调避免死锁
     */
    private void executeBatchDeployment(BatchTaskInfo taskInfo) {
        // 创建信号量控制并发数
        Semaphore semaphore = new Semaphore(taskInfo.concurrency);
        ExecutorService executorService = Executors.newFixedThreadPool(taskInfo.concurrency);
        CountDownLatch latch = new CountDownLatch(taskInfo.totalCount);

        log.info("开始执行批量发版任务: {}, 并发数: {}", taskInfo.batchTaskId, taskInfo.concurrency);

        for (Integer serviceId : taskInfo.serviceIds) {
            // 先获取信号量许可（控制并发数）
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                log.error("获取信号量许可失败: serviceId={}", serviceId, e);
                Thread.currentThread().interrupt();
                latch.countDown();
                continue;
            }

            ServiceTaskStatus status = taskInfo.serviceStatusMap.get(serviceId);
            if (status != null && "CANCELLED".equals(status.getStatus())) {
                // 任务已被取消
                semaphore.release();
                latch.countDown();
                continue;
            }

            // 更新状态为运行中
            if (status != null) {
                status.setStatus("RUNNING");
                status.setStartTime(System.currentTimeMillis());
                status.setMessage("正在部署...");
            }

            // 获取服务名称
            AppService service = appServiceRepository.findById(serviceId).orElse(null);
            if (service != null) {
                if (status != null) {
                    status.setServiceName("Service-" + serviceId);
                }

                // 执行部署（异步回调处理结果，不再阻塞等待）
                CompletableFuture<Integer> future = deploymentOrchestrationService.executeDeploymentAsync(serviceId);

                // 使用 whenComplete 异步回调处理结果
                future.whenComplete((recordId, throwable) -> {
                    try {
                        if (throwable != null) {
                            // 部署失败
                            log.error("服务部署失败: serviceId={}", serviceId, throwable);
                            if (status != null) {
                                status.setStatus("FAILED");
                                status.setMessage("部署失败: " + throwable.getMessage());
                                status.setEndTime(System.currentTimeMillis());
                            }
                        } else {
                            // 部署成功
                            log.info("服务部署成功: serviceId={}, recordId={}", serviceId, recordId);
                            if (status != null) {
                                status.setStatus("COMPLETED");
                                status.setMessage("部署成功");
                                status.setDeploymentRecordId(recordId);
                                status.setEndTime(System.currentTimeMillis());
                            }
                        }
                    } finally {
                        // 释放信号量许可
                        semaphore.release();
                        latch.countDown();
                    }
                });
            } else {
                // 服务不存在，直接标记失败
                log.error("服务不存在: serviceId={}", serviceId);
                if (status != null) {
                    status.setStatus("FAILED");
                    status.setMessage("服务不存在: " + serviceId);
                    status.setEndTime(System.currentTimeMillis());
                }
                semaphore.release();
                latch.countDown();
            }
        }

        try {
            // 等待所有任务完成（最多等待24小时）
            boolean finished = latch.await(24, TimeUnit.HOURS);

            if (finished) {
                // 检查是否有失败的任务
                boolean hasFailed = taskInfo.serviceStatusMap.values().stream()
                        .anyMatch(s -> "FAILED".equals(s.getStatus()));

                taskInfo.status = hasFailed ? "FAILED" : "COMPLETED";
            } else {
                taskInfo.status = "FAILED";
                log.error("批量任务超时: {}", taskInfo.batchTaskId);
            }

        } catch (InterruptedException e) {
            log.error("批量任务执行被中断: {}", taskInfo.batchTaskId, e);
            taskInfo.status = "FAILED";
            Thread.currentThread().interrupt();
        } finally {
            taskInfo.endTime = System.currentTimeMillis();
            executorService.shutdown();

            log.info("批量发版任务完成: {}, 状态: {}, 耗时: {}ms",
                    taskInfo.batchTaskId,
                    taskInfo.status,
                    taskInfo.endTime - taskInfo.startTime);
        }
    }

    /**
     * 检查所有任务是否都已完成
     */
    private boolean isAllTasksFinished(BatchTaskInfo taskInfo) {
        return taskInfo.serviceStatusMap.values().stream()
                .allMatch(s -> "COMPLETED".equals(s.getStatus())
                        || "FAILED".equals(s.getStatus())
                        || "CANCELLED".equals(s.getStatus()));
    }

    /**
     * 批量任务信息（内部类）
     */
    private static class BatchTaskInfo {
        String batchTaskId;
        List<Integer> serviceIds;
        int totalCount;
        int concurrency;
        String status;
        long startTime;
        Long endTime;
        ConcurrentHashMap<Integer, ServiceTaskStatus> serviceStatusMap;
    }
}
