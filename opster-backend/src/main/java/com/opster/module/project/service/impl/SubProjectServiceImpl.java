package com.opster.module.project.service.impl;

import cn.hutool.core.util.StrUtil;
import com.opster.common.enums.Status;
import com.opster.module.project.entity.SubProject;
import com.opster.module.project.repository.SubProjectRepository;
import com.opster.module.project.service.SubProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.criteria.Predicate;

/**
 * 子项目 Service 实现类
 */
@Service
@Transactional
public class SubProjectServiceImpl implements SubProjectService {

    @Autowired
    private SubProjectRepository subProjectRepository;

    @Override
    public List<SubProject> findAll() {
        return subProjectRepository.findAll();
    }

    @Override
    public List<SubProject> findList(Integer projectId, Integer serverId, String projectType, Integer status) {
        Specification<SubProject> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 项目 ID 精确匹配
            if (projectId != null) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }

            // 服务器 ID 精确匹配
            if (serverId != null) {
                predicates.add(cb.equal(root.get("serverId"), serverId));
            }

            // 项目类型精确匹配
            if (projectType != null && !projectType.isEmpty()) {
                predicates.add(cb.equal(root.get("projectType"), projectType));
            }

            // 状态精确匹配
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), Status.fromCode(status)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return subProjectRepository.findAll(spec);
    }

    @Override
    public List<SubProject> findByProjectId(Integer projectId) {
        return subProjectRepository.findByProjectId(projectId);
    }

    @Override
    public Page<SubProject> findPage(Pageable pageable) {
        return subProjectRepository.findAll(pageable);
    }

    @Override
    public Optional<SubProject> findById(Integer id) {
        return subProjectRepository.findById(id);
    }

    @Override
    public SubProject save(SubProject subProject) {
        // 验证必填字段
        if (subProject.getProjectId() == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        if (StrUtil.isBlank(subProject.getSubProjectName())) {
            throw new IllegalArgumentException("子项目名称不能为空");
        }
        if (StrUtil.isBlank(subProject.getGitUrl())) {
            throw new IllegalArgumentException("Git仓库地址不能为空");
        }

        return subProjectRepository.save(subProject);
    }

    @Override
    public void deleteById(Integer id) {
        subProjectRepository.deleteById(id);
    }

    @Override
    public long count() {
        return subProjectRepository.count();
    }

    @Override
    public long countByProjectId(Integer projectId) {
        return subProjectRepository.countByProjectIdAndStatus(projectId, Status.ENABLED);
    }

    @Override
    public boolean deploy(Integer id) {
        // TODO: 实现部署逻辑
        // 1. 查询子项目配置
        // 2. 创建/获取 AppService 实例
        // 3. 调用 DeploymentOrchestrationService.executeDeployment(serviceId)
        // 4. 返回部署结果
        return true;
    }

    @Override
    public String getConfigFiles(Integer id) {
        Optional<SubProject> subProjectOpt = subProjectRepository.findById(id);
        if (subProjectOpt.isPresent()) {
            return subProjectOpt.get().getConfigFiles();
        }
        return null;
    }

    @Override
    public boolean saveConfigFiles(Integer id, String configFiles) {
        Optional<SubProject> subProjectOpt = subProjectRepository.findById(id);
        if (subProjectOpt.isPresent()) {
            SubProject subProject = subProjectOpt.get();
            subProject.setConfigFiles(configFiles);
            subProjectRepository.save(subProject);
            return true;
        }
        return false;
    }
}
