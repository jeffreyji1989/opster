package com.opster.module.server.service.impl;

import com.opster.common.SecurityUtils;
import com.opster.common.SshKeyGenerator;
import com.opster.common.SshUtils;
import com.opster.common.enums.Status;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.server.service.ServerService;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
@Slf4j
public class ServerServiceImpl implements ServerService {

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private SshKeyGenerator sshKeyGenerator;

    @Override
    public List<Server> findAll() {
        return serverRepository.findAll();
    }

    @Override
    public List<Server> findList(String ip, String groupName, String env, Integer status) {
        Specification<Server> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (ip != null && !ip.isEmpty()) {
                predicates.add(cb.like(root.get("ip"), "%" + ip + "%"));
            }
            
            if (groupName != null && !groupName.isEmpty()) {
                predicates.add(cb.like(root.get("groupName"), "%" + groupName + "%"));
            }
            
            if (env != null && !env.isEmpty()) {
                predicates.add(cb.equal(root.get("env"), env));
            }
            
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), Status.fromCode(status)));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return serverRepository.findAll(spec);
    }

    @Override
    public Page<Server> findPage(Pageable pageable) {
        return serverRepository.findAll(pageable);
    }

    @Override
    public Optional<Server> findById(Integer id) {
        return serverRepository.findById(id);
    }

    @Override
    public Server save(Server server) {
        if (server.getPassword() != null && !server.getPassword().isEmpty()) {
            // 如果不是加密过的，说明是明文，需要加密
            if (!com.opster.common.SecurityUtils.isEncrypted(server.getPassword())) {
                server.setPassword(com.opster.common.SecurityUtils.encrypt(server.getPassword()));
            }
        }
        return serverRepository.save(server);
    }

    @Override
    public void deleteById(Integer id) {
        serverRepository.deleteById(id);
    }

    @Override
    public long count() {
        return serverRepository.count();
    }

    @Override
    @Transactional
    public Map<String, Object> generateAndDeployKey(Integer serverId) {
        // 1. 获取服务器信息
        Server server = serverRepository.findById(serverId)
            .orElseThrow(() -> new RuntimeException("服务器不存在"));

        // 验证密码是否存在
        if (server.getPassword() == null || server.getPassword().isEmpty()) {
            throw new RuntimeException("服务器未配置密码，无法生成密钥对");
        }

        try {
            // 2. 生成 ED25519 密钥对
            java.security.KeyPair keyPair = sshKeyGenerator.generateKeyPair();
            String privateKeyPem = sshKeyGenerator.toPemFormat(keyPair.getPrivate());
            String publicKeyOpenSSH = sshKeyGenerator.toOpenSSHPublicKey(keyPair.getPublic());

            // 3. 使用密码认证连接
            log.info("开始连接服务器 {}@{} 部署公钥", server.getUsername(), server.getIp());
            Session session = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());

            try {
                // 4. 创建 ~/.ssh 目录并设置权限
                log.info("创建 ~/.ssh 目录");
                SshUtils.exec(session, "mkdir -p ~/.ssh && chmod 700 ~/.ssh");

                // 5. 追加公钥到 authorized_keys（避免覆盖现有密钥）
                log.info("追加公钥到 authorized_keys");
                // 转义公钥中的特殊字符（单引号）
                String escapedPublicKey = publicKeyOpenSSH.replace("'", "'\\''");
                SshUtils.exec(session, "echo '" + escapedPublicKey + "' >> ~/.ssh/authorized_keys");

                // 6. 设置 authorized_keys 权限
                log.info("设置 authorized_keys 权限");
                SshUtils.exec(session, "chmod 600 ~/.ssh/authorized_keys");

                // 7. 验证公钥是否部署成功
                // 使用公钥的 base64 部分进行精确匹配（跳过算法前缀和注释）
                int algoEndIndex = publicKeyOpenSSH.indexOf(' ');
                int commentStartIndex = publicKeyOpenSSH.lastIndexOf(" opster-generated");
                String publicKeyBase64 = publicKeyOpenSSH.substring(algoEndIndex + 1, commentStartIndex > 0 ? commentStartIndex : publicKeyOpenSSH.length());
                String checkResult = SshUtils.exec(session, "grep -F '" + publicKeyBase64 + "' ~/.ssh/authorized_keys | wc -l");
                if (!checkResult.trim().equals("1")) {
                    throw new RuntimeException("公钥部署失败，无法在 authorized_keys 中找到部署的公钥");
                }

                // 8. 保存私钥到数据库（加密存储）
                log.info("保存私钥到数据库");
                server.setPrivateKey(SecurityUtils.encrypt(privateKeyPem));
                serverRepository.save(server);

                log.info("密钥对生成并部署成功：{}@{}", server.getUsername(), server.getIp());

                return Map.of(
                    "success", true,
                    "message", "密钥生成并部署成功",
                    "publicKey", publicKeyOpenSSH,
                    "serverId", server.getId(),
                    "serverIp", server.getIp()
                );

            } finally {
                SshUtils.disconnect(session);
            }

        } catch (Exception e) {
            log.error("密钥部署失败：{}@{}", server.getUsername(), server.getIp(), e);
            throw new RuntimeException("密钥部署失败：" + e.getMessage(), e);
        }
    }
}
