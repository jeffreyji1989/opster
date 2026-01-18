package com.opster.module.service.repository;

import com.opster.common.enums.Status;
import com.opster.module.service.entity.AppService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppServiceRepository extends JpaRepository<AppService, Integer>, JpaSpecificationExecutor<AppService> {
    long countByStatus(Status status);
    List<AppService> findByProjectId(Integer projectId);
    List<AppService> findByServerId(Integer serverId);
}
