package com.opster.module.project.controller;

import com.opster.module.project.entity.Project;
import com.opster.module.project.service.ProjectService;
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

    /**
     * 获取所有项目
     */
    @GetMapping("/list")
    public List<Project> list() {
        return projectService.findAll();
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
    public boolean save(@RequestBody Project project) {
        projectService.save(project);
        return true;
    }

    /**
     * 修改项目
     */
    @PutMapping
    public boolean update(@RequestBody Project project) {
        projectService.save(project);
        return true;
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    public boolean remove(@PathVariable Integer id) {
        projectService.deleteById(id);
        return true;
    }
}
