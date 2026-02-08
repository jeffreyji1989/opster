package com.opster.module.service.service;

import java.util.List;

/**
 * 批量发版服务接口
 * 支持批量并发发版、进度查询和取消功能
 */
public interface BatchDeploymentService {

    /**
     * 批量发版进度信息
     */
    class BatchProgress {
        private String batchTaskId;          // 批量任务ID
        private int totalServices;            // 总服务数
        private int completedServices;        // 已完成服务数
        private int failedServices;           // 失败服务数
        private int pendingServices;          // 待处理服务数
        private int runningServices;          // 运行中服务数
        private int cancelledServices;        // 已取消服务数
        private long startTime;               // 开始时间（时间戳）
        private Long endTime;                 // 结束时间（时间戳）
        private String status;                // 任务状态：RUNNING, COMPLETED, FAILED, CANCELLED
        private List<ServiceTaskStatus> tasks; // 各服务任务状态

        // Getters and Setters
        public String getBatchTaskId() { return batchTaskId; }
        public void setBatchTaskId(String batchTaskId) { this.batchTaskId = batchTaskId; }
        public int getTotalServices() { return totalServices; }
        public void setTotalServices(int totalServices) { this.totalServices = totalServices; }
        public int getCompletedServices() { return completedServices; }
        public void setCompletedServices(int completedServices) { this.completedServices = completedServices; }
        public int getFailedServices() { return failedServices; }
        public void setFailedServices(int failedServices) { this.failedServices = failedServices; }
        public int getPendingServices() { return pendingServices; }
        public void setPendingServices(int pendingServices) { this.pendingServices = pendingServices; }
        public int getRunningServices() { return runningServices; }
        public void setRunningServices(int runningServices) { this.runningServices = runningServices; }
        public int getCancelledServices() { return cancelledServices; }
        public void setCancelledServices(int cancelledServices) { this.cancelledServices = cancelledServices; }
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public Long getEndTime() { return endTime; }
        public void setEndTime(Long endTime) { this.endTime = endTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<ServiceTaskStatus> getTasks() { return tasks; }
        public void setTasks(List<ServiceTaskStatus> tasks) { this.tasks = tasks; }
    }

    /**
     * 单个服务任务状态
     */
    class ServiceTaskStatus {
        private Integer serviceId;            // 服务ID
        private String serviceName;           // 服务名称
        private String status;                // 任务状态：PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
        private String message;               // 状态消息
        private Integer deploymentRecordId;   // 部署记录ID
        private Long startTime;               // 开始时间（时间戳）
        private Long endTime;                 // 结束时间（时间戳）

        // Getters and Setters
        public Integer getServiceId() { return serviceId; }
        public void setServiceId(Integer serviceId) { this.serviceId = serviceId; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Integer getDeploymentRecordId() { return deploymentRecordId; }
        public void setDeploymentRecordId(Integer deploymentRecordId) { this.deploymentRecordId = deploymentRecordId; }
        public Long getStartTime() { return startTime; }
        public void setStartTime(Long startTime) { this.startTime = startTime; }
        public Long getEndTime() { return endTime; }
        public void setEndTime(Long endTime) { this.endTime = endTime; }
    }

    /**
     * 批量发版响应结果
     */
    class BatchDeploymentResult {
        private String batchTaskId;           // 批量任务ID
        private String message;               // 响应消息

        // Getters and Setters
        public String getBatchTaskId() { return batchTaskId; }
        public void setBatchTaskId(String batchTaskId) { this.batchTaskId = batchTaskId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 提交批量发版任务
     * 使用 @Async("batchDeploymentExecutor") 异步执行
     * 使用 Semaphore 控制并发数（默认5）
     *
     * @param serviceIds 服务ID列表
     * @param concurrency 并发数（可选，默认5）
     * @return 批量发版结果（包含任务ID）
     */
    BatchDeploymentResult submitBatchDeployment(List<Integer> serviceIds, Integer concurrency);

    /**
     * 查询批量发版进度
     *
     * @param batchTaskId 批量任务ID
     * @return 批量进度信息
     */
    BatchProgress getBatchProgress(String batchTaskId);

    /**
     * 取消批量发版任务
     * 注意：只能取消待处理(PENDING)的任务，正在运行的任务无法取消
     *
     * @param batchTaskId 批量任务ID
     * @return 取消是否成功
     */
    boolean cancelBatchDeployment(String batchTaskId);

    /**
     * 清理已完成的批量任务（定时任务使用）
     */
    void cleanupCompletedTasks();
}
