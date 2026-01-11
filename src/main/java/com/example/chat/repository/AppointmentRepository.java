package com.example.chat.repository;

import com.example.chat.entity.Appointment;
import com.example.chat.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByDoctorAndAppointmentDateTimeBetween(Doctor doctor, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByChatRoomId(UUID chatRoomId);
    List<Appointment> findByPatientNameIgnoreCase(String patientName);
    List<Appointment> findByPatient_UsernameIgnoreCase(String username);
}
