package com.opster.module.schedule.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 定时发版任务状态枚举
 */
public enum ScheduledStatus {
    PENDING(0, "待执行"),
    COMPLETED(1, "已完成"),
    CANCELLED(2, "已取消");

    private final Integer code;
    private final String name;

    ScheduledStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * JSON序列化时返回code值，而非枚举名称
     */
    @JsonValue
    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ScheduledStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ScheduledStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
