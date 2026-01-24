package com.opster.module.monitor.service;

import com.opster.module.monitor.entity.ServerMetric;
import com.opster.module.monitor.entity.ServerMetricDTO;

import java.util.List;

public interface ServerMonitorService {
    
    /**
     * 获取指定服务器的当前实时指标
     */
    ServerMetricDTO getCurrentMetric(Integer serverId);

    /**
     * 保存指标快照
     */
    void saveMetricSnapshot(Integer serverId, ServerMetricDTO dto);

    /**
     * 获取历史数据
     */
    List<ServerMetric> getHistory(Integer serverId, String range);
}
