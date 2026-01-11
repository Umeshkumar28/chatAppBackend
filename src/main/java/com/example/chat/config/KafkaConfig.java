package com.example.chat.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic chatMessagesTopic() {
        return new NewTopic("chat.messages", 1, (short) 1);
    }
}
