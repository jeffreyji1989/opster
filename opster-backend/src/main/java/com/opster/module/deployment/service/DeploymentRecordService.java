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
}
