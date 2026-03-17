package com.opster.module.service.service.impl;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.opster.module.project.entity.SubProject;
import com.opster.module.project.repository.SubProjectRepository;
import com.opster.module.service.dto.ConfigFileDTO;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.entity.ConfigFileVersion;
import com.opster.module.service.repository.ConfigFileVersionRepository;
import com.opster.module.service.service.ServiceConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 配置文件服务实现类
 */
@Slf4j
@Service
public class ServiceConfigServiceImpl implements ServiceConfigService {

    @Autowired
    private ConfigFileVersionRepository configFileVersionRepository;

    @Autowired
    private SubProjectRepository subProjectRepository;

    @Autowired
    private com.opster.module.service.repository.AppServiceRepository appServiceRepository;

    @Override
    public List<ConfigFileDTO> getConfigFiles(Integer serviceId) {
        // 获取服务信息
        AppService service = appServiceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("服务不存在"));

        // 获取子项目 ID
        Integer subProjectId = service.getSubProjectId();
        if (subProjectId == null) {
            return new ArrayList<>();
        }

        // 获取最新的配置文件版本（每个文件名一个）
        List<ConfigFileVersion> latestVersions = configFileVersionRepository
            .findLatestVersionsBySubProjectId(subProjectId);

        // 转换为 DTO
        return latestVersions.stream().map(version -> {
            ConfigFileDTO dto = new ConfigFileDTO();
            dto.setId(version.getId());
            dto.setFilename(version.getFilename());
            dto.setContent(version.getContent());
            dto.setLastModified(version.getCreateTime());
            dto.setIsDeployed(version.getIsDeployed());
            dto.setVersionTag(version.getVersionTag());
            dto.setVersionDescription(version.getVersionDescription());

            // 查询版本数量
            List<ConfigFileVersion> allVersions = configFileVersionRepository
                .findBySubProjectIdAndFilenameOrderByCreateTimeDesc(subProjectId, version.getFilename());
            dto.setVersionCount(allVersions.size());

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public ConfigFileDTO saveConfigFile(Integer serviceId, ConfigFileDTO configFile) {
        // 获取服务信息
        AppService service = appServiceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("服务不存在"));

        // 获取子项目 ID
        Integer subProjectId = service.getSubProjectId();
        if (subProjectId == null) {
            throw new RuntimeException("服务未关联子项目");
        }

        // 创建新版本记录
        ConfigFileVersion version = new ConfigFileVersion();
        version.setSubProjectId(subProjectId);
        version.setFilename(configFile.getFilename());
        version.setContent(configFile.getContent());
        version.setVersionTag(configFile.getVersionTag());
        version.setVersionDescription(configFile.getVersionDescription());
        version.setIsDeployed(false); // 保存时标记为未部署

        configFileVersionRepository.save(version);

        // 转换为 DTO
        ConfigFileDTO dto = new ConfigFileDTO();
        dto.setId(version.getId());
        dto.setFilename(version.getFilename());
        dto.setContent(version.getContent());
        dto.setLastModified(version.getCreateTime());
        dto.setIsDeployed(false);

        return dto;
    }

    @Override
    public void deleteConfigFile(Integer serviceId, Integer configId) {
        // 获取服务信息
        AppService service = appServiceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("服务不存在"));

        // 获取子项目 ID
        Integer subProjectId = service.getSubProjectId();
        if (subProjectId == null) {
            throw new RuntimeException("服务未关联子项目");
        }

        // 验证配置文件属于该子项目
        ConfigFileVersion version = configFileVersionRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("配置文件不存在"));

        if (!version.getSubProjectId().equals(subProjectId)) {
            throw new RuntimeException("配置文件不属于该服务");
        }

