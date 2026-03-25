package com.opster.module.server.service;

import com.opster.module.server.entity.Server;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ServerService {
    List<Server> findAll();
    List<Server> findList(String ip, String groupName, String env, Integer status);
    Page<Server> findPage(Pageable pageable);
    Optional<Server> findById(Integer id);
    Server save(Server server);
    void deleteById(Integer id);
    long count();

    /**
     * 生成 SSH 密钥对并部署到目标服务器
     * @param serverId 服务器 ID
     * @return 部署结果
     */
    Map<String, Object> generateAndDeployKey(Integer serverId);
}
