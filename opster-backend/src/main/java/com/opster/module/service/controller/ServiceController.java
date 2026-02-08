package com.opster.module.service.controller;

import com.opster.module.service.entity.AppService;
import com.opster.module.service.service.AppServiceService;
import com.opster.module.service.service.BatchDeploymentService;
import com.opster.module.service.service.DeploymentOrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 服务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/service")
public class ServiceController {

    @Autowired
    private AppServiceService appServiceService;

    @Autowired
    private DeploymentOrchestrationService deploymentOrchestrationService;

    @Autowired
    private BatchDeploymentService batchDeploymentService;

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

    // --- 异步部署 API ---

    /**
     * 异步发版（后台执行，不打开 WebSocket 窗口）
     */
    @PostMapping("/{id}/deploy-async")
    public Map<String, Object> deployAsync(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            CompletableFuture<Integer> future = deploymentOrchestrationService.executeDeploymentAsync(id);
            // 不等待，直接返回（任务已在后台执行）
            result.put("success", true);
            result.put("message", "正在发版，详细信息去发版记录查看");
            // 注意：由于是异步执行，recordId 可能还未生成
        } catch (Exception e) {
            log.error("Async deployment failed for service: {}", id, e);
            result.put("success", false);
            result.put("message", "发版失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 批量异步发版（后台执行，不打开 WebSocket 窗口）
     * 使用新的 BatchDeploymentService 实现真正的并发批量发版
     */
    @PostMapping("/batch-deploy")
    public Map<String, Object> batchDeployAsync(
            @RequestBody List<Integer> serviceIds,
            @RequestParam(required = false) Integer concurrency) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (serviceIds == null || serviceIds.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择要发版的服务");
                return result;
            }

            // 使用新的批量发版服务
            BatchDeploymentService.BatchDeploymentResult deploymentResult =
                    batchDeploymentService.submitBatchDeployment(serviceIds, concurrency);

            result.put("success", true);
            result.put("message", deploymentResult.getMessage());
            result.put("batchTaskId", deploymentResult.getBatchTaskId());
        } catch (Exception e) {
            log.error("Batch deployment failed", e);
            result.put("success", false);
            result.put("message", "批量发版失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 查询批量发版进度
     */
    @GetMapping("/batch-deploy/{batchTaskId}/progress")
    public Map<String, Object> getBatchDeployProgress(@PathVariable String batchTaskId) {
        Map<String, Object> result = new HashMap<>();
        try {
            BatchDeploymentService.BatchProgress progress = batchDeploymentService.getBatchProgress(batchTaskId);
            if (progress == null) {
                result.put("success", false);
                result.put("message", "批量任务不存在: " + batchTaskId);
                return result;
            }

            result.put("success", true);
            result.put("data", progress);
        } catch (Exception e) {
            log.error("Get batch deployment progress failed: {}", batchTaskId, e);
            result.put("success", false);
            result.put("message", "查询进度失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 取消批量发版任务
     */
    @PostMapping("/batch-deploy/{batchTaskId}/cancel")
    public Map<String, Object> cancelBatchDeploy(@PathVariable String batchTaskId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean cancelled = batchDeploymentService.cancelBatchDeployment(batchTaskId);
            result.put("success", cancelled);
            result.put("message", cancelled ? "批量任务已取消" : "取消失败，任务可能已结束或不存在");
        } catch (Exception e) {
            log.error("Cancel batch deployment failed: {}", batchTaskId, e);
            result.put("success", false);
            result.put("message", "取消失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 异步回退到指定版本
     */
    @PostMapping("/deployment/{recordId}/rollback-async")
    public Map<String, Object> rollbackToVersionAsync(@PathVariable Integer recordId) {
        Map<String, Object> result = new HashMap<>();
        try {
            CompletableFuture<Integer> future = deploymentOrchestrationService.rollbackToSpecificVersionAsync(recordId);
            // 不等待，直接返回（任务已在后台执行）
            result.put("success", true);
            result.put("message", "正在回退，详细信息去发版记录查看");
            // 注意：由于是异步执行，newRecordId 可能还未生成
        } catch (Exception e) {
            log.error("Async rollback failed for record: {}", recordId, e);
            result.put("success", false);
            result.put("message", "回退失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 上传启动脚本到服务器
     */
    @PostMapping("/{id}/upload-start-script")
    public Map<String, Object> uploadStartScript(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String scriptContent = request.get("scriptContent");
            appServiceService.uploadStartScript(id, scriptContent);
            result.put("success", true);
            result.put("message", "脚本上传成功");
        } catch (Exception e) {
            log.error("Upload start script failed for service: {}", id, e);
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }
        return result;
    }
}
