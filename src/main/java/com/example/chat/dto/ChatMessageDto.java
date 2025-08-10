package com.example.chat.dto;

import com.example.chat.entity.MessageStatus;
import com.example.chat.entity.MessageType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ChatMessageDto {
    private UUID chatRoomId;
    private UUID from;
    private UUID to;
    private String content;
    private LocalDateTime timestamp;
    private MessageStatus status;
    private MessageType messageType;
}
