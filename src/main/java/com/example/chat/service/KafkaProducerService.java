package com.example.chat.service;

import com.example.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, ChatMessageDto> kafkaTemplate;

    public void sendMessage(ChatMessageDto message) {
        kafkaTemplate.send("chat.messages", message.getChatRoomId().toString(), message);
    }
}
