package com.sicmagroup.gpr.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

        @Override
        public void configureMessageBroker(MessageBrokerRegistry registry) {
            registry.setApplicationDestinationPrefixes("/api/v1");
            registry.enableSimpleBroker("/topic");
            // registry.setUserDestinationPrefix("/secured/session");

        }

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            // registry.addEndpoint("/ws").setAllowedOrigins("http://127.0.0.1:3000","http://localhost:3000", "http://localhost:3001", "http://196.168.30.157","http://192.168.100.5:81", "http://192.168.100.5",
            // "http://localhost:9195", "http://localhost:8080", "http://localhost:81", "http://196.168.30.157:81", "https://196.168.30.157", "https://196.168.30.157:81", "https://196.168.30.157:443", "https://196.168.30.157:444",
            //  "https://gpsassilassime.sicmagroup.com").withSockJS();
            registry.addEndpoint("/ws").setAllowedOrigins("https://gpr-sicma:9001").withSockJS();
            // registry.addEndpoint("/message").setAllowedOrigins("http://localhost:3000", "http://localhost:3001").withSockJS();
            
        }

}
