package com.example.chat.controller;

import com.example.chat.dto.ChatRoomWithHistoryDto;
import com.example.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping("/chatroom/{username}")
    public ResponseEntity<?> getOrCreateChatRoom(@PathVariable String username, Authentication auth) {
        String currentUsername = auth.getName();
        ChatRoomWithHistoryDto dto = chatRoomService.getOrCreateChatRoom(currentUsername, username);
        return ResponseEntity.ok(dto);
    }
}

