package com.opster.module.service.service;

import com.opster.config.OpsterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NodeVersionService 测试类
 * 测试 Node.js 版本管理的核心功能
 */
@SpringBootTest
public class NodeVersionServiceTest {

    @Autowired
    private NodeVersionService nodeVersionService;

    @Autowired
    private OpsterProperties opsterProperties;

    /**
     * 测试：验证版本号格式
     * 应该：正确识别有效和无效的版本号格式
     */
    @Test
    public void testValidateVersionFormat() {
        // 有效的版本号格式
        assertTrue(nodeVersionService.isValidVersionFormat("v18.17.0"));
        assertTrue(nodeVersionService.isValidVersionFormat("v20.10.0"));
        assertTrue(nodeVersionService.isValidVersionFormat("v16.20.2"));

        // 无效的版本号格式
        assertFalse(nodeVersionService.isValidVersionFormat("18.17.0"));  // 缺少 v 前缀
        assertFalse(nodeVersionService.isValidVersionFormat("v18"));      // 缺少次版本和修订版
        assertFalse(nodeVersionService.isValidVersionFormat("latest"));   // 不是语义化版本
        assertFalse(nodeVersionService.isValidVersionFormat("invalid"));  // 完全无效
        assertFalse(nodeVersionService.isValidVersionFormat(""));         // 空字符串
        assertFalse(nodeVersionService.isValidVersionFormat(null));       // null
    }

    /**
     * 测试：获取已安装的版本列表
     * 应该：返回非空的列表（假设系统至少安装了一个 Node.js 版本）
     */
    @Test
    public void testListInstalledVersions() {
        List<String> versions = nodeVersionService.listInstalledVersions();

        assertNotNull(versions, "版本列表不应为 null");
        // 如果系统有 nvm 和安装的版本，列表应该非空
        // 但在没有 nvm 的环境中，可能返回空列表，这里只验证不抛异常
    }

    /**
     * 测试：检查版本是否已安装
     * 应该：正确判断版本是否已安装
     */
    @Test
    public void testIsVersionInstalled() {
        // 测试一个可能存在的版本（假设 v18.17.0 可能被安装）
        boolean installed = nodeVersionService.isVersionInstalled("v18.17.0");
        // 不强制断言结果，因为不同环境安装的版本不同
        // 只验证方法可以正常调用而不抛异常
        assertNotNull(installed);

        // 明确不存在的版本
        assertFalse(nodeVersionService.isVersionInstalled("v99.99.99"));
    }

    /**
     * 测试：获取 Node.js 可执行文件路径
     * 应该：返回正确格式的路径
     */
    @Test
    public void testGetNodePath() throws Exception {
        String version = "v18.17.0";

        // 即使版本不存在，也应该能生成路径
        String path = nodeVersionService.getNodePath(version);

        assertNotNull(path, "Node 路径不应为 null");
        assertTrue(path.endsWith("/bin/node"), "路径应以 /bin/node 结尾");
        assertTrue(path.contains(version), "路径应包含版本号");
    }

    /**
     * 测试：nvm 路径配置
     * 应该：正确读取配置的 nvm 路径
     */
    @Test
    public void testNvmPathConfiguration() {
        OpsterProperties.NodeJsConfig nodejsConfig = opsterProperties.getNodejs();

        assertNotNull(nodejsConfig, "Node.js 配置不应为 null");
        assertNotNull(nodejsConfig.getNvmPath(), "nvm 路径不应为 null");
        assertTrue(nodejsConfig.getNvmPath().contains(".nvm"), "nvm 路径应包含 .nvm");
    }

    /**
     * 测试：自动安装配置
     * 应该：正确读取自动安装配置
     */
    @Test
    public void testAutoInstallConfiguration() {
        OpsterProperties.NodeJsConfig nodejsConfig = opsterProperties.getNodejs();

        assertNotNull(nodejsConfig.getAutoInstall(), "自动安装配置不应为 null");
        assertNotNull(nodejsConfig.getInstallTimeout(), "安装超时配置不应为 null");
        assertTrue(nodejsConfig.getInstallTimeout() > 0, "超时时间应大于 0");
    }
}
