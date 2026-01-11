package com.example.chat.dto;

import com.example.chat.entity.MessageType;
import lombok.Data;

import java.util.UUID;

@Data
public class IncomingChatMessageDto {
    private UUID chatRoomId;
    private UserInfo from;
    private String to; // username
    private String content;
    private MessageType messageType;
    
    @Data
    public static class UserInfo {
        private String username;
    }
}
