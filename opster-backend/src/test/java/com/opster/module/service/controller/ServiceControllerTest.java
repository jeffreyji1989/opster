//package com.opster.module.service.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.opster.module.project.entity.Project;
//import com.opster.module.project.service.ProjectService;
//import com.opster.module.server.entity.Server;
//import com.opster.module.server.service.ServerService;
//import com.opster.module.service.entity.AppService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.MethodOrderer;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//public class ServiceControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private ProjectService projectService;
//
//    @Autowired
//    private ServerService serverService;
//
//    @BeforeEach
//    public void setup() {
//        // Ensure dependencies exist
//        if (projectService.count() == 0) {
//            Project p = new Project();
//            p.setProjectName("Dependency Project");
//            projectService.save(p);
//        }
//        if (serverService.count() == 0) {
//            Server s = new Server();
//            s.setIp("127.0.0.1");
//            serverService.save(s);
//        }
//    }
//
//    @Test
//    @Order(1)
//    public void testSaveService() throws Exception {
//        AppService service = new AppService();
//        service.setProjectId(1);
//        service.setServerId(1);
//        service.setGitBranch("main");
//        service.setDeployPath("/opt/app");
//        service.setStatus(0);
//        service.setStartScript("./start.sh");
//
//        mockMvc.perform(post("/api/service")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(service)))
//                .andExpect(status().isOk())
//                .andExpect(content().string("true"));
//    }
//
//    @Test
//    @Order(2)
//    public void testListServices() throws Exception {
//        mockMvc.perform(get("/api/service/list"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].deployPath").value("/opt/app"));
//    }
//
//    @Test
//    @Order(3)
//    public void testServiceActions() throws Exception {
//        // Test Start
//        mockMvc.perform(post("/api/service/1/start"))
//                .andExpect(status().isOk())
//                .andExpect(content().string(org.hamcrest.Matchers.containsString("Starting service")));
//
//        // Test Restart
//        mockMvc.perform(post("/api/service/1/restart"))
//                .andExpect(status().isOk())
//                .andExpect(content().string(org.hamcrest.Matchers.containsString("Restarting service")));
//
//        // Test Compile and Restart
//        mockMvc.perform(post("/api/service/1/compile-restart"))
//                .andExpect(status().isOk())
//                .andExpect(content().string(org.hamcrest.Matchers.containsString("Executing Maven Command")));
//    }
//
//    @Test
//    @Order(4)
//    public void testDeleteService() throws Exception {
//        mockMvc.perform(delete("/api/service/1"))
//                .andExpect(status().isOk())
//                .andExpect(content().string("true"));
//    }
//}
