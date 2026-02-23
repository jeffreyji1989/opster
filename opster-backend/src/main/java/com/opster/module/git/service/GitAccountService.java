package com.opster.module.git.service;

import com.opster.module.git.entity.GitAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Git 账号 Service 接口
 */
public interface GitAccountService {

    /**
     * 查询所有 Git 账号
     */
    List<GitAccount> findAll();

    /**
     * 条件查询 Git 账号列表
     *
     * @param accountName 账号名称（支持模糊搜索）
     * @param gitPlatform Git 平台
     * @param status 状态
     * @return Git 账号列表
     */
    List<GitAccount> findList(String accountName, String gitPlatform, Integer status);

    /**
     * 分页查询 Git 账号
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<GitAccount> findPage(Pageable pageable);

    /**
     * 根据 ID 查询 Git 账号
     *
     * @param id Git 账号 ID
     * @return Git 账号信息
     */
    Optional<GitAccount> findById(Integer id);

    /**
     * 新增或更新 Git 账号
     * 保存时会自动加密密码字段
     *
     * @param gitAccount Git 账号信息
     * @return 保存后的 Git 账号信息
     */
    GitAccount save(GitAccount gitAccount);

    /**
     * 删除 Git 账号
     *
     * @param id Git 账号 ID
     */
    void deleteById(Integer id);

    /**
     * 统计 Git 账号数量
     *
     * @return Git 账号总数
     */
    long count();

    /**
     * 获取所有支持的 Git 平台列表
     *
     * @return 平台列表
     */
    List<String> getSupportedPlatforms();
}
