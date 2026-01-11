package com.example.chat.repository;

import com.example.chat.entity.Doctor;
import com.example.chat.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, UUID> {
    List<DoctorAvailability> findByDoctorAndDateAndIsAvailableTrue(Doctor doctor, LocalDate date);
    List<DoctorAvailability> findByDoctorAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqualAndIsAvailableTrue(
            Doctor doctor, LocalDate date, LocalTime startTime, LocalTime endTime);
    List<DoctorAvailability> findByDoctor(Doctor doctor);
    List<DoctorAvailability> findByDateAndIsAvailableTrue(LocalDate date);
}
