package com.opster.module.server.controller;

import com.opster.module.server.entity.Server;
import com.opster.module.server.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 服务器管理控制器
 */
@RestController
@RequestMapping("/api/server")
public class ServerController {

    @Autowired
    private ServerService serverService;

    /**
     * 获取所有服务器
     */
    @GetMapping("/list")
    public List<Server> list() {
        return serverService.findAll();
    }

    /**
     * 分页查询服务器
     */
    @GetMapping("/page")
    public Page<Server> page(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size) {
        return serverService.findPage(PageRequest.of(page - 1, size));
    }

    /**
     * 根据ID获取服务器
     */
    @GetMapping("/{id}")
    public Server getById(@PathVariable Integer id) {
        return serverService.findById(id).orElse(null);
    }

    /**
     * 新增服务器
     */
    @PostMapping
    public boolean save(@RequestBody Server server) {
        serverService.save(server);
        return true;
    }

    /**
     * 修改服务器
     */
    @PutMapping
    public boolean update(@RequestBody Server server) {
        serverService.save(server);
        return true;
    }

    /**
     * 删除服务器
     */
    @DeleteMapping("/{id}")
    public boolean remove(@PathVariable Integer id) {
        serverService.deleteById(id);
        return true;
    }
}