        configFileVersionRepository.deleteById(configId);
    }

    @Override
    public List<ConfigFileVersion> getConfigFileVersions(Integer serviceId, String filename) {
        // 获取服务信息
        AppService service = appServiceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("服务不存在"));

        // 获取子项目 ID
        Integer subProjectId = service.getSubProjectId();
        if (subProjectId == null) {
            return new ArrayList<>();
        }

        return configFileVersionRepository
            .findBySubProjectIdAndFilenameOrderByCreateTimeDesc(subProjectId, filename);
    }

    @Override
    public ConfigFileVersion rollbackToVersion(Integer serviceId, Integer versionId) {
        // 获取服务信息
        AppService service = appServiceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("服务不存在"));

        // 获取子项目 ID
        Integer subProjectId = service.getSubProjectId();
        if (subProjectId == null) {
            throw new RuntimeException("服务未关联子项目");
        }

        // 获取版本记录
        ConfigFileVersion sourceVersion = configFileVersionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("版本不存在"));

        if (!sourceVersion.getSubProjectId().equals(subProjectId)) {
            throw new RuntimeException("版本不属于该服务");
        }

        // 创建新版本记录（回退版本）
        ConfigFileVersion newVersion = new ConfigFileVersion();
        newVersion.setSubProjectId(subProjectId);
        newVersion.setFilename(sourceVersion.getFilename());
        newVersion.setContent(sourceVersion.getContent());
        newVersion.setVersionTag(sourceVersion.getVersionTag() + " (回退)");
        newVersion.setVersionDescription("回退到版本：" + sourceVersion.getVersionTag());
        newVersion.setGitCommitHash(sourceVersion.getGitCommitHash());
        newVersion.setIsDeployed(false);

        return configFileVersionRepository.save(newVersion);
    }

    @Override
    public List<ConfigFileDTO> syncFromGit(Integer serviceId) {
        // TODO: 实现从 Git 同步配置文件的逻辑
        // 这需要：
        // 1. 克隆 Git 仓库
        // 2. 读取配置文件内容
        // 3. 保存到数据库
        // 由于实现复杂度较高，暂时返回空列表
        log.warn("从 Git 同步配置文件功能尚未实现");
        return new ArrayList<>();
    }

    @Override
    public void markAsDeployed(Integer configId) {
        ConfigFileVersion version = configFileVersionRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("配置文件不存在"));

        version.setIsDeployed(true);
        version.setDeployTime(LocalDateTime.now());
        configFileVersionRepository.save(version);
    }

    @Override
    public void uploadConfigFilesToServer(Integer serviceId, Session sshSession,
                                           String deployPath, WebSocketSession wsSession) throws Exception {
        // 获取服务信息
        AppService service = appServiceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("服务不存在"));

        // 获取子项目 ID
        Integer subProjectId = service.getSubProjectId();
        if (subProjectId == null) {
            sendWsMessage(wsSession, "未找到子项目配置，跳过配置文件上传");
            return;
        }

        // 获取已配置的文件列表（从 SubProject 的 configFiles 字段）
        SubProject subProject = subProjectRepository.findById(subProjectId)
            .orElseThrow(() -> new RuntimeException("子项目不存在"));

        String configFilesJson = subProject.getConfigFiles();
        if (configFilesJson == null || configFilesJson.trim().isEmpty()) {
            sendWsMessage(wsSession, "未配置配置文件，跳过上传");
            return;
        }

        // 从数据库获取最新的配置文件内容
        List<ConfigFileVersion> configVersions = configFileVersionRepository
            .findLatestVersionsBySubProjectId(subProjectId);

        if (configVersions.isEmpty()) {
            sendWsMessage(wsSession, "未找到配置文件版本，跳过上传");
            return;
        }

        sendWsMessage(wsSession, "发现 " + configVersions.size() + " 个配置文件，开始上传...");

        // 上传每个配置文件
        for (ConfigFileVersion version : configVersions) {
            try {
                sendWsMessage(wsSession, "正在上传配置文件：" + version.getFilename());

                // 创建临时文件
                Path tempFile = Files.createTempFile("config-" + version.getFilename() + "-", ".tmp");
                try {
                    // 写入配置文件内容
                    Files.write(tempFile, version.getContent().getBytes(StandardCharsets.UTF_8));

                    // 使用 SCP 命令上传文件
                    uploadFileViaScp(sshSession, tempFile, deployPath + "/" + version.getFilename(), wsSession);

                    // 标记为已部署
                    markAsDeployed(version.getId());

                    sendWsMessage(wsSession, "配置文件 " + version.getFilename() + " 上传成功");
                } finally {
                    // 删除临时文件
                    Files.deleteIfExists(tempFile);
                }
            } catch (Exception e) {
                log.error("上传配置文件失败：" + version.getFilename(), e);
                sendWsMessage(wsSession, "配置文件 " + version.getFilename() + " 上传失败：" + e.getMessage());
            }
        }

        sendWsMessage(wsSession, "配置文件上传完成");
    }

    /**
     * 通过 SCP 上传文件到远程服务器
     */
    private void uploadFileViaScp(Session session, Path localFile, String remotePath,
                                   WebSocketSession wsSession) throws Exception {
        String command = "scp -t " + remotePath;
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        channel.connect(30000);

        // 检查服务器响应
        int reply = in.read();
        if (reply != 0) {
            throw new Exception("SCP 命令失败：" + in.readLine());
        }

        // 发送文件信息
        long fileSize = Files.size(localFile);
        String sendCommand = "C0644 " + fileSize + " " + localFile.getFileName().toString() + "\n";
        channel.getOutputStream().write(sendCommand.getBytes());
        channel.getOutputStream().flush();

        // 检查服务器响应
        reply = in.read();
        if (reply != 0) {
            throw new Exception("发送文件信息失败");
        }

        // 发送文件内容
        byte[] fileContent = Files.readAllBytes(localFile);
        channel.getOutputStream().write(fileContent);
        channel.getOutputStream().flush();

        // 检查服务器响应
        reply = in.read();
        if (reply != 0) {
            throw new Exception("发送文件内容失败");
        }

        // 发送结束标记
        channel.getOutputStream().write(0);
        channel.getOutputStream().flush();

        channel.disconnect();
        in.close();
    }

    /**
     * 发送 WebSocket 消息
     */
    private void sendWsMessage(WebSocketSession wsSession, String message) {
        if (wsSession != null && wsSession.isOpen()) {
            try {
                wsSession.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("发送 WebSocket 消息失败", e);
            }
        }
    }
}
