package com.example.chat.dto;

import com.example.chat.entity.MessageStatus;
import com.example.chat.entity.MessageType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OutgoingChatMessageDto {
    private UUID chatRoomId;
    private UserInfo from;
    private UserInfo to;
    private String content;
    private LocalDateTime timestamp;
    private MessageStatus status;
    private MessageType messageType;
    
    @Data
    public static class UserInfo {
        private UUID id;
        private String username;
    }
}
