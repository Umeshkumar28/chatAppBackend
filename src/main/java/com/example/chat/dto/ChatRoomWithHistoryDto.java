package com.example.chat.dto;

import com.example.chat.entity.Message;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomWithHistoryDto {
    private UUID chatRoomId;
    private List<Message> history;
}
