package com.opster.module.service.controller;

import com.opster.module.service.dto.ConfigFileDTO;
import com.opster.module.service.entity.ConfigFileVersion;
import com.opster.module.service.service.ServiceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置文件管理控制器
 */
@RestController
@RequestMapping("/api/service")
public class ServiceConfigController {

    @Autowired
    private ServiceConfigService serviceConfigService;

    /**
     * 获取服务配置文件列表
     */
    @GetMapping("/{serviceId}/config-files")
    public List<ConfigFileDTO> getConfigFiles(@PathVariable Integer serviceId) {
        return serviceConfigService.getConfigFiles(serviceId);
    }

    /**
     * 保存配置文件
     */
    @PostMapping("/{serviceId}/config-files")
    public Map<String, Object> saveConfigFile(
            @PathVariable Integer serviceId,
            @RequestBody ConfigFileDTO configFile) {
        Map<String, Object> result = new HashMap<>();
        try {
            ConfigFileDTO saved = serviceConfigService.saveConfigFile(serviceId, configFile);
            result.put("success", true);
            result.put("data", saved);
            result.put("message", "配置文件保存成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "配置文件保存失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 删除配置文件
     */
    @DeleteMapping("/{serviceId}/config-files/{configId}")
    public Map<String, Object> deleteConfigFile(
            @PathVariable Integer serviceId,
            @PathVariable Integer configId) {
        Map<String, Object> result = new HashMap<>();
        try {
            serviceConfigService.deleteConfigFile(serviceId, configId);
            result.put("success", true);
            result.put("message", "配置文件删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "配置文件删除失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 获取配置文件版本历史
     */
    @GetMapping("/{serviceId}/config-files/{filename}/versions")
    public List<ConfigFileVersion> getConfigFileVersions(
            @PathVariable Integer serviceId,
            @PathVariable String filename) {
        return serviceConfigService.getConfigFileVersions(serviceId, filename);
    }

    /**
     * 回退到历史版本
     */
    @PostMapping("/{serviceId}/config-versions/{versionId}/rollback")
    public Map<String, Object> rollbackToVersion(
            @PathVariable Integer serviceId,
            @PathVariable Integer versionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ConfigFileVersion version = serviceConfigService.rollbackToVersion(serviceId, versionId);
            result.put("success", true);
            result.put("data", version);
            result.put("message", "版本回退成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "版本回退失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 从 Git 同步配置文件
     */
    @PostMapping("/{serviceId}/config-files/sync-git")
    public Map<String, Object> syncFromGit(@PathVariable Integer serviceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ConfigFileDTO> configs = serviceConfigService.syncFromGit(serviceId);
            result.put("success", true);
            result.put("data", configs);
            result.put("message", "Git 同步完成");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Git 同步失败：" + e.getMessage());
        }
        return result;
    }
}
