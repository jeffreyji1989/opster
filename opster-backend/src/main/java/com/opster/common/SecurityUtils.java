package com.opster.common;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * 安全工具类，提供加密解密功能
 */
@Component
public class SecurityUtils {

    @Value("${opster.encrypt-key:opster-default-key}")
    private String encryptKey;

    private static AES aes;

    @PostConstruct
    public void init() {
        // 确保密钥长度为16位
        byte[] keyBytes = new byte[16];
        byte[] originalKeyBytes = encryptKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(originalKeyBytes, 0, keyBytes, 0, Math.min(originalKeyBytes.length, 16));
        aes = SecureUtil.aes(keyBytes);
    }

    /**
     * 加密
     * @param data 明文
     * @return 密文
     */
    public static String encrypt(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        return aes.encryptHex(data);
    }

    /**
     * 解密
     * @param hexData 密文
     * @return 明文
     */
    public static String decrypt(String hexData) {
        if (hexData == null || hexData.isEmpty()) {
            return hexData;
        }
        try {
            return aes.decryptStr(hexData);
        } catch (Exception e) {
            // 如果解密失败，可能是原文（比如旧数据），直接返回
            return hexData;
        }
    }

    /**
     * 判断是否是加密后的数据
     * @param data 数据
     * @return 是否加密
     */
    public static boolean isEncrypted(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        // 检查是否是32位十六进制字符串（可能是MD5或其他哈希）
        // 这类格式无法用AES解密，但也不是明文密码
        if (data.length() == 32 && data.matches("[0-9a-fA-F]{32}")) {
            return true; // 假设这是旧的哈希格式，当作已加密处理
        }

        try {
            // 如果能解密出来，且不等于原字符串，通常说明是加密过的
            String decrypted = aes.decryptStr(data);
            return !data.equals(decrypted);
        } catch (Exception e) {
            return false;
        }
    }
}