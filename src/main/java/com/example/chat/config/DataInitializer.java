package com.example.chat.config;

import com.example.chat.entity.*;
import com.example.chat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final HumanAgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeBotUser();
        initializeDoctors();
        initializeHumanAgents();
    }

    private void initializeBotUser() {
        if (userRepository.findByUsername("DoctorAssistant").isEmpty()) {
            User bot = User.builder()
                    .username("DoctorAssistant")
                    .password(passwordEncoder.encode("bot-password-not-used"))
                    .firstName("Doctor")
                    .lastName("Assistant")
                    .isBot(true)
                    .build();
            userRepository.save(bot);
            log.info("DoctorAssistant bot user created");
        }
    }

    private void initializeDoctors() {
        if (doctorRepository.count() == 0) {
            // Create sample doctors
            Doctor drSmith = Doctor.builder()
                    .name("Dr. Smith")
                    .specialty("Cardiology")
                    .description("Expert in heart diseases and cardiovascular health")
                    .build();
            doctorRepository.save(drSmith);

            Doctor drJones = Doctor.builder()
                    .name("Dr. Jones")
                    .specialty("Dermatology")
                    .description("Specialist in skin conditions and dermatological treatments")
                    .build();
            doctorRepository.save(drJones);

            Doctor drWilliams = Doctor.builder()
                    .name("Dr. Williams")
                    .specialty("Orthopedics")
                    .description("Expert in bone, joint, and muscle conditions")
                    .build();
            doctorRepository.save(drWilliams);

            Doctor drBrown = Doctor.builder()
                    .name("Dr. Brown")
                    .specialty("General Medicine")
                    .description("General practitioner for common health issues")
                    .build();
            doctorRepository.save(drBrown);

            // Create availability for today, tomorrow, and next few days
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            LocalDate dayAfter = today.plusDays(2);
            LocalDate day3 = today.plusDays(3);
            LocalDate day4 = today.plusDays(4);

            // Dr. Smith availability
            createAvailability(drSmith, today, LocalTime.of(10, 0), LocalTime.of(12, 0));
            createAvailability(drSmith, tomorrow, LocalTime.of(9, 0), LocalTime.of(12, 0));
            createAvailability(drSmith, tomorrow, LocalTime.of(14, 0), LocalTime.of(17, 0));
            createAvailability(drSmith, dayAfter, LocalTime.of(10, 0), LocalTime.of(13, 0));
            createAvailability(drSmith, day3, LocalTime.of(9, 0), LocalTime.of(11, 0));

            // Dr. Jones availability
            createAvailability(drJones, today, LocalTime.of(14, 0), LocalTime.of(17, 0));
            createAvailability(drJones, tomorrow, LocalTime.of(10, 0), LocalTime.of(13, 0));
            createAvailability(drJones, dayAfter, LocalTime.of(9, 0), LocalTime.of(12, 0));
            createAvailability(drJones, day3, LocalTime.of(10, 0), LocalTime.of(13, 0));

            // Dr. Williams availability
            createAvailability(drWilliams, today, LocalTime.of(9, 0), LocalTime.of(11, 0));
            createAvailability(drWilliams, tomorrow, LocalTime.of(8, 0), LocalTime.of(11, 0));
            createAvailability(drWilliams, tomorrow, LocalTime.of(15, 0), LocalTime.of(18, 0));
            createAvailability(drWilliams, dayAfter, LocalTime.of(9, 0), LocalTime.of(12, 0));
            createAvailability(drWilliams, day3, LocalTime.of(14, 0), LocalTime.of(17, 0));

            // Dr. Brown availability (most available)
            createAvailability(drBrown, today, LocalTime.of(9, 0), LocalTime.of(17, 0));
            createAvailability(drBrown, tomorrow, LocalTime.of(9, 0), LocalTime.of(17, 0));
            createAvailability(drBrown, dayAfter, LocalTime.of(9, 0), LocalTime.of(17, 0));
            createAvailability(drBrown, day3, LocalTime.of(9, 0), LocalTime.of(17, 0));
            createAvailability(drBrown, day4, LocalTime.of(9, 0), LocalTime.of(17, 0));

            log.info("Sample doctors and availability created");
        }
    }

    private void createAvailability(Doctor doctor, LocalDate date, LocalTime start, LocalTime end) {
        DoctorAvailability availability = DoctorAvailability.builder()
                .doctor(doctor)
                .date(date)
                .startTime(start)
                .endTime(end)
                .isAvailable(true)
                .slotDurationMinutes(30)
                .build();
        availabilityRepository.save(availability);
    }

    private void initializeHumanAgents() {
        if (agentRepository.count() == 0) {
            HumanAgent agent1 = HumanAgent.builder()
                    .name("Sarah Johnson")
                    .email("sarah.johnson@superclinic.com")
                    .isAvailable(true)
                    .build();
            agentRepository.save(agent1);

            HumanAgent agent2 = HumanAgent.builder()
                    .name("Michael Chen")
                    .email("michael.chen@superclinic.com")
                    .isAvailable(true)
                    .build();
            agentRepository.save(agent2);

            log.info("Human agents initialized");
        }
    }
}
