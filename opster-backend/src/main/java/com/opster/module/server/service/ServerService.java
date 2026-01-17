package com.opster.module.server.service;

import com.opster.module.server.entity.Server;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ServerService {
    List<Server> findAll();
    Page<Server> findPage(Pageable pageable);
    Optional<Server> findById(Integer id);
    Server save(Server server);
    void deleteById(Integer id);
    long count();
}
