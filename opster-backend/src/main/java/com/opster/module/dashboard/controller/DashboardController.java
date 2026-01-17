package com.opster.module.dashboard.controller;

import com.opster.module.project.service.ProjectService;
import com.opster.module.server.service.ServerService;
import com.opster.module.service.service.AppServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页 Dashboard 控制器
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ServerService serverService;

    @Autowired
    private AppServiceService appServiceService;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("projectCount", projectService.count());
        stats.put("serverCount", serverService.count());
        stats.put("serviceCount", appServiceService.count());
        
        // Detailed Status
        stats.put("statusUnknown", appServiceService.countByStatus(0));
        stats.put("statusNormal", appServiceService.countByStatus(1));
        stats.put("statusError", appServiceService.countByStatus(2));
        
        return stats;
    }
}
