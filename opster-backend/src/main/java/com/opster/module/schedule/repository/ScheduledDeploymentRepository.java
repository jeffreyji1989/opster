package com.opster.module.schedule.repository;

import com.opster.module.schedule.entity.ScheduledDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 定时发版任务仓库
 */
public interface ScheduledDeploymentRepository extends JpaRepository<ScheduledDeployment, Integer> {

    /**
     * 查询所有待执行的任务，按执行时间排序
     */
    @Query("SELECT sd FROM ScheduledDeployment sd WHERE sd.status = :status ORDER BY sd.executeDate, sd.executeTime")
    List<ScheduledDeployment> findByStatusOrderByExecuteDateAndTime(@Param("status") Integer status);

    /**
     * 查询指定日期和时间的待执行任务
     */
    @Query("SELECT sd FROM ScheduledDeployment sd WHERE sd.status = :status AND sd.executeDate = :date AND sd.executeTime = :time")
    List<ScheduledDeployment> findByStatusAndDateAndTime(
            @Param("status") Integer status,
            @Param("date") String date,
            @Param("time") String time
    );

    /**
     * 按创建时间倒序查询所有任务
     */
    List<ScheduledDeployment> findAllByOrderByCreateTimeDesc();

    /**
     * 查询执行时间在前后2分钟内的待执行任务
     * 条件：status = 0 AND executeDate = currentDate AND executeTime >= timeBefore AND executeTime <= timeAfter
     * @param status 状态（0-待执行）
     * @param date 当前日期（yyyy-MM-dd）
     * @param timeBefore 前2分钟时间（HH:mm）
     * @param timeAfter 后2分钟时间（HH:mm）
     */
    @Query("SELECT sd FROM ScheduledDeployment sd WHERE sd.status = :status AND sd.executeDate = :date AND sd.executeTime >= :timeBefore AND sd.executeTime <= :timeAfter")
    List<ScheduledDeployment> findPendingTasksInTimeRange(
            @Param("status") Integer status,
            @Param("date") String date,
            @Param("timeBefore") String timeBefore,
            @Param("timeAfter") String timeAfter
    );
}
