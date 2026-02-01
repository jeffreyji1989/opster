package com.opster.module.service.service.impl;

import com.opster.common.LocalCommandUtils;
import com.opster.config.OpsterProperties;
import com.opster.module.service.service.NodeVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node.js 版本管理服务实现类
 * 基于 nvm 进行 Node.js 版本管理
 */
@Service
@Slf4j
public class NodeVersionServiceImpl implements NodeVersionService {

    @Autowired
    private OpsterProperties opsterProperties;

    private static final Pattern VERSION_PATTERN = Pattern.compile("^v\\d+\\.\\d+\\.\\d+$");

    /**
     * 展开路径中的 ~ 符号为用户的 home 目录
     * @param path 可能包含 ~ 的路径
     * @return 展开后的绝对路径
     */
    private String expandTilde(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    @Override
    public List<String> listInstalledVersions() {
        List<String> versions = new ArrayList<>();

        try {
            String nvmPath = expandTilde(opsterProperties.getNodejs().getNvmPath());
            // 检查 nvm 是否存在
            File nvmDir = new File(nvmPath);
            if (!nvmDir.exists()) {
                log.warn("nvm 目录不存在: {}", nvmPath);
                return versions;
            }

            // 检查 versions/node 目录
            File versionsDir = new File(nvmDir, "versions/node");
            if (!versionsDir.exists()) {
                log.warn("nvm versions 目录不存在: {}", versionsDir.getAbsolutePath());
                return versions;
            }

            // 列出所有已安装的版本
            File[] versionDirs = versionsDir.listFiles(File::isDirectory);
            if (versionDirs != null) {
                for (File versionDir : versionDirs) {
                    String version = versionDir.getName();
                    if (isValidVersionFormat(version)) {
                        versions.add(version);
                    }
                }
            }

        } catch (Exception e) {
            log.error("获取已安装的 Node.js 版本列表失败", e);
        }

        return versions;
    }

    @Override
    public boolean installVersion(String version) throws Exception {
        if (!isValidVersionFormat(version)) {
            throw new IllegalArgumentException("无效的 Node.js 版本号格式: " + version);
        }

        if (isVersionInstalled(version)) {
            log.info("Node.js 版本 {} 已安装，跳过安装", version);
            return true;
        }

        try {
            String nvmPath = expandTilde(opsterProperties.getNodejs().getNvmPath());
            String nvmScript = nvmPath + "/nvm.sh";

            // 检查 nvm.sh 是否存在
            File nvmSh = new File(nvmScript);
            if (!nvmSh.exists()) {
                throw new Exception("nvm.sh 不存在: " + nvmScript);
            }

            // 构建 nvm install 命令
            String command = String.format("source %s && nvm install %s", nvmScript, version);

            log.info("开始安装 Node.js 版本: {}", version);
            log.info("nvm 路径: {}", nvmPath);

            // 执行安装命令
            Path workDir = Paths.get(System.getProperty("user.home"));
            LocalCommandUtils.CommandResult result = LocalCommandUtils.executeCommandQuietly(workDir, command);

            if (result.isSuccess()) {
                log.info("Node.js 版本 {} 安装成功", version);
                return true;
            } else {
                log.error("Node.js 版本 {} 安装失败: {}", version, result.getOutput());
                throw new Exception("安装失败: " + result.getOutput());
            }

        } catch (Exception e) {
            log.error("安装 Node.js 版本 {} 时发生异常", version, e);
            throw e;
        }
    }

    @Override
    public String getNodePath(String version) throws Exception {
        if (!isValidVersionFormat(version)) {
            throw new IllegalArgumentException("无效的 Node.js 版本号格式: " + version);
        }

        String nvmPath = expandTilde(opsterProperties.getNodejs().getNvmPath());
        // nvm Node.js 安装路径：~/.nvm/versions/node/v{version}/bin/node
        return nvmPath + "/versions/node/" + version + "/bin/node";
    }

    @Override
    public boolean isVersionInstalled(String version) {
        if (!isValidVersionFormat(version)) {
            return false;
        }

        try {
            String nodePath = getNodePath(version);
            File nodeFile = new File(nodePath);
            return nodeFile.exists() && nodeFile.canExecute();
        } catch (Exception e) {
            log.error("检查 Node.js 版本是否存在时发生异常: {}", version, e);
            return false;
        }
    }

    @Override
    public boolean isValidVersionFormat(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }

        Matcher matcher = VERSION_PATTERN.matcher(version);
        return matcher.matches();
    }
}
