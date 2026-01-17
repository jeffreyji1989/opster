//package com.opster.module.project.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.opster.module.project.entity.Project;
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
//public class ProjectControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Test
//    @Order(1)
//    public void testSaveProject() throws Exception {
//        Project project = new Project();
//        project.setProjectName("Test Project");
//        project.setProjectOwner("Tester");
//        project.setGitUrl("http://git.test.com");
//        project.setBusinessLine("Core");
//        project.setStatus(1);
//
//        mockMvc.perform(post("/api/project")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(project)))
//                .andExpect(status().isOk())
//                .andExpect(content().string("true"));
//    }
//
//    @Test
//    @Order(2)
//    public void testListProjects() throws Exception {
//        mockMvc.perform(get("/api/project/list"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].projectName").value("Test Project"));
//    }
//
//    @Test
//    @Order(3)
//    public void testUpdateProject() throws Exception {
//        Project project = new Project();
//        project.setId(1);
//        project.setProjectName("Updated Project");
//
//        mockMvc.perform(put("/api/project")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(project)))
//                .andExpect(status().isOk())
//                .andExpect(content().string("true"));
//    }
//
//    @Test
//    @Order(4)
//    public void testGetProjectById() throws Exception {
//        mockMvc.perform(get("/api/project/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.projectName").value("Updated Project"));
//    }
//
//    @Test
//    @Order(5)
//    public void testDeleteProject() throws Exception {
//        mockMvc.perform(delete("/api/project/1"))
//                .andExpect(status().isOk())
//                .andExpect(content().string("true"));
//    }
//}
