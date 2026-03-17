package com.opster.module.service.service;

import com.opster.module.service.dto.StartScriptDTO;
import com.opster.module.service.dto.StartScriptVersionDTO;

import java.util.List;

/**
 * 启动脚本管理服务接口
 */
public interface ServiceStartScriptService {

    /**
     * 查询所有脚本列表
     */
    List<StartScriptDTO> listScripts();

    /**
     * 获取脚本详情（包含当前激活版本）
     */
    StartScriptDTO getScriptDetail(Integer id);

    /**
     * 创建脚本
     */
    StartScriptDTO createScript(StartScriptDTO dto);

    /**
     * 更新脚本
     */
    StartScriptDTO updateScript(StartScriptDTO dto);

    /**
     * 删除脚本
     */
    void deleteScript(Integer id);

    /**
     * 查询脚本的所有版本
     */
    List<StartScriptVersionDTO> getScriptVersions(Integer scriptId);

    /**
     * 激活指定版本
     */
    void activateVersion(Integer scriptId, Integer versionId);

    /**
     * 获取默认脚本
     */
    StartScriptDTO getDefaultScript();
}
