package com.example.chat.service;

import com.example.chat.dto.ChatMessageDto;
import com.example.chat.entity.ChatRoom;
import com.example.chat.entity.Message;
import com.example.chat.entity.MessageStatus;
import com.example.chat.entity.User;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageProcessor {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final PresenceService presenceService;
    private final ChatService chatService;


    public void processIncomingMessage(ChatMessageDto messageDto) {
        ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getChatRoomId())
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));
        Optional<User> sender = userRepository.findById(messageDto.getFrom());
        Optional<User> receiver = userRepository.findById(messageDto.getTo());
        
        User senderUser = sender.orElseThrow(() -> new RuntimeException("sender not found"));
        User receiverUser = receiver.orElseThrow(() -> new RuntimeException("receiver not found"));
        
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Save to DB
        Message msg = Message.builder()
                .chatRoom(chatRoom)
                .sender(senderUser)
                .receiver(receiverUser)
                .content(messageDto.getContent())
                .timestamp(timestamp)
                .status(MessageStatus.SENT)
                .messageType(messageDto.getMessageType())
                .build();
        messageRepository.save(msg);

        // Set timestamp in the DTO for delivery
        messageDto.setTimestamp(timestamp);

        // Deliver or mark as pending
        if (presenceService.isUserOnline(receiverUser.getUsername())) {
            msg.setStatus(MessageStatus.DELIVERED);
            messageRepository.save(msg);
            messageDto.setStatus(MessageStatus.DELIVERED);
            chatService.deliverMessage(messageDto);
        } else {
            msg.setStatus(MessageStatus.PENDING);
            messageRepository.save(msg);
            messageDto.setStatus(MessageStatus.PENDING);
        }
    }
}
