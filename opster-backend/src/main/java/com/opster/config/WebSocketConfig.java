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
    private LogWebSocketHandler logWebSocketHandler;

    @Autowired
    private ExecWebSocketHandler execWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logWebSocketHandler, "/ws/log/{serviceId}")
                .setAllowedOrigins("*");
        registry.addHandler(execWebSocketHandler, "/ws/exec/{serviceId}/{action}")
                .setAllowedOrigins("*");
    }
}
