package com.example.chat.controller;

import com.example.chat.dto.ChatMessageDto;
import com.example.chat.dto.MessageStatusUpdateDto;
import com.example.chat.dto.StatusUpdateDto;
import com.example.chat.repository.MessageRepository;
import com.example.chat.service.ChatService;
import com.example.chat.service.MessageProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final MessageProcessor messageProcessor;
    private final ChatService chatService;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageDto messageDto) {
        messageProcessor.processIncomingMessage(messageDto);
    }

    @MessageMapping("/chat.updateStatus")
    public void updateMessageStatus(StatusUpdateDto statusUpdate) {
        messageRepository.findById(statusUpdate.getMessageId()).ifPresent(msg -> {
            msg.setStatus(statusUpdate.getStatus());
            messageRepository.save(msg);

            messagingTemplate.convertAndSend(
                    "/topic/status/" + msg.getChatRoom().getId(),
                    statusUpdate
            );
        });
    }
}
