package com.example.chat.repository;

import com.example.chat.entity.ChatRoom;
import com.example.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findTop50ByChatRoomOrderByTimestampAsc(ChatRoom chatRoom);
    List<Message> findByChatRoomIdOrderByTimestampAsc(UUID chatRoomId);
}
