package com.opster.module.service.repository;

import com.opster.module.service.entity.ServiceStartScriptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 服务启动脚本版本 Repository
 */
@Repository
public interface ServiceStartScriptVersionRepository extends JpaRepository<ServiceStartScriptVersion, Integer> {

    /**
     * 查询指定脚本的所有版本
     */
    List<ServiceStartScriptVersion> findByScriptIdOrderByVersionNoDesc(Integer scriptId);

    /**
     * 查询指定脚本的激活版本
     */
    ServiceStartScriptVersion findByScriptIdAndIsActive(Integer scriptId, Integer isActive);

    /**
     * 查询指定脚本的所有激活版本
     */
    List<ServiceStartScriptVersion> findByScriptIdAndIsActiveOrderByVersionNoDesc(Integer scriptId, Integer isActive);

    /**
     * 取消脚本的所有激活版本
     */
    @Modifying
    @Query("UPDATE ServiceStartScriptVersion SET isActive = 0 WHERE scriptId = ?1 AND isActive = 1")
    void deactivateAllByScriptId(Integer scriptId);
}
