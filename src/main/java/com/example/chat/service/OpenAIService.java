package com.example.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getChatCompletion(String userMessage, List<Map<String, Object>> functions, List<Map<String, Object>> conversationHistory, String systemPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", buildMessages(conversationHistory, userMessage, systemPrompt));
            requestBody.put("functions", functions);
            requestBody.put("function_call", "auto");
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            
            // Check if function call was requested
            JsonNode message = responseJson.get("choices").get(0).get("message");
            if (message.has("function_call")) {
                return "FUNCTION_CALL:" + message.get("function_call").toString();
            }
            
            // Return regular message
            return message.get("content").asText();
        } catch (Exception e) {
            log.error("Error calling OpenAI API: ", e);
            return "I apologize, but I'm experiencing technical difficulties. Please try again later.";
        }
    }

    private List<Map<String, Object>> buildMessages(List<Map<String, Object>> history, String userMessage, String systemPrompt) {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // Add system message with custom prompt
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt != null ? systemPrompt : getDefaultSystemPrompt());
        messages.add(systemMsg);
        
        // Add conversation history
        if (history != null) {
            messages.addAll(history);
        }
        
        // Add current user message
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        
        return messages;
    }

    private String getDefaultSystemPrompt() {
        return "You are DoctorAssistant, a helpful chatbot for Super Clinic. " +
                "Today's date is " + LocalDate.now().toString() + ". " +
                "You help patients book appointments with doctors, find doctors by specialty, " +
                "and answer questions about doctor availability. Be friendly, professional, and concise.";
    }

    public String getSimpleCompletion(String userMessage, List<Map<String, Object>> conversationHistory, String systemPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", buildMessages(conversationHistory, userMessage, systemPrompt));
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return responseJson.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            log.error("Error calling OpenAI API: ", e);
            return "I apologize, but I'm experiencing technical difficulties. Please try again later.";
        }
    }
}
