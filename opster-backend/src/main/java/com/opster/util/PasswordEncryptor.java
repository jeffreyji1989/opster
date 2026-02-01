package com.opster.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 密码加密工具
 * 用于生成服务器密码的AES加密值
 */
public class PasswordEncryptor {

    public static void main(String[] args) {
        String encryptKey = "opster-default-key";

        // 初始化AES加密器
        byte[] keyBytes = new byte[16];
        byte[] originalKeyBytes = encryptKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(originalKeyBytes, 0, keyBytes, 0, Math.min(originalKeyBytes.length, 16));
        AES aes = SecureUtil.aes(keyBytes);

        System.out.println("=== Opster 服务器密码加密工具 ===");
        System.out.println("请输入服务器的真实SSH密码:");

        Scanner scanner = new Scanner(System.in);
        String plainPassword = scanner.nextLine().trim();

        if (plainPassword.isEmpty()) {
            System.out.println("密码不能为空！");
            return;
        }

        // 加密密码
        String encryptedPassword = aes.encryptHex(plainPassword);

        System.out.println("\n加密成功！");
        System.out.println("明文密码: " + plainPassword);
        System.out.println("加密后: " + encryptedPassword);
        System.out.println("\n请使用以下SQL语句更新数据库:");
        System.out.println("UPDATE server SET password = '" + encryptedPassword + "' WHERE ip = '服务器IP';");

        scanner.close();
    }
}
