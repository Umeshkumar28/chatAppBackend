package com.example.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String ONLINE_USERS_KEY = "online_users";

    public void markUserOnline(String username) {
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, username);
        messagingTemplate.convertAndSend("/topic/presence",
                Map.of("userId", username, "status", "ONLINE"));
    }

    public void markUserOffline(String username) {
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, username);
        messagingTemplate.convertAndSend("/topic/presence",
                Map.of("userId", username, "status", "OFFLINE"));
    }

    public boolean isUserOnline(String username) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, username));
    }

    // For explicit logout calls
    public void markOffline(String username) {
        markUserOffline(username);
    }
}
