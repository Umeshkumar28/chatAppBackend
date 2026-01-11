package com.example.chat.service;

import com.example.chat.dto.ChatRoomWithHistoryDto;
import com.example.chat.entity.ChatRoom;
import com.example.chat.entity.Message;
import com.example.chat.entity.User;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ChatRoomWithHistoryDto getOrCreateChatRoom(String currentUsername, String targetUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        // Ensure deterministic user pair order to avoid duplicates
        ChatRoom room = chatRoomRepository
                .findByUserPair(currentUser.getId(), targetUser.getId())
                .orElseGet(() -> {
                    ChatRoom newRoom = new ChatRoom();
                    newRoom.setUser1(currentUser);
                    newRoom.setUser2(targetUser);
                    return chatRoomRepository.save(newRoom);
                });

        // Load last 50 messages by timestamp
        List<Message> history = messageRepository
                .findTop50ByChatRoomOrderByTimestampAsc(room);

        return new ChatRoomWithHistoryDto(room.getId(), history);
    }
}
