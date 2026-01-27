package com.opster.config;

import com.opster.common.enums.Status;
import com.opster.module.project.dto.RepositoryDTO;
import com.opster.module.project.entity.Project;
import com.opster.module.project.repository.ProjectRepository;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.repository.AppServiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(ProjectRepository projectRepository,
                                      ServerRepository serverRepository,
                                      AppServiceRepository appServiceRepository) {
        return args -> {
        };
    }
}
