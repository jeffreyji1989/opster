package com.opster.module.service.repository;

import com.opster.module.service.entity.ServiceStartScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 服务启动脚本 Repository
 */
@Repository
public interface ServiceStartScriptRepository extends JpaRepository<ServiceStartScript, Integer> {

    /**
     * 查询所有未删除的脚本
     */
    List<ServiceStartScript> findByDelFlagOrderByIsDefaultDescIdDesc(Integer delFlag);

    /**
     * 查询默认脚本
     */
    ServiceStartScript findByIsDefaultAndDelFlag(Integer isDefault, Integer delFlag);

    /**
     * 根据名称查询脚本
     */
    ServiceStartScript findByNameAndDelFlag(String name, Integer delFlag);
}
