package com.opster.module.project.service;

import com.opster.module.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProjectService {
    List<Project> findAll();
    List<Project> findList(String projectName, String businessLine, Integer status);
    Page<Project> findPage(Pageable pageable);
    Optional<Project> findById(Integer id);
    Project save(Project project);
    void deleteById(Integer id);
    long count();
}
