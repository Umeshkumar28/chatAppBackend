package com.example.chat.repository;

import com.example.chat.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    List<Doctor> findBySpecialty(String specialty);
    Optional<Doctor> findByName(String name);
}
