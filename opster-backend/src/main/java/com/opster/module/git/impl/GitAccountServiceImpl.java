package com.opster.module.git.impl;

import com.opster.common.enums.Status;
import com.opster.common.SecurityUtils;
import com.opster.module.git.entity.GitAccount;
import com.opster.module.git.repository.GitAccountRepository;
import com.opster.module.git.service.GitAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.criteria.Predicate;

/**
 * Git 账号 Service 实现类
 */
@Service
@Transactional
public class GitAccountServiceImpl implements GitAccountService {

    @Autowired
    private GitAccountRepository gitAccountRepository;

    /**
     * 支持的 Git 平台列表
     */
    private static final List<String> SUPPORTED_PLATFORMS = Arrays.asList("gitee", "gitlab", "github");

    @Override
    public List<GitAccount> findAll() {
        return gitAccountRepository.findAll();
    }

    @Override
    public List<GitAccount> findList(String accountName, String gitPlatform, Integer status) {
        Specification<GitAccount> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 账号名称模糊搜索
            if (accountName != null && !accountName.isEmpty()) {
                predicates.add(cb.like(root.get("accountName"), "%" + accountName + "%"));
            }

            // 平台精确匹配
            if (gitPlatform != null && !gitPlatform.isEmpty()) {
                predicates.add(cb.equal(root.get("gitPlatform"), gitPlatform));
            }

            // 状态精确匹配
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), Status.fromCode(status)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return gitAccountRepository.findAll(spec);
    }

    @Override
    public Page<GitAccount> findPage(Pageable pageable) {
        return gitAccountRepository.findAll(pageable);
    }

    @Override
    public Optional<GitAccount> findById(Integer id) {
        return gitAccountRepository.findById(id);
    }

    @Override
    public GitAccount save(GitAccount gitAccount) {
        // 加密 Git 密码（HTTPS 认证）
        if (gitAccount.getGitPassword() != null && !gitAccount.getGitPassword().isEmpty()) {
            // 如果不是加密过的，说明是明文，需要加密
            if (!SecurityUtils.isEncrypted(gitAccount.getGitPassword())) {
                gitAccount.setGitPassword(SecurityUtils.encrypt(gitAccount.getGitPassword()));
            }
        }

        // 加密 SSH 私钥密码（SSH 认证）
        if (gitAccount.getSshKeyPassphrase() != null && !gitAccount.getSshKeyPassphrase().isEmpty()) {
            // 如果不是加密过的，说明是明文，需要加密
            if (!SecurityUtils.isEncrypted(gitAccount.getSshKeyPassphrase())) {
                gitAccount.setSshKeyPassphrase(SecurityUtils.encrypt(gitAccount.getSshKeyPassphrase()));
            }
        }

        return gitAccountRepository.save(gitAccount);
    }

    @Override
    public void deleteById(Integer id) {
        gitAccountRepository.deleteById(id);
    }

    @Override
    public long count() {
        return gitAccountRepository.count();
    }

    @Override
    public List<String> getSupportedPlatforms() {
        return new ArrayList<>(SUPPORTED_PLATFORMS);
    }
}
