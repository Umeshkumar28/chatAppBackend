package com.example.chat.config;

import com.example.chat.service.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketUserPresenceInterceptor implements ChannelInterceptor {

    private static final String ONLINE_USERS_KEY = "online_users";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String username = jwtProvider.extractUsername(token);
                    redisTemplate.opsForSet().add(ONLINE_USERS_KEY, username);
                } catch (Exception ignored) {}
            }
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            // Optionally mark offline on disconnect if desired
            // String username = ... (requires session tracking)
        }
        return message;
    }
}

