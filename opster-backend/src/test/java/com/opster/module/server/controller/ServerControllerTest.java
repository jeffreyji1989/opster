//package com.opster.module.server.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.opster.module.server.entity.Server;
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
//public class ServerControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Test
//    @Order(1)
//    public void testSaveServer() throws Exception {
//        Server server = new Server();
//        server.setIp("192.168.1.100");
//        server.setAlias("Test Server");
//        server.setUsername("root");
//        server.setPassword("123456");
//        server.setGroupName("Dev");
//        server.setEnv("test");
//        server.setStatus(1);
//
//        mockMvc.perform(post("/api/server")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(server)))
//                .andExpect(status().isOk())
//                .andExpect(content().string("true"));
//    }
//
//    @Test
//    @Order(2)
//    public void testListServers() throws Exception {
//        mockMvc.perform(get("/api/server/list"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].ip").value("192.168.1.100"));
//    }
//
//    @Test
//    @Order(3)
//    public void testUpdateServer() throws Exception {
//        Server server = new Server();
//        server.setId(1);
//        server.setAlias("Updated Server");
//
//        mockMvc.perform(put("/api/server")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(server)))
//                .andExpect(status().isOk())
//                .andExpect(content().string("true"));
//    }
//
//    @Test
//    @Order(4)
//    public void testGetServerById() throws Exception {
//        mockMvc.perform(get("/api/server/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.alias").value("Updated Server"));
//    }
//
//    @Test
//    @Order(5)
//    public void testDeleteServer() throws Exception {
//        mockMvc.perform(delete("/api/server/1"))
//                .andExpect(status().isOk())
//                .andExpect(content().string("true"));
//    }
//}
