package com.example.chat.dto;

import com.example.chat.entity.MessageStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class MessageStatusUpdateDto {
    private UUID messageId;
    private MessageStatus status;
}
