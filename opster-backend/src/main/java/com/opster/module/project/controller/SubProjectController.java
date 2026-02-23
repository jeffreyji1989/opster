package com.opster.module.project.controller;

import com.opster.module.project.entity.SubProject;
import com.opster.module.project.service.SubProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 子项目管理控制器
 */
@RestController
@RequestMapping("/api/sub-project")
public class SubProjectController {

    @Autowired
    private SubProjectService subProjectService;

    /**
     * 条件查询子项目列表
     *
     * @param projectId 项目 ID
     * @param serverId 服务器 ID
     * @param projectType 项目类型
     * @param status 状态
     * @return 子项目列表
     */
    @GetMapping("/list")
    public List<SubProject> list(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer serverId,
            @RequestParam(required = false) String projectType,
            @RequestParam(required = false) Integer status) {
        return subProjectService.findList(projectId, serverId, projectType, status);
    }

    /**
     * 分页查询子项目
     *
     * @param page 页码（从 1 开始）
     * @param size 每页大小
     * @return 分页结果
     */
    @GetMapping("/page")
    public Page<SubProject> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return subProjectService.findPage(PageRequest.of(page - 1, size));
    }

    /**
     * 根据 ID 查询子项目
     *
     * @param id 子项目 ID
     * @return 子项目信息
     */
    @GetMapping("/{id}")
    public SubProject getById(@PathVariable Integer id) {
        return subProjectService.findById(id).orElse(null);
    }

    /**
     * 根据项目 ID 查询子项目列表
     *
     * @param projectId 项目 ID
     * @return 子项目列表
     */
    @GetMapping("/project/{projectId}")
    public List<SubProject> getByProjectId(@PathVariable Integer projectId) {
        return subProjectService.findByProjectId(projectId);
    }

    /**
     * 新增子项目
     *
     * @param subProject 子项目信息
     * @return 是否成功
     */
    @PostMapping
    public boolean save(@RequestBody SubProject subProject) {
        subProjectService.save(subProject);
        return true;
    }

    /**
     * 修改子项目
     *
     * @param subProject 子项目信息
     * @return 是否成功
     */
    @PutMapping
    public boolean update(@RequestBody SubProject subProject) {
        subProjectService.save(subProject);
        return true;
    }

    /**
     * 删除子项目
     *
     * @param id 子项目 ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public boolean remove(@PathVariable Integer id) {
        subProjectService.deleteById(id);
        return true;
    }

    /**
     * 触发子项目部署
     *
     * @param id 子项目 ID
     * @return 部署结果
     */
    @PostMapping("/{id}/deploy")
    public Map<String, Object> deploy(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        boolean success = subProjectService.deploy(id);
        result.put("success", success);
        result.put("message", success ? "部署已触发" : "部署触发失败");
        return result;
    }

    /**
     * 获取子项目的配置文件
     *
     * @param id 子项目 ID
     * @return 配置文件 JSON 字符串
     */
    @GetMapping("/{id}/config-files")
    public Map<String, Object> getConfigFiles(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        String configFiles = subProjectService.getConfigFiles(id);
        result.put("configFiles", configFiles);
        return result;
    }

    /**
     * 保存子项目的配置文件
     *
     * @param id 子项目 ID
     * @param configFiles 配置文件 JSON 字符串
     * @return 是否成功
     */
    @PostMapping("/{id}/config-files")
    public Map<String, Object> saveConfigFiles(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String configFiles = request.get("configFiles");
        boolean success = subProjectService.saveConfigFiles(id, configFiles);
        result.put("success", success);
        result.put("message", success ? "配置文件保存成功" : "配置文件保存失败");
        return result;
    }

    /**
     * 统计项目下的子项目数量
     *
     * @param projectId 项目 ID
     * @return 统计信息
     */
    @GetMapping("/count/project/{projectId}")
    public Map<String, Object> countByProjectId(@PathVariable Integer projectId) {
        Map<String, Object> result = new HashMap<>();
        long count = subProjectService.countByProjectId(projectId);
        result.put("projectId", projectId);
        result.put("count", count);
        return result;
    }
}
