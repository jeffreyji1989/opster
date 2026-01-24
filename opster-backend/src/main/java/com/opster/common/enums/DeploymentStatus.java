package com.opster.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 部署状态枚举
 */
public enum DeploymentStatus {
    IN_PROGRESS(0, "进行中"),
    COMPLETED(1, "完成"),
    FAILED(2, "失败");

    private final Integer code;
    private final String name;

    DeploymentStatus(Integer code, String name) {
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

    public static DeploymentStatus getByCode(Integer code) {
        for (DeploymentStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
