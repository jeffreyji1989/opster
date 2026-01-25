package com.opster.module.server.controller;

import com.opster.common.SecurityUtils;
import com.opster.module.server.entity.Server;
import com.opster.module.server.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<Server> list(
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String env,
            @RequestParam(required = false) Integer status) {
        return serverService.findList(ip, groupName, env, status);
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

    /**
     * 测试服务器密码（调试用）
     * 返回密码的加密状态信息
     */
    @GetMapping("/test-password/{id}")
    public Map<String, Object> testPassword(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        Server server = serverService.findById(id).orElse(null);

        if (server == null) {
            result.put("error", "服务器不存在");
            return result;
        }

        String originalPassword = server.getPassword();
        String decryptedPassword = SecurityUtils.decrypt(originalPassword);
        boolean isEncrypted = SecurityUtils.isEncrypted(originalPassword);

        result.put("serverId", server.getId());
        result.put("ip", server.getIp());
        result.put("username", server.getUsername());
        result.put("originalPasswordLength", originalPassword != null ? originalPassword.length() : 0);
        result.put("decryptedPasswordLength", decryptedPassword != null ? decryptedPassword.length() : 0);
        result.put("isEncrypted", isEncrypted);
        result.put("originalPasswordPreview", originalPassword != null ? originalPassword.substring(0, Math.min(10, originalPassword.length())) : null);
        result.put("decryptedPasswordPreview", decryptedPassword != null ? decryptedPassword.substring(0, Math.min(10, decryptedPassword.length())) : null);

        return result;
    }
}
