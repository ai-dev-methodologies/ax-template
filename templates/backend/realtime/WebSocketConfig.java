/**
 * @ax-template-meta
 * template_id: backend/realtime/WebSocketConfig
 * layer: backend-cross-cutting
 * provenance_class: internal_design
 * opt_in_via: ax.realtime.websocket.enabled=true (blueprints/realtime-policy-manifest.yaml)
 * serverless_safe: false
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — WebSocket (Web on Servlet Stack §5)"
 *     url: "https://docs.spring.io/spring-framework/reference/web/websocket.html"
 *   - source_type: external
 *     citation: "Spring Framework Reference — STOMP (Web on Servlet Stack §5.4)"
 *     url: "https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html"
 * usage: |
 *   STOMP over WebSocket fallback for environments where SSE is blocked by reverse proxies.
 *   Opt-in: requires ax.realtime.websocket.enabled=true.
 *   NOT for serverless runtimes — same constraint as SSE. See blueprints/realtime-policy-manifest.yaml.
 *
 *   1. Enable via application.yaml: ax.realtime.websocket.enabled=true
 *   2. Replace 'com.example.app' with your base package.
 *   3. Client connects to /ws/stomp endpoint; subscribes to /topic/<name> or /user/queue/<name>.
 *   4. Add SimpMessagingTemplate injection to send messages from service layer.
 *
 * ## Serverless Deployment
 * WebSocket connections are long-lived. Serverless platforms kill them on function timeout.
 * Use the polling default (ax.realtime.websocket.enabled=false) on serverless platforms.
 */
package com.example.app.realtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket configuration — opt-in fallback transport.
 *
 * <p>Provides bidirectional messaging via STOMP protocol over WebSocket.
 * Use when SSE is blocked by reverse proxies (some proxies buffer chunked responses,
 * which breaks SSE delivery). WebSocket bypasses this by using the WS/WSS protocol upgrade.
 *
 * <p>Client topics:
 * <ul>
 *   <li>{@code /topic/notification} — broadcast to all subscribers
 *   <li>{@code /user/queue/notification} — directed to a single user session
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(name = "ax.realtime.websocket.enabled", havingValue = "true")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker for /topic/** (broadcast) and /user/** (targeted)
        registry.enableSimpleBroker("/topic", "/user");
        // Application destination prefix for @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        // User destination prefix for SimpMessagingTemplate.convertAndSendToUser(...)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/ws/stomp")
            .setAllowedOriginPatterns("*")   // tighten in production via ax.cors.allowed-origins
            .withSockJS();                   // SockJS fallback for browsers without WS support
    }
}
