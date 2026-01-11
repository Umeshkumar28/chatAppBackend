package com.example.chat.service;

import com.example.chat.dto.UserDto;
import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    public List<UserDto> listUsers() {
        List<UserDto> allUsers = userRepository.findAll().stream()
                .map(u -> UserDto.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .isBot(u.getIsBot())
                        .build())
                .collect(Collectors.toList());
        
        // Sort: bots first (DoctorAssistant at top), then regular users
        allUsers.sort((u1, u2) -> {
            boolean u1IsBot = Boolean.TRUE.equals(u1.getIsBot());
            boolean u2IsBot = Boolean.TRUE.equals(u2.getIsBot());
            
            if (u1IsBot && !u2IsBot) return -1;
            if (!u1IsBot && u2IsBot) return 1;
            if (u1IsBot && u2IsBot) {
                // If both are bots, DoctorAssistant comes first
                if ("DoctorAssistant".equals(u1.getUsername())) return -1;
                if ("DoctorAssistant".equals(u2.getUsername())) return 1;
            }
            return u1.getUsername().compareTo(u2.getUsername());
        });
        
        return allUsers;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPassword())
                .authorities("USER")
                .build();
    }
}
