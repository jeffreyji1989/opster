package com.opster.module.monitor.entity;

import lombok.Data;
import java.util.List;

/**
 * 服务器监控数据传输对象
 */
@Data
public class ServerMetricDTO {
    /** CPU使用率 */
    private Double cpuUsage;
    /** 内存总量(MB) */
    private Long memoryTotal;
    /** 已用内存(MB) */
    private Long memoryUsed;
    /** 内存使用率 */
    private Double memoryUsage;
    /** 磁盘信息列表 */
    private List<DiskInfo> diskInfos;
    /** 时间戳(HH:mm:ss格式) */
    private String time;

    /**
     * 磁盘信息内部类
     */
    @Data
    public static class DiskInfo {
        /** 挂载点 */
        private String mountedOn;
        /** 总大小 */
        private String size;
        /** 已用 */
        private String used;
        /** 可用 */
        private String avail;
        /** 使用率 */
        private String usePercent;
    }
}