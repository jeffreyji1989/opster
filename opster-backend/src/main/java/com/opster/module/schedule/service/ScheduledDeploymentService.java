package com.opster.module.schedule.service;

import com.opster.module.schedule.entity.ScheduledDeployment;
import com.opster.module.schedule.enums.ScheduledStatus;

import java.util.List;

/**
 * 定时发版服务接口
 */
public interface ScheduledDeploymentService {

    /**
     * 创建定时发版任务
     * @param task 定时发版任务
     * @return 创建的任务
     */
    ScheduledDeployment create(ScheduledDeployment task);

    /**
     * 更新定时发版任务
     * @param task 定时发版任务
     * @return 更新后的任务
     */
    ScheduledDeployment update(ScheduledDeployment task);

    /**
     * 根据ID获取任务
     * @param id 任务ID
     * @return 定时发版任务
     */
    ScheduledDeployment getById(Integer id);

    /**
     * 查询所有任务（按创建时间倒序）
     * @return 任务列表
     */
    List<ScheduledDeployment> getAll();

    /**
     * 查询待执行的任务
     * @return 待执行任务列表
     */
    List<ScheduledDeployment> getPendingTasks();

    /**
     * 删除任务
     * @param id 任务ID
     */
    void deleteById(Integer id);

    /**
     * 取消任务
     * @param id 任务ID
     */
    void cancelById(Integer id);

    /**
     * 检查并执行待执行的定时发版任务
     */
    void checkAndExecuteTasks();
}
