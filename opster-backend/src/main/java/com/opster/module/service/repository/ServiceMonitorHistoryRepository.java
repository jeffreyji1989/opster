package com.opster.module.service.repository;

import com.opster.module.service.entity.ServiceMonitorHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 服务监控历史记录仓库
 */
@Repository
public interface ServiceMonitorHistoryRepository extends JpaRepository<ServiceMonitorHistory, Integer> {

    /**
     * 查询指定服务的最近一条记录
     */
    Optional<ServiceMonitorHistory> findFirstByServiceIdOrderByRecordTimeDesc(Integer serviceId);

    /**
     * 查询指定服务的最近N条记录
     */
    List<ServiceMonitorHistory> findTopNByServiceIdOrderByRecordTimeDesc(Integer serviceId);

    /**
     * 查询指定服务在指定时间范围内的记录
     */
    List<ServiceMonitorHistory> findByServiceIdAndRecordTimeBetweenOrderByRecordTimeDesc(
            Integer serviceId,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * 删除指定时间之前的历史记录（用于数据清理）
     */
    @Modifying
    @Transactional
    void deleteByRecordTimeBefore(LocalDateTime time);

    /**
     * 查询所有服务的最新监控记录
     * 使用窗口函数获取每个服务的最新记录
     */
    @Query(value = "SELECT * FROM service_monitor_history WHERE id IN " +
            "(SELECT MAX(id) FROM service_monitor_history GROUP BY service_id)",
            nativeQuery = true)
    List<ServiceMonitorHistory> findAllLatestRecords();

    /**
     * 聚合查询服务的监控统计信息（最近一次记录）
     * 用于指定服务ID列表
     */
    @Query(value = "SELECT * FROM service_monitor_history WHERE id IN " +
            "(SELECT MAX(id) FROM service_monitor_history WHERE service_id IN (:serviceIds) GROUP BY service_id)",
            nativeQuery = true)
    List<ServiceMonitorHistory> findLatestByServiceIds(@Param("serviceIds") List<Integer> serviceIds);

    /**
     * 获取服务的历史记录数量
     */
    long countByServiceId(Integer serviceId);

    /**
     * 获取服务在指定时间范围内的记录数量
     */
    long countByServiceIdAndRecordTimeBetween(
            Integer serviceId,
            LocalDateTime start,
            LocalDateTime end
    );
}