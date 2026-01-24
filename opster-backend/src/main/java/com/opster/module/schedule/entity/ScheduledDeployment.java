package com.opster.module.schedule.entity;

import com.opster.common.BaseEntity;
import com.opster.module.schedule.enums.ScheduledStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时发版任务实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "scheduled_deployment")
public class ScheduledDeployment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 任务名称
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 服务ID
     */
    @Column(name = "service_id", nullable = false)
    private Integer serviceId;

    /**
     * 项目名称
     */
    @Column(name = "project_name")
    private String projectName;

    /**
     * 服务器IP
     */
    @Column(name = "server_ip")
    private String serverIp;

    /**
     * 服务器别名
     */
    @Column(name = "server_alias")
    private String serverAlias;

    /**
     * 执行日期 yyyy-MM-dd
     */
    @Column(name = "execute_date", nullable = false)
    private String executeDate;

    /**
     * 执行时间 HH:mm
     */
    @Column(name = "execute_time", nullable = false)
    private String executeTime;

    /**
     * 状态: 0-待执行 1-已完成 2-已取消
     */
    @Column(name = "status", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer status;

    /**
     * 备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
}