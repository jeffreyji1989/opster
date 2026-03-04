package com.opster.module.deployment.service;

import com.opster.common.enums.DeploymentStatus;
import com.opster.module.deployment.entity.DeploymentRecord;

import java.util.List;

/**
 * 部署记录服务
 */
public interface DeploymentRecordService {

    /**
     * 创建部署记录
     * @param record 部署记录
     * @return 创建的部署记录
     */
    DeploymentRecord create(DeploymentRecord record);

    /**
     * 更新部署记录
     * @param record 部署记录
     * @return 更新后的部署记录
     */
    DeploymentRecord update(DeploymentRecord record);

    /**
     * 根据ID获取部署记录
     * @param id 部署记录ID
     * @return 部署记录
     */
    DeploymentRecord getById(Integer id);

    /**
     * 查询部署记录
     * @param projectName 项目名称（模糊搜索）
     * @param status 部署状态
     * @return 部署记录列表
     */
    List<DeploymentRecord> query(String projectName, DeploymentStatus status);

    /**
     * 根据条件查询部署记录
     * @param projectName 项目名称（模糊搜索）
     * @param serviceName 服务名称（模糊搜索）
     * @param serverIp 服务器IP（模糊搜索）
     * @param status 部署状态
     * @return 部署记录列表
     */
    List<DeploymentRecord> queryByConditions(String projectName, String serviceName, String serverIp, DeploymentStatus status);

    /**
     * 获取所有部署记录（按创建时间倒序）
     * @return 部署记录列表
     */
    List<DeploymentRecord> getAll();

    /**
     * 获取部署日志路径
     * @param id 部署记录ID
     * @return 部署日志路径
     */
    String getLogPath(Integer id);

    /**
     * 获取部署日志内容
     * @param id 部署记录ID
     * @return 部署日志内容
     */
    String getLogContent(Integer id);

    /**
     * 获取服务的版本历史列表（排除回退记录）
     * @param serviceId 服务ID
     * @return 版本历史列表
     */
    List<DeploymentRecord> getVersionHistory(Integer serviceId);

    /**
     * 获取服务的所有发版记录（包括回退记录），按发版时间倒序
     * @param serviceId 服务ID
     * @return 发版记录列表
     */
    List<DeploymentRecord> getServiceRecords(Integer serviceId);

    /**
     * 更新版本描述和标签
     * @param id 部署记录ID
     * @param description 版本描述
     * @param tag 版本标签
     */
    void updateVersionInfo(Integer id, String description, String tag);

    /**
     * 批量删除部署记录
     * @param ids 部署记录ID列表
     */
    void deleteByIds(List<Integer> ids);
}
