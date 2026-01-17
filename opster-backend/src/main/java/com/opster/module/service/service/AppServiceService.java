package com.opster.module.service.service;

import com.opster.module.service.entity.AppService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AppServiceService {
    List<AppService> findAll();
    Page<AppService> findPage(Pageable pageable);
    Optional<AppService> findById(Integer id);
    AppService save(AppService service);
    void deleteById(Integer id);
    long count();
    
    // Actions
    String compileAndRestart(Integer id);
    String restart(Integer id);
    String start(Integer id);
    String viewLog(Integer id);

    // Stats
    long countByStatus(Integer status);
    
    // Monitor
    List<Map<String, Object>> searchMonitor(Integer projectId, String ip);
}
