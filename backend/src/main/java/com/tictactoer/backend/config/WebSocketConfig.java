package com.tictactoer.backend.config;

import com.tictactoer.backend.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-game")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:5173")
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

                        if (request instanceof ServletServerHttpRequest servletRequest) {
                            HttpServletRequest req = servletRequest.getServletRequest();
                            Cookie[] cookies = req.getCookies();

                            if (cookies != null) {
                                for (Cookie cookie : cookies) {
                                    if ("jwt".equals(cookie.getName())) {
                                        String token = cookie.getValue();

                                        if (jwtService.isTokenValid(token)) {
                                            String username = jwtService.extractUsername(token);
                                            return new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                                        }
                                    }
                                }
                            }
                        }
                        return null;
                    }
                });
    }
}