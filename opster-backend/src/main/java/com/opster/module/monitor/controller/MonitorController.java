package com.opster.module.monitor.controller;

import com.opster.module.monitor.entity.ServerMetric;
import com.opster.module.monitor.service.ServerMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    @Autowired
    private ServerMonitorService serverMonitorService;

    @GetMapping("/history")
    public List<ServerMetric> getHistory(@RequestParam Integer serverId, @RequestParam(defaultValue = "1h") String range) {
        return serverMonitorService.getHistory(serverId, range);
    }
}
