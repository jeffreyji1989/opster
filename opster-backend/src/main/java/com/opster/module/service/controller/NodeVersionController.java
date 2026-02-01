package com.opster.module.service.controller;

import com.opster.module.service.service.NodeVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node.js 版本管理 API 控制器
 * 提供 Node.js 版本的查询、安装和验证功能
 */
@RestController
@RequestMapping("/api/node-version")
@Slf4j
public class NodeVersionController {

    @Autowired
    private NodeVersionService nodeVersionService;

    /**
     * 获取已安装的 Node.js 版本列表
     *
     * @return 版本列表
     */
    @GetMapping("/installed")
    public Map<String, Object> getInstalledVersions() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> versions = nodeVersionService.listInstalledVersions();
            result.put("success", true);
            result.put("data", versions);
            log.info("获取已安装的 Node.js 版本列表成功，共 {} 个版本", versions.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取版本列表失败: " + e.getMessage());
            log.error("获取已安装的 Node.js 版本列表失败", e);
        }
        return result;
    }

    /**
     * 安装指定的 Node.js 版本
     *
     * @param version 版本号，格式：v18.17.0
     * @return 安装结果
     */
    @PostMapping("/install/{version}")
    public Map<String, Object> installVersion(@PathVariable String version) {
        Map<String, Object> result = new HashMap<>();
        log.info("开始安装 Node.js 版本: {}", version);

        try {
            // 验证版本格式
            if (!nodeVersionService.isValidVersionFormat(version)) {
                result.put("success", false);
                result.put("message", "无效的版本号格式: " + version + "，正确格式应为：v18.17.0");
                return result;
            }

            // 检查版本是否已安装
            if (nodeVersionService.isVersionInstalled(version)) {
                result.put("success", true);
                result.put("message", "版本 " + version + " 已经安装，无需重复安装");
                log.info("版本 {} 已经安装", version);
                return result;
            }

            // 安装版本
            boolean installed = nodeVersionService.installVersion(version);
            if (installed) {
                result.put("success", true);
                result.put("message", "版本 " + version + " 安装成功");
                log.info("版本 {} 安装成功", version);
            } else {
                result.put("success", false);
                result.put("message", "版本 " + version + " 安装失败");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "安装失败: " + e.getMessage());
            log.error("安装 Node.js 版本 {} 失败", version, e);
        }

        return result;
    }

    /**
     * 验证版本号格式是否有效
     *
     * @param version 版本号
     * @return 验证结果
     */
    @GetMapping("/validate/{version}")
    public Map<String, Object> validateVersion(@PathVariable String version) {
        Map<String, Object> result = new HashMap<>();
        boolean valid = nodeVersionService.isValidVersionFormat(version);
        result.put("success", true);
        result.put("valid", valid);
        result.put("message", valid ? "版本号格式有效" : "版本号格式无效，正确格式应为：v18.17.0");
        return result;
    }

    /**
     * 检查指定版本是否已安装
     *
     * @param version 版本号
     * @return 检查结果
     */
    @GetMapping("/check/{version}")
    public Map<String, Object> checkVersionInstalled(@PathVariable String version) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean installed = nodeVersionService.isVersionInstalled(version);
            result.put("success", true);
            result.put("installed", installed);
            result.put("message", installed ? "版本 " + version + " 已安装" : "版本 " + version + " 未安装");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查版本失败: " + e.getMessage());
            log.error("检查 Node.js 版本 {} 是否安装失败", version, e);
        }
        return result;
    }
}
