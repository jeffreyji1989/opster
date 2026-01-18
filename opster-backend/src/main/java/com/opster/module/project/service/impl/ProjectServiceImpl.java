package com.opster.module.project.service.impl;

import com.opster.common.enums.Status;
import com.opster.module.project.entity.Project;
import com.opster.module.project.repository.ProjectRepository;
import com.opster.module.project.service.ProjectService;
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

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    @Override
    public List<Project> findList(String projectName, String businessLine, Integer status) {
        Specification<Project> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (projectName != null && !projectName.isEmpty()) {
                predicates.add(cb.like(root.get("projectName"), "%" + projectName + "%"));
            }
            
            if (businessLine != null && !businessLine.isEmpty()) {
                predicates.add(cb.like(root.get("businessLine"), "%" + businessLine + "%"));
            }
            
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), Status.fromCode(status)));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return projectRepository.findAll(spec);
    }

    @Override
    public Page<Project> findPage(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    @Override
    public Optional<Project> findById(Integer id) {
        return projectRepository.findById(id);
    }

    @Override
    public Project save(Project project) {
        return projectRepository.save(project);
    }

    @Override
    public void deleteById(Integer id) {
        projectRepository.deleteById(id);
    }

    @Override
    public long count() {
        return projectRepository.count();
    }
}
