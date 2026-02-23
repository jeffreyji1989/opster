package com.opster.module.project.controller;

import com.opster.module.project.entity.Project;
import com.opster.module.project.service.ProjectService;
import com.opster.module.project.service.SubProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目管理控制器
 */
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SubProjectService subProjectService;

    /**
     * 获取所有项目
     */
    @GetMapping("/list")
    public List<Project> list(
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String businessLine,
            @RequestParam(required = false) Integer status) {
        return projectService.findList(projectName, businessLine, status);
    }

    /**
     * 分页查询项目
     */
    @GetMapping("/page")
    public Page<Project> page(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size) {
        // JPA Page starts from 0
        return projectService.findPage(PageRequest.of(page - 1, size));
    }

    /**
     * 根据ID获取项目
     */
    @GetMapping("/{id}")
    public Project getById(@PathVariable Integer id) {
        return projectService.findById(id).orElse(null);
    }

    /**
     * 新增项目
     */
    @PostMapping
    public Project save(@RequestBody Project project) {
        return projectService.save(project);
    }

    /**
     * 修改项目
     */
    @PutMapping
    public Project update(@RequestBody Project project) {
        return projectService.save(project);
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    public void remove(@PathVariable Integer id) {
        projectService.deleteById(id);
    }

    /**
     * 获取项目的所有子项目
     * 注：新增接口，用于获取项目下的子项目列表
     */
    @GetMapping("/{id}/sub-projects")
    public List<?> getSubProjects(@PathVariable Integer id) {
        return subProjectService.findByProjectId(id);
    }
}
