package com.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private User patient; // Can be null if patient info not collected yet

    @Column(name = "patient_name", length = 100)
    private String patientName;

    @Column(name = "patient_phone", length = 20)
    private String patientPhone;

    @Column(name = "patient_email", length = 100)
    private String patientEmail;

    @Column(name = "appointment_date_time", nullable = false)
    private LocalDateTime appointmentDateTime;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "BOOKED"; // BOOKED, CANCELLED, COMPLETED

    @Column(name = "chat_room_id")
    private UUID chatRoomId; // Link to the chat room where appointment was booked

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
