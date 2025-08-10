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
        Optional<User> reciever = userRepository.findById(messageDto.getTo());
        // Save to DB
        Message msg = Message.builder()
                .chatRoom(chatRoom)
                .sender(sender.orElseThrow(() -> new RuntimeException("sender not found")))
                .receiver(reciever.orElseThrow(() -> new RuntimeException("reciever not found")))
                .content(messageDto.getContent())
                .status(MessageStatus.SENT)
                .messageType(messageDto.getMessageType())
                .build();
        messageRepository.save(msg);

        // Deliver or mark as pending
        if (presenceService.isUserOnline(String.valueOf(messageDto.getTo()))) {
            msg.setStatus(MessageStatus.DELIVERED);
            messageRepository.save(msg);
            chatService.deliverMessage(messageDto);
        } else {
            msg.setStatus(MessageStatus.PENDING);
            messageRepository.save(msg);
        }
    }
}
