package com.opster.module.service.service;

import java.util.List;

/**
 * Node.js 版本管理服务接口
 * 提供基于 nvm 的 Node.js 版本管理功能
 */
public interface NodeVersionService {

    /**
     * 获取已安装的 Node.js 版本列表
     *
     * @return 版本号列表，如 ["v18.17.0", "v20.10.0"]
     */
    List<String> listInstalledVersions();

    /**
     * 安装指定的 Node.js 版本
     *
     * @param version 版本号，格式：v18.17.0
     * @return 是否安装成功
     * @throws Exception 安装失败时抛出异常
     */
    boolean installVersion(String version) throws Exception;

    /**
     * 获取指定版本的 Node.js 可执行文件路径
     *
     * @param version 版本号，格式：v18.17.0
     * @return Node 可执行文件的完整路径
     * @throws Exception 获取路径失败时抛出异常
     */
    String getNodePath(String version) throws Exception;

    /**
     * 检查指定版本是否已安装
     *
     * @param version 版本号，格式：v18.17.0
     * @return true-已安装，false-未安装
     */
    boolean isVersionInstalled(String version);

    /**
     * 验证版本号格式是否有效
     *
     * @param version 版本号
     * @return true-格式有效，false-格式无效
     */
    boolean isValidVersionFormat(String version);
}
