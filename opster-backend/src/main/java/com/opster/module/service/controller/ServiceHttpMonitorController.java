package com.opster.module.service.controller;

import com.opster.module.service.entity.ServiceMonitorHistory;
import com.opster.module.service.service.ServiceHttpMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务HTTP监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/service/monitor")
public class ServiceHttpMonitorController {

    @Autowired
    private ServiceHttpMonitorService serviceHttpMonitorService;

    /**
     * 获取所有服务的最新监控记录列表
     * @return 监控记录列表
     */
    @GetMapping("/list")
    public List<ServiceMonitorHistory> getMonitorList() {
        return serviceHttpMonitorService.getAllLatestRecords();
    }

    /**
     * 获取指定服务的最新监控记录
     * @param serviceId 服务ID
     * @return 监控记录
     */
    @GetMapping("/{serviceId}/latest")
    public ServiceMonitorHistory getLatestRecord(@PathVariable Integer serviceId) {
        return serviceHttpMonitorService.getLatestRecord(serviceId);
    }

    /**
     * 获取指定服务的监控历史记录
     * @param serviceId 服务ID
     * @param days 查询天数（默认7天）
     * @return 历史记录列表
     */
    @GetMapping("/{serviceId}/history")
    public List<ServiceMonitorHistory> getHistory(
            @PathVariable Integer serviceId,
            @RequestParam(defaultValue = "7") Integer days) {
        return serviceHttpMonitorService.getHistoryRecords(serviceId, days);
    }

    /**
     * 手动触发指定服务的监控检查
     * @param serviceId 服务ID
     * @return 操作结果
     */
    @PostMapping("/{serviceId}/check")
    public Map<String, Object> manualCheck(@PathVariable Integer serviceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            serviceHttpMonitorService.checkAndRecord(serviceId);
            result.put("success", true);
            result.put("message", "监控检查完成");
        } catch (Exception e) {
            log.error("手动监控检查失败", e);
            result.put("success", false);
            result.put("message", "监控检查失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 手动触发所有服务的监控检查
     * @return 操作结果
     */
    @PostMapping("/check-all")
    public Map<String, Object> checkAll() {
        Map<String, Object> result = new HashMap<>();
        try {
            serviceHttpMonitorService.checkAllServices();
            result.put("success", true);
            result.put("message", "所有服务监控检查完成");
        } catch (Exception e) {
            log.error("手动监控检查失败", e);
            result.put("success", false);
            result.put("message", "监控检查失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 清理历史数据
     * @param days 保留天数
     * @return 操作结果
     */
    @PostMapping("/cleanup")
    public Map<String, Object> cleanup(@RequestParam(defaultValue = "7") Integer days) {
        Map<String, Object> result = new HashMap<>();
        try {
            serviceHttpMonitorService.cleanupOldRecords(days);
            result.put("success", true);
            result.put("message", "历史数据清理完成");
        } catch (Exception e) {
            log.error("历史数据清理失败", e);
            result.put("success", false);
            result.put("message", "清理失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取所有可用于监控的服务列表
     * @return 服务列表
     */
    @GetMapping("/available-services")
    public List<Map<String, Object>> getAvailableServices() {
        return serviceHttpMonitorService.getAvailableServices();
    }

    /**
     * 更新服务的监控URL
     * @param serviceId 服务ID
     * @param request 请求参数
     * @return 操作结果
     */
    @PutMapping("/{serviceId}/monitor-url")
    public Map<String, Object> updateMonitorUrl(
            @PathVariable Integer serviceId,
            @RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String monitorUrl = request.get("monitorUrl");
            serviceHttpMonitorService.updateMonitorUrl(serviceId, monitorUrl);
            result.put("success", true);
            result.put("message", "监控URL更新成功");
        } catch (Exception e) {
            log.error("更新监控URL失败", e);
            result.put("success", false);
            result.put("message", "更新失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取监控统计信息（包含上一次记录）
     * @return 监控统计列表
     */
    @GetMapping("/statistics")
    public List<Map<String, Object>> getStatistics() {
        List<ServiceMonitorHistory> latestRecords = serviceHttpMonitorService.getAllLatestRecords();
        return latestRecords.stream().map(record -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("serviceId", record.getServiceId());
            stats.put("projectName", record.getProjectName());
            stats.put("serviceName", record.getServiceName());
            stats.put("serverIp", record.getServerIp());
            stats.put("port", record.getPort());
            stats.put("monitorUrl", record.getMonitorUrl());

            // 当前状态
            stats.put("currentStatus", record.getStatus());
            stats.put("currentTime", record.getRecordTime());
            stats.put("currentTimeStr", record.getRecordTime().toString());
            stats.put("currentResponseTime", record.getResponseTime());

            // 上一次状态（从历史记录中获取）
            List<ServiceMonitorHistory> history =
                serviceHttpMonitorService.getHistoryRecords(record.getServiceId(), 7);
            if (history.size() > 1) {
                ServiceMonitorHistory lastRecord = history.get(1);
                stats.put("lastStatus", lastRecord.getStatus());
                stats.put("lastTime", lastRecord.getRecordTime());
                stats.put("lastTimeStr", lastRecord.getRecordTime().toString());
                stats.put("lastResponseTime", lastRecord.getResponseTime());
            } else {
                stats.put("lastStatus", null);
                stats.put("lastTime", null);
                stats.put("lastTimeStr", null);
                stats.put("lastResponseTime", null);
            }

            return stats;
        }).toList();
    }
}