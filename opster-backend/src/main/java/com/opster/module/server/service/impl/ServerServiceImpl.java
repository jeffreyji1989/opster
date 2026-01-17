package com.opster.module.server.service.impl;

import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.server.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    public Page<Server> findPage(Pageable pageable) {
        return serverRepository.findAll(pageable);
    }

    @Override
    public Optional<Server> findById(Integer id) {
        return serverRepository.findById(id);
    }

    @Override
    public Server save(Server server) {
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
