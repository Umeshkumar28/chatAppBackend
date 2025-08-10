package com.example.chat.repository;

import com.example.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
    @Query("SELECT cr FROM ChatRoom cr WHERE " +
            "(cr.user1.id = :id1 AND cr.user2.id = :id2) OR " +
            "(cr.user1.id = :id2 AND cr.user2.id = :id1)")
    Optional<ChatRoom> findByUserPair(UUID id1, UUID id2);
    Optional<ChatRoom> findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);
    List<ChatRoom> findByUser1IdOrUser2Id(UUID user1Id, UUID user2Id);
}
