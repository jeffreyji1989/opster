package com.opster.common.enums;

/**
 * 服务类型枚举
 * 注：与 RepositoryType 保持一致，仅用于字段命名区分
 */
public enum ServiceType {
    /**
     * 前端
     */
    FRONTEND(0, "前端"),
    /**
     * 后端
     */
    BACKEND(1, "后端"),
    /**
     * 管理后台
     */
    ADMIN(2, "管理后台"),
    /**
     * 移动端
     */
    MOBILE(3, "移动端");

    private final Integer code;
    private final String desc;

    ServiceType(Integer code, String desc) {
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
    public static ServiceType fromCode(Integer code) {
        for (ServiceType type : ServiceType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
