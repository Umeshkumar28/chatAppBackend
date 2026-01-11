package com.example.chat.service;

import com.example.chat.dto.ChatMessageDto;
import com.example.chat.dto.MessageStatusUpdateDto;
import com.example.chat.dto.OutgoingChatMessageDto;
import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final KafkaTemplate<String, ChatMessageDto> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public void sendMessage(ChatMessageDto messageDto) {
        kafkaTemplate.send("chat.messages", messageDto.getChatRoomId().toString(), messageDto);
    }

    public void broadcastStatus(MessageStatusUpdateDto statusDto, String chatRoomId) {
        messagingTemplate.convertAndSend("/topic/status/" + chatRoomId, statusDto);
    }

    public void deliverMessage(ChatMessageDto messageDto) {
        // Convert to outgoing format for frontend
        OutgoingChatMessageDto outgoingMessage = convertToOutgoingFormat(messageDto);
        messagingTemplate.convertAndSend("/topic/messages/" + messageDto.getChatRoomId(), outgoingMessage);
    }
    
    private OutgoingChatMessageDto convertToOutgoingFormat(ChatMessageDto messageDto) {
        User sender = userRepository.findById(messageDto.getFrom())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(messageDto.getTo())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        
        OutgoingChatMessageDto outgoing = new OutgoingChatMessageDto();
        outgoing.setChatRoomId(messageDto.getChatRoomId());
        outgoing.setContent(messageDto.getContent());
        outgoing.setTimestamp(messageDto.getTimestamp());
        outgoing.setStatus(messageDto.getStatus());
        outgoing.setMessageType(messageDto.getMessageType());
        
        // Set sender info
        OutgoingChatMessageDto.UserInfo senderInfo = new OutgoingChatMessageDto.UserInfo();
        senderInfo.setId(sender.getId());
        senderInfo.setUsername(sender.getUsername());
        outgoing.setFrom(senderInfo);
        
        // Set receiver info
        OutgoingChatMessageDto.UserInfo receiverInfo = new OutgoingChatMessageDto.UserInfo();
        receiverInfo.setId(receiver.getId());
        receiverInfo.setUsername(receiver.getUsername());
        outgoing.setTo(receiverInfo);
        
        return outgoing;
    }
}
