package com.example.chat.service;

import com.example.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final MessageProcessor messageProcessor;

    @KafkaListener(topics = "chat.messages", groupId = "chat-group")
    public void consume(ChatMessageDto message) {
        messageProcessor.processIncomingMessage(message);
    }
}
