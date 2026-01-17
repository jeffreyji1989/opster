package com.opster.module.service.repository;

import com.opster.module.service.entity.AppService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppServiceRepository extends JpaRepository<AppService, Integer> {
    long countByStatus(Integer status);
    List<AppService> findByProjectId(Integer projectId);
    List<AppService> findByServerId(Integer serverId);
}
