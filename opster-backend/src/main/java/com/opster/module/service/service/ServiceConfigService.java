package com.opster.module.service.service;

import com.jcraft.jsch.Session;
import com.opster.module.service.dto.ConfigFileDTO;
import com.opster.module.service.entity.ConfigFileVersion;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * 配置文件服务接口
 */
public interface ServiceConfigService {

    /**
     * 获取服务配置文件列表
     * @param serviceId 服务 ID
     * @return 配置文件列表
     */
    List<ConfigFileDTO> getConfigFiles(Integer serviceId);

    /**
     * 保存配置文件
     * @param serviceId 服务 ID
     * @param configFile 配置文件信息
     * @return 保存后的配置文件
     */
    ConfigFileDTO saveConfigFile(Integer serviceId, ConfigFileDTO configFile);

    /**
     * 删除配置文件
     * @param serviceId 服务 ID
     * @param configId 配置文件 ID
     */
    void deleteConfigFile(Integer serviceId, Integer configId);

    /**
     * 获取配置文件版本历史
     * @param serviceId 服务 ID
     * @param filename 文件名
     * @return 版本历史列表
     */
    List<ConfigFileVersion> getConfigFileVersions(Integer serviceId, String filename);

    /**
     * 回退到历史版本
     * @param serviceId 服务 ID
     * @param versionId 版本 ID
     * @return 回退后的版本
     */
    ConfigFileVersion rollbackToVersion(Integer serviceId, Integer versionId);

    /**
     * 从 Git 同步配置文件
     * @param serviceId 服务 ID
     * @return 同步后的配置文件列表
     */
    List<ConfigFileDTO> syncFromGit(Integer serviceId);

    /**
     * 标记配置文件为已部署
     * @param configId 配置文件 ID
     */
    void markAsDeployed(Integer configId);

    /**
     * 上传配置文件到服务器
     * @param serviceId 服务 ID
     * @param sshSession SSH 会话
     * @param deployPath 部署路径
     * @param wsSession WebSocket 会话（用于推送日志）
     * @throws Exception 上传过程中发生的异常
     */
    void uploadConfigFilesToServer(Integer serviceId, Session sshSession, String deployPath, WebSocketSession wsSession) throws Exception;
}
