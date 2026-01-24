package com.opster.module.schedule.controller;

import com.opster.module.schedule.entity.ScheduledDeployment;
import com.opster.module.schedule.service.ScheduledDeploymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时发版任务控制器
 */
@RestController
@RequestMapping("/api/scheduled-deployments")
public class ScheduledDeploymentController {

    @Autowired
    private ScheduledDeploymentService scheduledDeploymentService;

    /**
     * 查询所有定时发版任务
     * @return 任务列表
     */
    @GetMapping
    public List<ScheduledDeployment> list() {
        return scheduledDeploymentService.getAll();
    }

    /**
     * 根据ID获取任务
     * @param id 任务ID
     * @return 定时发版任务
     */
    @GetMapping("/{id}")
    public ScheduledDeployment getById(@PathVariable Integer id) {
        return scheduledDeploymentService.getById(id);
    }

    /**
     * 创建定时发版任务
     * @param task 定时发版任务
     * @return 创建的任务
     */
    @PostMapping
    public ScheduledDeployment create(@RequestBody ScheduledDeployment task) {
        return scheduledDeploymentService.create(task);
    }

    /**
     * 更新定时发版任务
     * @param task 定时发版任务
     * @return 更新后的任务
     */
    @PutMapping
    public ScheduledDeployment update(@RequestBody ScheduledDeployment task) {
        return scheduledDeploymentService.update(task);
    }

    /**
     * 删除定时发版任务
     * @param id 任务ID
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        scheduledDeploymentService.deleteById(id);
    }

    /**
     * 取消定时发版任务
     * @param id 任务ID
     */
    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable Integer id) {
        scheduledDeploymentService.cancelById(id);
    }
}
