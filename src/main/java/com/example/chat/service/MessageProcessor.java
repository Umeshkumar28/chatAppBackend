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
    private final ChatBotService chatBotService;


    public void processIncomingMessage(ChatMessageDto messageDto) {
        ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getChatRoomId())
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));
        Optional<User> sender = userRepository.findById(messageDto.getFrom());
        Optional<User> receiver = userRepository.findById(messageDto.getTo());
        
        User senderUser = sender.orElseThrow(() -> new RuntimeException("sender not found"));
        User receiverUser = receiver.orElseThrow(() -> new RuntimeException("receiver not found"));
        
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Truncate content if too long (safety check)
        String content = messageDto.getContent();
        if (content != null && content.length() > 10000) {
            content = content.substring(0, 10000) + "... [message truncated]";
        }
        
        // Save to DB
        Message msg = Message.builder()
                .chatRoom(chatRoom)
                .sender(senderUser)
                .receiver(receiverUser)
                .content(content)
                .timestamp(timestamp)
                .status(MessageStatus.SENT)
                .messageType(messageDto.getMessageType())
                .build();
        messageRepository.save(msg);

        // Set timestamp in the DTO for delivery
        messageDto.setTimestamp(timestamp);

        // Check if receiver is the bot
        if (Boolean.TRUE.equals(receiverUser.getIsBot()) && "DoctorAssistant".equals(receiverUser.getUsername())) {
            // Process bot message and generate response
            String botResponse = chatBotService.processBotMessage(
                    messageDto.getContent(),
                    messageDto.getChatRoomId(),
                    senderUser.getUsername()
            );
            
            // Truncate if too long (safety check - TEXT can hold up to 65KB, but let's limit to 10KB for safety)
            if (botResponse != null && botResponse.length() > 10000) {
                botResponse = botResponse.substring(0, 10000) + "... [message truncated]";
            }
            
            // Save bot response as a message
            Message botMsg = Message.builder()
                    .chatRoom(chatRoom)
                    .sender(receiverUser)
                    .receiver(senderUser)
                    .content(botResponse)
                    .timestamp(LocalDateTime.now())
                    .status(MessageStatus.DELIVERED) // Bot messages are always delivered
                    .messageType(messageDto.getMessageType())
                    .build();
            messageRepository.save(botMsg);
            
            // Create DTO for bot response
            ChatMessageDto botResponseDto = new ChatMessageDto();
            botResponseDto.setChatRoomId(messageDto.getChatRoomId());
            botResponseDto.setFrom(receiverUser.getId());
            botResponseDto.setTo(senderUser.getId());
            botResponseDto.setContent(botResponse);
            botResponseDto.setTimestamp(botMsg.getTimestamp());
            botResponseDto.setStatus(MessageStatus.DELIVERED);
            botResponseDto.setMessageType(messageDto.getMessageType());
            
            // Deliver bot response
            chatService.deliverMessage(botResponseDto);
        }
        
        // Deliver or mark as pending for regular messages
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
