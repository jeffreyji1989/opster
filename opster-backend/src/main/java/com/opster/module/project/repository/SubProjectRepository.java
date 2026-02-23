package com.opster.module.project.repository;

import com.opster.common.enums.Status;
import com.opster.module.project.entity.SubProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 子项目 Repository 接口
 */
@Repository
public interface SubProjectRepository extends JpaRepository<SubProject, Integer>, JpaSpecificationExecutor<SubProject> {

    /**
     * 根据项目 ID 和状态查询子项目列表
     */
    List<SubProject> findByProjectIdAndStatus(Integer projectId, Status status);

    /**
     * 根据服务器 ID 和状态查询子项目列表
     */
    List<SubProject> findByServerIdAndStatus(Integer serverId, Status status);

    /**
     * 根据项目类型和状态查询子项目列表
     */
    List<SubProject> findByProjectTypeAndStatus(String projectType, Status status);

    /**
     * 根据项目 ID 查询所有子项目（包括已禁用）
     */
    List<SubProject> findByProjectId(Integer projectId);

    /**
     * 根据项目 ID、服务器 ID、项目类型和状态查询（支持多条件组合）
     */
    @Query("SELECT s FROM SubProject s WHERE " +
           "(:projectId IS NULL OR s.projectId = :projectId) AND " +
           "(:serverId IS NULL OR s.serverId = :serverId) AND " +
           "(:projectType IS NULL OR s.projectType = :projectType) AND " +
           "(:status IS NULL OR s.status = :status)")
    List<SubProject> findByConditions(
        @Param("projectId") Integer projectId,
        @Param("serverId") Integer serverId,
        @Param("projectType") String projectType,
        @Param("status") Status status
    );

    /**
     * 统计项目下的子项目数量
     */
    long countByProjectIdAndStatus(Integer projectId, Status status);

    /**
     * 检查子项目名称是否已存在（同一项目下）
     */
    boolean existsByProjectIdAndSubProjectName(Integer projectId, String subProjectName);
}
