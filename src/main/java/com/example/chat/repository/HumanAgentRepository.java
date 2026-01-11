package com.example.chat.repository;

import com.example.chat.entity.HumanAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HumanAgentRepository extends JpaRepository<HumanAgent, UUID> {
    List<HumanAgent> findByIsAvailableTrue();
    Optional<HumanAgent> findByEmail(String email);
}
