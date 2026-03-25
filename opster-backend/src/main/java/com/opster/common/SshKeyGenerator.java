package com.opster.common;

import com.jcraft.jsch.KeyPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * SSH 密钥生成器
 * 使用 JSch KeyPair 生成 RSA 密钥对（确保兼容 JSch 0.1.55）
 */
@Slf4j
@Component
public class SshKeyGenerator {
    
    // 缓存 JSch KeyPair 以便获取 PEM 格式
    private ThreadLocal<com.jcraft.jsch.KeyPair> jschKeyPairCache = new ThreadLocal<>();

    /**
     * 生成 RSA 密钥对（使用 JSch KeyPair，确保兼容 JSch 0.1.55）
     */
    public java.security.KeyPair generateKeyPair() throws Exception {
        // 使用 JSch KeyPair 生成，确保格式兼容
        com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
        com.jcraft.jsch.KeyPair jschKpair = com.jcraft.jsch.KeyPair.genKeyPair(
            jsch, 
            com.jcraft.jsch.KeyPair.RSA, 
            2048
        );
        
        // 缓存 JSch KeyPair 以便后续获取 PEM 格式
        jschKeyPairCache.set(jschKpair);
        
        // 从 JSch KeyPair 提取 Java 标准的 KeyPair
        java.io.ByteArrayOutputStream privOut = new java.io.ByteArrayOutputStream();
        jschKpair.writePrivateKey(privOut);
        String pem = privOut.toString();
        PrivateKey privateKey = fromPemFormat(pem);
        
        // 读取公钥并解析
        byte[] pubKeyBlob = jschKpair.getPublicKeyBlob();
        PublicKey publicKey = parseOpenSshPublicKey(pubKeyBlob);
        
        java.security.KeyPair keyPair = new java.security.KeyPair(publicKey, privateKey);
        log.info("已生成 RSA 密钥对 (2048 位，使用 JSch KeyPair)");
        return keyPair;
    }

