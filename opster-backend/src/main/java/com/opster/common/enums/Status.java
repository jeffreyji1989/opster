package com.opster.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通用状态枚举
 */
public enum Status {
    /**
     * 禁用
     */
    DISABLED(0, "禁用"),
    
    /**
     * 启用
     */
    ENABLED(1, "启用");
    
    private final Integer code;
    private final String desc;
    
    Status(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    /**
     * JSON序列化时返回code值，而非枚举名称
     */
    @JsonValue
    public Integer getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    /**
     * 根据code获取枚举
     */
    public static Status fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (Status status : Status.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
