package com.opster.module.schedule.entity;

import com.opster.common.BaseEntity;
import com.opster.module.schedule.enums.ScheduledStatus;
import jakarta.persistence.*;

/**
 * 定时发版任务实体类
 */
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getServiceId() {
        return serviceId;
    }

    public void setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getServerAlias() {
        return serverAlias;
    }

    public void setServerAlias(String serverAlias) {
        this.serverAlias = serverAlias;
    }

    public String getExecuteDate() {
        return executeDate;
    }

    public void setExecuteDate(String executeDate) {
        this.executeDate = executeDate;
    }

    public String getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(String executeTime) {
        this.executeTime = executeTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
