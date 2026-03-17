package com.opster.module.service.repository;

import com.opster.module.service.entity.ConfigFileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 配置文件版本 Repository
 */
@Repository
public interface ConfigFileVersionRepository extends JpaRepository<ConfigFileVersion, Integer> {

    /**
     * 根据子项目 ID 查询配置文件版本列表
     */
    List<ConfigFileVersion> findBySubProjectIdOrderByCreateTimeDesc(@Param("subProjectId") Integer subProjectId);

    /**
     * 根据子项目 ID 和文件名查询配置文件版本列表
     */
    List<ConfigFileVersion> findBySubProjectIdAndFilenameOrderByCreateTimeDesc(
        @Param("subProjectId") Integer subProjectId,
        @Param("filename") String filename
    );

    /**
     * 根据子项目 ID 查询最新的配置文件版本（每个文件名一个）
     */
    @Query(value = """
        SELECT * FROM config_file_version cv1
        WHERE cv1.sub_project_id = :subProjectId
        AND cv1.create_time = (
            SELECT MAX(cv2.create_time)
            FROM config_file_version cv2
            WHERE cv2.sub_project_id = cv1.sub_project_id
            AND cv2.filename = cv1.filename
        )
        ORDER BY cv1.filename
        """, nativeQuery = true)
    List<ConfigFileVersion> findLatestVersionsBySubProjectId(@Param("subProjectId") Integer subProjectId);

    /**
     * 根据子项目 ID 和文件名查询最新的配置文件版本
     */
    ConfigFileVersion findFirstBySubProjectIdAndFilenameOrderByCreateTimeDesc(
        @Param("subProjectId") Integer subProjectId,
        @Param("filename") String filename
    );

    /**
     * 查询已部署的配置文件版本
     */
    List<ConfigFileVersion> findBySubProjectIdAndIsDeployedTrue(
        @Param("subProjectId") Integer subProjectId
    );
}
