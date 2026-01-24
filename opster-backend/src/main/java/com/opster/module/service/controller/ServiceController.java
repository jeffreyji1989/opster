package com.opster.module.service.controller;

import com.opster.module.service.entity.AppService;
import com.opster.module.service.service.AppServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 服务管理控制器
 */
@RestController
@RequestMapping("/api/service")
public class ServiceController {

    @Autowired
    private AppServiceService appServiceService;

    /**
     * 获取所有服务
     */
    @GetMapping("/list")
    public List<AppService> list(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String businessLine,
            @RequestParam(required = false) String env,
            @RequestParam(required = false) Integer runStatus,
            @RequestParam(required = false) Integer status) {
        return appServiceService.findList(projectId, businessLine, env, runStatus, status);
    }

    /**
     * 分页查询服务
     */
    @GetMapping("/page")
    public Page<AppService> page(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        return appServiceService.findPage(PageRequest.of(page - 1, size));
    }

    /**
     * 根据ID获取服务
     */
    @GetMapping("/{id}")
    public AppService getById(@PathVariable Integer id) {
        return appServiceService.findById(id).orElse(null);
    }

    /**
     * 新增服务
     */
    @PostMapping
    public boolean save(@RequestBody AppService service) {
        appServiceService.save(service);
        return true;
    }

    /**
     * 批量新增服务
     */
    @PostMapping("/batch")
    public boolean saveBatch(@RequestBody List<AppService> services) {
        for (AppService service : services) {
            appServiceService.save(service);
        }
        return true;
    }

    /**
     * 修改服务
     */
    @PutMapping
    public boolean update(@RequestBody AppService service) {
        appServiceService.save(service);
        return true;
    }

    /**
     * 删除服务
     */
    @DeleteMapping("/{id}")
    public boolean remove(@PathVariable Integer id) {
        appServiceService.deleteById(id);
        return true;
    }

    // --- Actions ---

    /**
     * 编译并重启
     */
    @PostMapping("/{id}/compile-restart")
    public String compileAndRestart(@PathVariable Integer id) {
        return appServiceService.compileAndRestart(id);
    }

    /**
     * 服务重启
     */
    @PostMapping("/{id}/restart")
    public String restart(@PathVariable Integer id) {
        return appServiceService.restart(id);
    }

    /**
     * 服务启动
     */
    @PostMapping("/{id}/start")
    public String start(@PathVariable Integer id) {
        return appServiceService.start(id);
    }

    /**
     * 查看日志
     */
    @GetMapping("/{id}/log")
    public String viewLog(@PathVariable Integer id) {
        return appServiceService.viewLog(id);
    }

    // --- Monitor ---
    
    /**
     * 监控查询
     */
    @GetMapping("/monitor")
    public List<Map<String, Object>> monitor(@RequestParam(required = false) Integer projectId,
                                             @RequestParam(required = false) String ip) {
        return appServiceService.searchMonitor(projectId, ip);
    }
}
