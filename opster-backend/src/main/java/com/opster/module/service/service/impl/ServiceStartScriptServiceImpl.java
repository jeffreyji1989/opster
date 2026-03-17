package com.opster.module.service.service.impl;

import com.opster.module.service.dto.StartScriptDTO;
import com.opster.module.service.dto.StartScriptVersionDTO;
import com.opster.module.service.entity.ServiceStartScript;
import com.opster.module.service.entity.ServiceStartScriptVersion;
import com.opster.module.service.repository.ServiceStartScriptRepository;
import com.opster.module.service.repository.ServiceStartScriptVersionRepository;
import com.opster.module.service.service.ScriptTemplateBuilder;
import com.opster.module.service.service.ServiceStartScriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 启动脚本管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceStartScriptServiceImpl implements ServiceStartScriptService {

    private final ServiceStartScriptRepository scriptRepository;
    private final ServiceStartScriptVersionRepository versionRepository;
    private final ScriptTemplateBuilder scriptTemplateBuilder;

    @Override
    public List<StartScriptDTO> listScripts() {
        return scriptRepository.findByDelFlagOrderByIsDefaultDescIdDesc(0)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public StartScriptDTO getScriptDetail(Integer id) {
        ServiceStartScript script = scriptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("脚本不存在：" + id));

        StartScriptDTO dto = convertToDTO(script);

        // 加载当前版本的脚本内容
        if (script.getCurrentVersionId() != null) {
            ServiceStartScriptVersion version = versionRepository.findById(script.getCurrentVersionId())
                    .orElse(null);
            if (version != null) {
                dto.setScriptContent(version.getScriptContent());
            }
        }

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartScriptDTO createScript(StartScriptDTO dto) {
        // 检查名称是否重复
        ServiceStartScript existing = scriptRepository.findByNameAndDelFlag(dto.getName(), 0);
        if (existing != null) {
            throw new RuntimeException("脚本名称已存在：" + dto.getName());
        }

        // 创建主表记录
        ServiceStartScript script = new ServiceStartScript();
        script.setName(dto.getName());
        script.setDescription(dto.getDescription());
        script.setPort(dto.getPort());
        script.setJvmArgs(dto.getJvmArgs());
        script.setPreJavaCmd(dto.getPreJavaCmd());
        script.setPostJavaCmd(dto.getPostJavaCmd());
        script.setIsDefault(dto.getIsDefault() != null && dto.getIsDefault() == 1 ? 1 : 0);
        script.setDelFlag(0);

        script = scriptRepository.save(script);

        // 如果是默认脚本，取消其他默认设置
        if (script.getIsDefault() == 1) {
            ServiceStartScript oldDefault = scriptRepository.findByIsDefaultAndDelFlag(1, 0);
            if (oldDefault != null && !oldDefault.getId().equals(script.getId())) {
                oldDefault.setIsDefault(0);
                scriptRepository.save(oldDefault);
            }
        }

        // 创建第一个版本 v1.0.0
        ServiceStartScriptVersion version = new ServiceStartScriptVersion();
        version.setScriptId(script.getId());
        version.setVersionNo("v1.0.0");
        version.setVersionDescription("初始版本");
        version.setPort(dto.getPort());
        version.setJvmArgs(dto.getJvmArgs());
        version.setPreJavaCmd(dto.getPreJavaCmd());
        version.setPostJavaCmd(dto.getPostJavaCmd());
        version.setScriptContent(scriptTemplateBuilder.buildStandardScript(
                dto.getName(), "v1.0.0", dto.getPort(),
                dto.getJvmArgs(), dto.getPreJavaCmd(), dto.getPostJavaCmd()
        ));
        version.setIsActive(1);
        version.setDelFlag(0);

        version = versionRepository.save(version);

        // 更新主表的 current_version_id
        script.setCurrentVersionId(version.getId());
        script = scriptRepository.save(script);

        return getScriptDetail(script.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartScriptDTO updateScript(StartScriptDTO dto) {
        ServiceStartScript script = scriptRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("脚本不存在：" + dto.getId()));

        // 更新主表
        script.setName(dto.getName());
        script.setDescription(dto.getDescription());
        script.setPort(dto.getPort());
        script.setJvmArgs(dto.getJvmArgs());
        script.setPreJavaCmd(dto.getPreJavaCmd());
        script.setPostJavaCmd(dto.getPostJavaCmd());
        script.setIsDefault(dto.getIsDefault() != null && dto.getIsDefault() == 1 ? 1 : 0);

        // 如果是默认脚本，取消其他默认设置
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            script.setIsDefault(1);
            ServiceStartScript oldDefault = scriptRepository.findByIsDefaultAndDelFlag(1, 0);
            if (oldDefault != null && !oldDefault.getId().equals(script.getId())) {
                oldDefault.setIsDefault(0);
                scriptRepository.save(oldDefault);
            }
        } else {
            script.setIsDefault(0);
        }

        script = scriptRepository.save(script);

        // 创建新版本
        List<ServiceStartScriptVersion> existingVersions = versionRepository.findByScriptIdOrderByVersionNoDesc(script.getId());
        String nextVersion = generateNextVersion(existingVersions);

        ServiceStartScriptVersion version = new ServiceStartScriptVersion();
        version.setScriptId(script.getId());
        version.setVersionNo(nextVersion);
        version.setVersionDescription("更新至：" + dto.getName());
        version.setPort(dto.getPort());
        version.setJvmArgs(dto.getJvmArgs());
        version.setPreJavaCmd(dto.getPreJavaCmd());
        version.setPostJavaCmd(dto.getPostJavaCmd());
        version.setScriptContent(scriptTemplateBuilder.buildStandardScript(
                dto.getName(), nextVersion, dto.getPort(),
                dto.getJvmArgs(), dto.getPreJavaCmd(), dto.getPostJavaCmd()
        ));
        version.setIsActive(1);
        version.setDelFlag(0);

        version = versionRepository.save(version);

        // 取消旧版本激活状态
        versionRepository.deactivateAllByScriptId(script.getId());

        // 更新主表的 current_version_id
        script.setCurrentVersionId(version.getId());
        script = scriptRepository.save(script);

        return getScriptDetail(script.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScript(Integer id) {
        ServiceStartScript script = scriptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("脚本不存在：" + id));

        script.setDelFlag(1);
        scriptRepository.save(script);
    }

    @Override
    public List<StartScriptVersionDTO> getScriptVersions(Integer scriptId) {
        return versionRepository.findByScriptIdOrderByVersionNoDesc(scriptId)
                .stream()
                .map(this::convertToVersionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateVersion(Integer scriptId, Integer versionId) {
        ServiceStartScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("脚本不存在：" + scriptId));

        ServiceStartScriptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("版本不存在：" + versionId));

        if (!version.getScriptId().equals(scriptId)) {
            throw new RuntimeException("版本不属于该脚本");
        }

        // 取消所有版本激活状态
        versionRepository.deactivateAllByScriptId(scriptId);

        // 激活指定版本
        version.setIsActive(1);
        versionRepository.save(version);

        // 更新主表的 current_version_id
        script.setCurrentVersionId(versionId);
        scriptRepository.save(script);
    }

    @Override
    public StartScriptDTO getDefaultScript() {
        ServiceStartScript script = scriptRepository.findByIsDefaultAndDelFlag(1, 0);
        if (script == null) {
            return null;
        }
        StartScriptDTO dto = convertToDTO(script);

        // 加载当前版本的脚本内容
        if (script.getCurrentVersionId() != null) {
            ServiceStartScriptVersion version = versionRepository.findById(script.getCurrentVersionId())
                    .orElse(null);
            if (version != null) {
                dto.setScriptContent(version.getScriptContent());
            }
        }

        return dto;
    }

    /**
     * 生成下一个版本号
     */
    private String generateNextVersion(List<ServiceStartScriptVersion> versions) {
        if (versions.isEmpty()) {
            return "v1.0.0";
        }

        // 获取最大版本号
        String maxVersion = versions.get(0).getVersionNo();
        String[] parts = maxVersion.replace("v", "").split("\\.");

        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        // 增加补丁版本号
        patch++;

        return String.format("v%d.%d.%d", major, minor, patch);
    }

    /**
     * 转换为 DTO
     */
    private StartScriptDTO convertToDTO(ServiceStartScript script) {
        StartScriptDTO dto = new StartScriptDTO();
        dto.setId(script.getId());
        dto.setName(script.getName());
        dto.setDescription(script.getDescription());
        dto.setPort(script.getPort());
        dto.setJvmArgs(script.getJvmArgs());
        dto.setPreJavaCmd(script.getPreJavaCmd());
        dto.setPostJavaCmd(script.getPostJavaCmd());
        dto.setCurrentVersionId(script.getCurrentVersionId());
        dto.setIsDefault(script.getIsDefault());
        dto.setCreatedBy(script.getCreateBy());
        dto.setCreatedTime(script.getCreateTime());
        dto.setUpdatedBy(script.getUpdateBy());
        dto.setUpdatedTime(script.getUpdateTime());
        return dto;
    }

    /**
     * 转换为版本 DTO
     */
    private StartScriptVersionDTO convertToVersionDTO(ServiceStartScriptVersion version) {
        StartScriptVersionDTO dto = new StartScriptVersionDTO();
        dto.setId(version.getId());
        dto.setScriptId(version.getScriptId());
        dto.setVersionNo(version.getVersionNo());
        dto.setVersionDescription(version.getVersionDescription());
        dto.setPort(version.getPort());
        dto.setJvmArgs(version.getJvmArgs());
        dto.setPreJavaCmd(version.getPreJavaCmd());
        dto.setPostJavaCmd(version.getPostJavaCmd());
        dto.setScriptContent(version.getScriptContent());
        dto.setIsActive(version.getIsActive());
        dto.setCreatedBy(version.getCreateBy());
        dto.setCreatedTime(version.getCreateTime());
        return dto;
    }
}
