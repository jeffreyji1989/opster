package com.opster.module.schedule.repository;

import com.opster.module.schedule.entity.ScheduledDeploymentServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 定时发版任务服务关联仓库
 */
public interface ScheduledDeploymentServiceRepository extends JpaRepository<ScheduledDeploymentServiceEntity, Integer> {

    /**
     * 根据任务ID查询所有关联的服务
     * @param taskId 任务ID
     * @return 服务关联列表
     */
    List<ScheduledDeploymentServiceEntity> findByTaskId(Integer taskId);

    /**
     * 根据任务ID删除所有关联的服务
     * @param taskId 任务ID
     */
    @Transactional
    void deleteByTaskId(Integer taskId);

    /**
     * 根据服务ID查询所有关联的任务
     * @param serviceId 服务ID
     * @return 服务关联列表
     */
    List<ScheduledDeploymentServiceEntity> findByServiceId(Integer serviceId);
}
