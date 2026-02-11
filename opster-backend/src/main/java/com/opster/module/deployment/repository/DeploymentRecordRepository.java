package com.opster.module.deployment.repository;

import com.opster.common.enums.DeploymentStatus;
import com.opster.module.deployment.entity.DeploymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 部署记录仓库
 */
public interface DeploymentRecordRepository extends JpaRepository<DeploymentRecord, Integer> {

    /**
     * 根据项目名称模糊搜索和状态过滤，按创建时间倒序排列
     * @param projectName 项目名称（模糊搜索）
     * @param status 部署状态
     * @return 部署记录列表
     */
    @Query("SELECT dr FROM DeploymentRecord dr WHERE (:projectName IS NULL OR dr.projectName LIKE %:projectName%) AND (:status IS NULL OR dr.status = :status) ORDER BY dr.createTime DESC")
    List<DeploymentRecord> findByProjectNameAndStatus(
            @Param("projectName") String projectName,
            @Param("status") DeploymentStatus status
    );

    /**
     * 按创建时间倒序排列所有部署记录
     * @return 部署记录列表
     */
    List<DeploymentRecord> findAllByOrderByCreateTimeDesc();

    /**
     * 根据服务ID查询版本历史（排除回退记录）
     * @param serviceId 服务ID
     * @param isRollback 是否为回退记录
     * @return 部署记录列表
     */
    List<DeploymentRecord> findByServiceIdAndIsRollbackOrderByCreateTimeDesc(
            Integer serviceId,
            Boolean isRollback
    );

    /**
     * 根据服务ID查询所有发版记录（包括回退记录），按创建时间倒序
     * @param serviceId 服务ID
     * @return 部署记录列表
     */
    List<DeploymentRecord> findByServiceIdOrderByCreateTimeDesc(Integer serviceId);
}
