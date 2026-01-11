package com.example.chat.controller;

import com.example.chat.dto.ChatMessageDto;
import com.example.chat.dto.IncomingChatMessageDto;
import com.example.chat.dto.StatusUpdateDto;
import com.example.chat.entity.User;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.UserRepository;
import com.example.chat.service.MessageProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final MessageProcessor messageProcessor;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @MessageMapping("/chat.send")
    public void sendMessage(IncomingChatMessageDto incomingMessage) {
        // Convert incoming message to internal format
        User sender = userRepository.findByUsername(incomingMessage.getFrom().getUsername())
                .orElseThrow(() -> new RuntimeException("Sender not found: " + incomingMessage.getFrom().getUsername()));
        
        User receiver = userRepository.findByUsername(incomingMessage.getTo())
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + incomingMessage.getTo()));
        
        ChatMessageDto messageDto = new ChatMessageDto();
        messageDto.setChatRoomId(incomingMessage.getChatRoomId());
        messageDto.setFrom(sender.getId());
        messageDto.setTo(receiver.getId());
        messageDto.setContent(incomingMessage.getContent());
        messageDto.setMessageType(incomingMessage.getMessageType());
        
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