    /**
     * 将私钥转换为 PEM 格式（使用 JSch KeyPair 确保格式正确）
     * 这个方法必须在 generateKeyPair() 之后调用，因为需要从缓存的 JSch KeyPair 获取 PEM
     */
    public String toPemFormat(PrivateKey privateKey) throws Exception {
        // 从缓存的 JSch KeyPair 获取 PEM 格式
        com.jcraft.jsch.KeyPair jschKpair = jschKeyPairCache.get();
        if (jschKpair != null) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            jschKpair.writePrivateKey(out);
            String pem = out.toString();
            log.info("私钥已转换为 PEM 格式 (JSch 兼容)");
            return pem;
        } else {
            // Fallback: 使用 PKCS#8 格式
            byte[] encoded = privateKey.getEncoded();
            String base64 = Base64.getEncoder().encodeToString(encoded);
            StringBuilder pem = new StringBuilder();
            pem.append("-----BEGIN PRIVATE KEY-----\n");
            for (int i = 0; i < base64.length(); i += 64) {
                int end = Math.min(i + 64, base64.length());
                pem.append(base64, i, end).append("\n");
            }
            pem.append("-----END PRIVATE KEY-----\n");
            return pem.toString();
        }
    }

    /**
     * 生成公钥（OpenSSH 格式，用于写入 authorized_keys）
     * 使用缓存的 JSch KeyPair 获取正确的公钥 blob
     */
    public String toOpenSSHPublicKey(PublicKey publicKey) throws Exception {
        com.jcraft.jsch.KeyPair jschKpair = jschKeyPairCache.get();
        if (jschKpair != null) {
            byte[] pubKeyBlob = jschKpair.getPublicKeyBlob();
            String base64Key = Base64.getEncoder().encodeToString(pubKeyBlob);
            return "ssh-rsa " + base64Key + " opster-generated";
        } else {
            // Fallback: 手动构建
            byte[] x509Encoded = publicKey.getEncoded();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            String sshAlgo = "ssh-rsa";
            byte[] algoBytes = sshAlgo.getBytes();
            writeUInt32(out, algoBytes.length);
            out.write(algoBytes);
            writeUInt32(out, x509Encoded.length);
            out.write(x509Encoded);
            byte[] sshKeyData = out.toByteArray();
            String base64Key = Base64.getEncoder().encodeToString(sshKeyData);
            return sshAlgo + " " + base64Key + " opster-generated";
        }
    }
    
    /**
     * 从 OpenSSH 公钥 blob 解析 PublicKey
     */
    private PublicKey parseOpenSshPublicKey(byte[] blob) throws Exception {
        java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(blob);
        
        // 读取算法名称
        int algoLen = readUInt32(in);
        byte[] algoBytes = new byte[algoLen];
        in.read(algoBytes);
        String algo = new String(algoBytes);
        
        if (!"ssh-rsa".equals(algo)) {
            throw new Exception("不支持的公钥算法：" + algo);
        }
        
        // 读取 e 和 n
        int eLen = readUInt32(in);
        byte[] eBytes = new byte[eLen];
        in.read(eBytes);
        java.math.BigInteger e = new java.math.BigInteger(1, eBytes);
        
        int nLen = readUInt32(in);
        byte[] nBytes = new byte[nLen];
        in.read(nBytes);
        java.math.BigInteger n = new java.math.BigInteger(1, nBytes);
        
        // 构建 RSA 公钥
        java.security.spec.RSAPublicKeySpec keySpec = new java.security.spec.RSAPublicKeySpec(n, e);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
    
    /**
     * 从 PEM 格式解析私钥（支持 PKCS#1 和 PKCS#8 格式）
     */
    public PrivateKey fromPemFormat(String pem) throws Exception {
        // 移除 PEM 头尾和空白
        String base64 = pem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] encoded = Base64.getDecoder().decode(base64);
        
        // 检测 PEM 格式类型
        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            // PKCS#1 格式 - 手动解析 ASN.1
            return parsePkcs1RsaPrivateKey(encoded);
        } else {
            // PKCS#8 格式
            java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(encoded);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        }
    }
    
    /**
     * 解析 PKCS#1 RSA 私钥 (ASN.1 DER 编码)
     * 格式：SEQUENCE { version, modulus, publicExponent, privateExponent, prime1, prime2, exponent1, exponent2, coefficient }
     */
    private PrivateKey parsePkcs1RsaPrivateKey(byte[] derEncoded) throws Exception {
        java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(derEncoded);
        
        // 跳过 SEQUENCE 标签和长度
        in.read(); // tag 0x30
        int seqLen = readLength(in);
        
        // 读取 version (INTEGER 0)
        in.read(); // tag 0x02
        int versionLen = in.read();
        in.skip(versionLen);
        
        // 读取各个字段
        java.math.BigInteger modulus = readBigInteger(in);
        java.math.BigInteger publicExp = readBigInteger(in);
        java.math.BigInteger privateExp = readBigInteger(in);
        java.math.BigInteger primeP = readBigInteger(in);
        java.math.BigInteger primeQ = readBigInteger(in);
        java.math.BigInteger primeExpP = readBigInteger(in);
        java.math.BigInteger primeExpQ = readBigInteger(in);
        java.math.BigInteger crtCoeff = readBigInteger(in);
        
        // 构建 RSA 私钥
        java.security.spec.RSAPrivateCrtKeySpec keySpec = new java.security.spec.RSAPrivateCrtKeySpec(
            modulus, publicExp, privateExp, primeP, primeQ, primeExpP, primeExpQ, crtCoeff
        );
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
    
    /**
     * 读取 ASN.1 长度
     */
    private int readLength(java.io.InputStream in) throws Exception {
        int len = in.read();
        if ((len & 0x80) != 0) {
            int numBytes = len & 0x7F;
            len = 0;
            for (int i = 0; i < numBytes; i++) {
                len = (len << 8) | in.read();
            }
        }
        return len;
    }
    
    /**
     * 从 ASN.1 流中读取 BigInteger
     */
    private java.math.BigInteger readBigInteger(java.io.InputStream in) throws Exception {
        int tag = in.read();
        if (tag != 0x02) {
            throw new Exception("Expected INTEGER tag, got: " + tag);
        }
        int len = readLength(in);
        byte[] bytes = new byte[len];
        in.read(bytes);
        return new java.math.BigInteger(1, bytes); // 使用正数构造函数
    }
    
    /**
     * 读取 32 位无符号整数（大端序）
     */
    private int readUInt32(java.io.InputStream in) throws Exception {
        int b1 = in.read() & 0xFF;
        int b2 = in.read() & 0xFF;
        int b3 = in.read() & 0xFF;
        int b4 = in.read() & 0xFF;
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }
    
    /**
     * 写入 32 位无符号整数（大端序）
     */
    private void writeUInt32(java.io.ByteArrayOutputStream out, int value) {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }
}
