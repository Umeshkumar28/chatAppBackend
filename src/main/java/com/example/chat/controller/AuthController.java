package com.example.chat.controller;

import com.example.chat.dto.AuthResponse;
import com.example.chat.dto.LoginRequest;
import com.example.chat.dto.RegisterRequest;
import com.example.chat.service.AuthService;
import com.example.chat.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final AuthService authService;
    private final PresenceService presenceService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        // Do not mark online here; presence is tracked via WebSocket CONNECT
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            presenceService.markOffline(username);
        }
        return ResponseEntity.ok().body("Logged out");
    }
}
