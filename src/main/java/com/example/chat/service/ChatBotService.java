package com.example.chat.service;

import org.springframework.stereotype.Service;

@Service
public class ChatBotService {
    public boolean detectHandover(String message) {
        return message != null && message.toLowerCase().contains("i need a human");
    }

    public void assignHumanAgent(String chatRoomId) {
        // Placeholder logic for assigning an available agent
        System.out.println("Assigning human agent to chat room: " + chatRoomId);
    }
}
