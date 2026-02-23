package com.opster.module.git.controller;

import com.opster.module.git.entity.GitAccount;
import com.opster.module.git.service.GitAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Git 账号管理控制器
 */
@RestController
@RequestMapping("/api/git-account")
public class GitAccountController {

    @Autowired
    private GitAccountService gitAccountService;

    /**
     * 条件查询 Git 账号列表
     *
     * @param accountName 账号名称（支持模糊搜索）
     * @param gitPlatform Git 平台
     * @param status 状态
     * @return Git 账号列表
     */
    @GetMapping("/list")
    public List<GitAccount> list(
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String gitPlatform,
            @RequestParam(required = false) Integer status) {
        return gitAccountService.findList(accountName, gitPlatform, status);
    }

    /**
     * 分页查询 Git 账号
     *
     * @param page 页码（从 1 开始）
     * @param size 每页大小
     * @return 分页结果
     */
    @GetMapping("/page")
    public Page<GitAccount> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return gitAccountService.findPage(PageRequest.of(page - 1, size));
    }

    /**
     * 根据 ID 查询 Git 账号
     *
     * @param id Git 账号 ID
     * @return Git 账号信息
     */
    @GetMapping("/{id}")
    public GitAccount getById(@PathVariable Integer id) {
        return gitAccountService.findById(id).orElse(null);
    }

    /**
     * 新增 Git 账号
     * 保存时会自动加密密码字段
     *
     * @param gitAccount Git 账号信息
     * @return 是否成功
     */
    @PostMapping
    public boolean save(@RequestBody GitAccount gitAccount) {
        gitAccountService.save(gitAccount);
        return true;
    }

    /**
     * 修改 Git 账号
     * 保存时会自动加密密码字段
     *
     * @param gitAccount Git 账号信息
     * @return 是否成功
     */
    @PutMapping
    public boolean update(@RequestBody GitAccount gitAccount) {
        gitAccountService.save(gitAccount);
        return true;
    }

    /**
     * 删除 Git 账号
     *
     * @param id Git 账号 ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public boolean remove(@PathVariable Integer id) {
        gitAccountService.deleteById(id);
        return true;
    }

    /**
     * 获取所有支持的 Git 平台列表
     *
     * @return 平台列表
     */
    @GetMapping("/platforms")
    public Map<String, Object> getSupportedPlatforms() {
        Map<String, Object> result = new HashMap<>();
        result.put("platforms", gitAccountService.getSupportedPlatforms());
        return result;
    }

    /**
     * 获取 Git 账号统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", gitAccountService.count());
        result.put("platforms", gitAccountService.getSupportedPlatforms());
        return result;
    }
}
