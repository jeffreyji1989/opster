package com.opster.common.enums;

/**
 * 运行状态枚举
 */
public enum RunStatus {
    /**
     * 未启动
     */
    NOT_STARTED(0, "未启动"),
    
    /**
     * 正常
     */
    NORMAL(1, "正常"),
    
    /**
     * 异常
     */
    ABNORMAL(2, "异常");
    
    private final Integer code;
    private final String desc;
    
    RunStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    /**
     * 根据code获取枚举
     */
    public static RunStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RunStatus status : RunStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
