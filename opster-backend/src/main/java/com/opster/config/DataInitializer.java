package com.opster.config;

import com.opster.common.enums.Status;
import com.opster.module.project.entity.Project;
import com.opster.module.project.repository.ProjectRepository;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.repository.AppServiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(ProjectRepository projectRepository,
                                      ServerRepository serverRepository,
                                      AppServiceRepository appServiceRepository) {
        return args -> {
            // Init Projects
            if (projectRepository.count() == 0) {
                Project p1 = new Project();
                p1.setProjectName("Opster Backend");
                p1.setProjectOwner("Admin");
                p1.setGitUrl("https://github.com/opster/backend");
                p1.setBusinessLine("Infrastructure");
                p1.setStatus(Status.DISABLED);
                projectRepository.save(p1);

                Project p2 = new Project();
                p2.setProjectName("Opster Frontend");
                p2.setProjectOwner("Admin");
                p2.setGitUrl("https://github.com/opster/frontend");
                p2.setBusinessLine("Infrastructure");
                p2.setStatus(Status.DISABLED);
                projectRepository.save(p2);
            }

            // Init Servers
            if (serverRepository.count() == 0) {
                Server s1 = new Server();
                s1.setIp("192.168.1.101");
                s1.setAlias("App Server 01");
                s1.setUsername("opuser");
                s1.setEnv("prod");
                s1.setStatus(Status.ENABLED);
                serverRepository.save(s1);

                Server s2 = new Server();
                s2.setIp("192.168.1.102");
                s2.setAlias("App Server 02");
                s2.setUsername("opuser");
                s2.setEnv("test");
                s2.setStatus(Status.ENABLED);
                serverRepository.save(s2);
            }

            // Init Services
            if (appServiceRepository.count() == 0 && projectRepository.count() > 0 && serverRepository.count() > 0) {
                Project p = projectRepository.findAll().get(0);
                Server s = serverRepository.findAll().get(0);

                AppService service = new AppService();
                service.setProjectId(p.getId());
                service.setServerId(s.getId());
                service.setGitBranch("main");
                service.setDeployPath("/data/apps/opster-backend");
                service.setLogPath("/data/logs/opster/app.log");
                service.setStatus(Status.ENABLED);
                service.setMavenCmd("mvn clean package");
                service.setStartScript("./start.sh");
                appServiceRepository.save(service);
            }
        };
    }
}
