package com.opster.module.server.service.impl;

import com.opster.common.enums.Status;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.server.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class ServerServiceImpl implements ServerService {

    @Autowired
    private ServerRepository serverRepository;

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
}
