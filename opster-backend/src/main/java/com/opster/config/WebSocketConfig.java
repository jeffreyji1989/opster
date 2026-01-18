package com.opster.config;

import com.opster.handler.ExecWebSocketHandler;
import com.opster.handler.LogWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ExecWebSocketHandler execWebSocketHandler;

    @Autowired
    private LogWebSocketHandler logWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 ExecWebSocketHandler
        registry.addHandler(execWebSocketHandler, "/ws/exec/**")
                .setAllowedOrigins("*");
        
        // 注册 LogWebSocketHandler
        registry.addHandler(logWebSocketHandler, "/ws/log/**")
                .setAllowedOrigins("*");
    }
}
