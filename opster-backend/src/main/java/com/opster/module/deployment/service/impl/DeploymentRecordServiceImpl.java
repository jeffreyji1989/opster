package com.opster.module.deployment.service.impl;

import com.opster.common.enums.DeploymentStatus;
import com.opster.module.deployment.entity.DeploymentRecord;
import com.opster.module.deployment.repository.DeploymentRecordRepository;
import com.opster.module.deployment.service.DeploymentRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部署记录服务实现
 */
@Service
public class DeploymentRecordServiceImpl implements DeploymentRecordService {

    @Autowired
    private DeploymentRecordRepository deploymentRecordRepository;

    @Override
    public DeploymentRecord create(DeploymentRecord record) {
        return deploymentRecordRepository.save(record);
    }

    @Override
    public DeploymentRecord update(DeploymentRecord record) {
        return deploymentRecordRepository.save(record);
    }

    @Override
    public DeploymentRecord getById(Integer id) {
        return deploymentRecordRepository.findById(id).orElse(null);
    }

    @Override
    public List<DeploymentRecord> query(String projectName, DeploymentStatus status) {
        return deploymentRecordRepository.findByProjectNameAndStatus(projectName, status);
    }

    @Override
    public List<DeploymentRecord> getAll() {
        return deploymentRecordRepository.findAllByOrderByCreateTimeDesc();
    }

    @Override
    public String getLogPath(Integer id) {
        DeploymentRecord record = deploymentRecordRepository.findById(id).orElse(null);
        return record != null ? record.getLogPath() : "";
    }

    @Override
    public String getLogContent(Integer id) {
        DeploymentRecord record = deploymentRecordRepository.findById(id).orElse(null);
        if (record == null || record.getLogPath() == null) {
            return "";
        }
        
        java.io.File logFile = new java.io.File(record.getLogPath());
        if (!logFile.exists()) {
            return "日志文件不存在: " + record.getLogPath();
        }
        
        StringBuilder content = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (java.io.IOException e) {
            return "读取日志文件失败: " + e.getMessage();
        }
        
        return content.toString();
    }
}
