package com.example.chat.dto;

import com.example.chat.entity.MessageStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Data
@Getter
@Setter
public class StatusUpdateDto {
    private UUID messageId;
    private MessageStatus status; // SENT, DELIVERED, READ
}
