package com.opster.module.project.service;

import com.opster.module.project.entity.SubProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 子项目 Service 接口
 */
public interface SubProjectService {

    /**
     * 查询所有子项目
     */
    List<SubProject> findAll();

    /**
     * 条件查询子项目列表
     *
     * @param projectId 项目 ID
     * @param serverId 服务器 ID
     * @param projectType 项目类型
     * @param status 状态
     * @return 子项目列表
     */
    List<SubProject> findList(Integer projectId, Integer serverId, String projectType, Integer status);

    /**
     * 根据项目 ID 查询子项目列表
     *
     * @param projectId 项目 ID
     * @return 子项目列表
     */
    List<SubProject> findByProjectId(Integer projectId);

    /**
     * 分页查询子项目
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SubProject> findPage(Pageable pageable);

    /**
     * 根据 ID 查询子项目
     *
     * @param id 子项目 ID
     * @return 子项目信息
     */
    Optional<SubProject> findById(Integer id);

    /**
     * 新增或更新子项目
     *
     * @param subProject 子项目信息
     * @return 保存后的子项目信息
     */
    SubProject save(SubProject subProject);

    /**
     * 删除子项目
     *
     * @param id 子项目 ID
     */
    void deleteById(Integer id);

    /**
     * 统计子项目数量
     *
     * @return 子项目总数
     */
    long count();

    /**
     * 统计项目下的子项目数量
     *
     * @param projectId 项目 ID
     * @return 子项目数量
     */
    long countByProjectId(Integer projectId);

    /**
     * 触发子项目部署
     *
     * @param id 子项目 ID
     * @return 是否成功
     */
    boolean deploy(Integer id);

    /**
     * 获取子项目的配置文件
     *
     * @param id 子项目 ID
     * @return 配置文件 JSON 字符串
     */
    String getConfigFiles(Integer id);

    /**
     * 保存子项目的配置文件
     *
     * @param id 子项目 ID
     * @param configFiles 配置文件 JSON 字符串
     * @return 是否成功
     */
    boolean saveConfigFiles(Integer id, String configFiles);
}
