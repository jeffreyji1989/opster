package com.opster.module.monitor.entity;

import java.util.List;

/**
 * 服务器监控数据传输对象
 */
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

    // Getter and Setter methods
    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Long getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(Long memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public Long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(Long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public List<DiskInfo> getDiskInfos() {
        return diskInfos;
    }

    public void setDiskInfos(List<DiskInfo> diskInfos) {
        this.diskInfos = diskInfos;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    /**
     * 磁盘信息内部类
     */
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

        // Getter and Setter methods
        public String getMountedOn() {
            return mountedOn;
        }

        public void setMountedOn(String mountedOn) {
            this.mountedOn = mountedOn;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getUsed() {
            return used;
        }

        public void setUsed(String used) {
            this.used = used;
        }

        public String getAvail() {
            return avail;
        }

        public void setAvail(String avail) {
            this.avail = avail;
        }

        public String getUsePercent() {
            return usePercent;
        }

        public void setUsePercent(String usePercent) {
            this.usePercent = usePercent;
        }
    }
}
