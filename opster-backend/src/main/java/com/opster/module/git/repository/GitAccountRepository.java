package com.opster.module.git.repository;

import com.opster.common.enums.Status;
import com.opster.module.git.entity.GitAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Git 账号 Repository 接口
 */
@Repository
public interface GitAccountRepository extends JpaRepository<GitAccount, Integer>, JpaSpecificationExecutor<GitAccount> {

    /**
     * 根据账号名称和状态查询
     */
    List<GitAccount> findByAccountNameContainingAndStatus(String accountName, Status status);

    /**
     * 根据平台和状态查询
     */
    List<GitAccount> findByGitPlatformAndStatus(String gitPlatform, Status status);

    /**
     * 根据账号名称、平台和状态查询（支持模糊搜索）
     */
    @Query("SELECT g FROM GitAccount g WHERE " +
           "(:accountName IS NULL OR g.accountName LIKE %:accountName%) AND " +
           "(:gitPlatform IS NULL OR g.gitPlatform = :gitPlatform) AND " +
           "g.status = :status")
    List<GitAccount> findByConditions(
        @Param("accountName") String accountName,
        @Param("gitPlatform") String gitPlatform,
        @Param("status") Status status
    );

    /**
     * 获取所有启用的账号
     */
    List<GitAccount> findByStatus(Status status);

    /**
     * 检查账号名称是否已存在
     */
    boolean existsByAccountName(String accountName);
}
