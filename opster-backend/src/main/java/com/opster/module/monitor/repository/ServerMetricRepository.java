package com.opster.module.monitor.repository;

import com.opster.module.monitor.entity.ServerMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServerMetricRepository extends JpaRepository<ServerMetric, Integer> {
    
    /**
     * 查询某段时间内的监控数据
     */
    List<ServerMetric> findByServerIdAndRecordTimeBetweenOrderByRecordTimeAsc(Integer serverId, LocalDateTime start, LocalDateTime end);

    /**
     * 删除指定时间之前的数据
     */
    void deleteByRecordTimeBefore(LocalDateTime time);
}
