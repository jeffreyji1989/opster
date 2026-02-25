package com.opster.module.service.service;

import com.opster.module.service.entity.ServiceMonitorHistory;

import java.util.List;
import java.util.Map;

/**
 * 服务HTTP监控服务接口
 */
public interface ServiceHttpMonitorService {

    /**
     * 执行单次HTTP监控检查并保存记录
     * @param serviceId 服务ID
     */
    void checkAndRecord(Integer serviceId);

    /**
     * 获取服务的最新监控记录
     * @param serviceId 服务ID
     * @return 最新监控记录
     */
    ServiceMonitorHistory getLatestRecord(Integer serviceId);

    /**
     * 获取所有服务的最新监控记录
     * @return 最新监控记录列表
     */
    List<ServiceMonitorHistory> getAllLatestRecords();

    /**
     * 获取服务的监控历史记录
     * @param serviceId 服务ID
     * @param days 查询天数（默认7天）
     * @return 历史记录列表
     */
    List<ServiceMonitorHistory> getHistoryRecords(Integer serviceId, Integer days);

    /**
     * 删除指定天数之前的历史记录
     * @param days 天数
     */
    void cleanupOldRecords(Integer days);

    /**
     * 手动触发所有服务的监控检查
     */
    void checkAllServices();

    /**
     * 获取所有可用于监控的服务列表
     * @return 服务列表（包含服务信息、项目名称、服务器IP等）
     */
    List<Map<String, Object>> getAvailableServices();

    /**
     * 更新服务的监控URL
     * @param serviceId 服务ID
     * @param monitorUrl 监控URL
     */
    void updateMonitorUrl(Integer serviceId, String monitorUrl);
}