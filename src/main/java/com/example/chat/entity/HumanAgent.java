package com.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "human_agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanAgent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "current_chat_room_id")
    private UUID currentChatRoomId; // Track which chat room they're handling
}
