package com.opster.module.deployment.controller;

import com.opster.common.enums.DeploymentStatus;
import com.opster.module.deployment.entity.DeploymentRecord;
import com.opster.module.deployment.service.DeploymentRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部署记录控制器
 */
@RestController
@RequestMapping("/api/deployment-records")
public class DeploymentRecordController {

    @Autowired
    private DeploymentRecordService deploymentRecordService;

    /**
     * 查询部署记录
     * @param projectName 项目名称（模糊搜索）
     * @param status 部署状态
     * @return 部署记录列表
     */
    @GetMapping
    public List<DeploymentRecord> query(
            @RequestParam(value = "projectName", required = false) String projectName,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        DeploymentStatus deploymentStatus = status != null ? DeploymentStatus.getByCode(status) : null;
        return deploymentRecordService.query(projectName, deploymentStatus);
    }

    /**
     * 获取部署日志内容
     * @param id 部署记录ID
     * @return 部署日志内容
     */
    @GetMapping("/{id}/logs")
    public String getLogs(@PathVariable Integer id) {
        return deploymentRecordService.getLogContent(id);
    }

    /**
     * 获取部署记录详情
     * @param id 部署记录ID
     * @return 部署记录
     */
    @GetMapping("/{id}")
    public DeploymentRecord getById(@PathVariable Integer id) {
        return deploymentRecordService.getById(id);
    }

    /**
     * 获取服务的版本历史
     * @param serviceId 服务ID
     * @return 版本历史列表
     */
    @GetMapping("/service/{serviceId}/versions")
    public List<DeploymentRecord> getVersionHistory(@PathVariable Integer serviceId) {
        return deploymentRecordService.getVersionHistory(serviceId);
    }

    /**
     * 更新版本描述和标签
     * @param id 部署记录ID
     * @param request 包含description和tag的请求体
     */
    @PutMapping("/{id}/version-info")
    public void updateVersionInfo(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request
    ) {
        deploymentRecordService.updateVersionInfo(
                id,
                request.get("description"),
                request.get("tag")
        );
    }
}
