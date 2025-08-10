package com.example.chat.service;

import com.example.chat.dto.ChatMessageDto;
import com.example.chat.dto.MessageStatusUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final KafkaTemplate<String, ChatMessageDto> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessage(ChatMessageDto messageDto) {
        kafkaTemplate.send("chat.messages", messageDto.getChatRoomId().toString(), messageDto);
    }

    public void broadcastStatus(MessageStatusUpdateDto statusDto, String chatRoomId) {
        messagingTemplate.convertAndSend("/topic/status/" + chatRoomId, statusDto);
    }

    public void deliverMessage(ChatMessageDto messageDto) {
        messagingTemplate.convertAndSend("/topic/messages/" + messageDto.getChatRoomId(), messageDto);
    }
}
