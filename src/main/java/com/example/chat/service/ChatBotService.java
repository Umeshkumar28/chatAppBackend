package com.example.chat.service;

import com.example.chat.entity.*;
import com.example.chat.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final HumanAgentRepository agentRepository;
    private final UserRepository userRepository;
    private final OpenAIService openAIService;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Store conversation history per chat room
    private final Map<UUID, List<Map<String, Object>>> conversationHistory = new HashMap<>();

    public String processBotMessage(String userMessage, UUID chatRoomId, String username) {
        try {
            // Get or initialize conversation history
            List<Map<String, Object>> history = conversationHistory.getOrDefault(chatRoomId, new ArrayList<>());

            // Add user message to history first
            addToHistory(history, "user", userMessage);
            conversationHistory.put(chatRoomId, history);

            // Get function definitions for OpenAI
            List<Map<String, Object>> functions = getFunctionDefinitions();

            // Build system prompt with current availability
            String systemPrompt = buildSystemPrompt();

            // Call OpenAI with function calling
            String response = openAIService.getChatCompletion(userMessage, functions, history, systemPrompt);

            // Check if OpenAI wants to call a function
            if (response.startsWith("FUNCTION_CALL:")) {
                String functionCallJson = response.substring("FUNCTION_CALL:".length());
                return handleFunctionCall(functionCallJson, chatRoomId, username, history);
            }

            // Add assistant response to history
            addToHistory(history, "assistant", response);
            conversationHistory.put(chatRoomId, history);

            return response;
        } catch (Exception e) {
            log.error("Error processing bot message: ", e);
            return "I apologize, but I encountered an error. Please try again.";
        }
    }

    private String handleFunctionCall(String functionCallJson, UUID chatRoomId, String username, 
                                      List<Map<String, Object>> history) {
        try {
            JsonNode functionCall = objectMapper.readTree(functionCallJson);
            String functionName = functionCall.get("name").asText();
            JsonNode arguments = objectMapper.readTree(functionCall.get("arguments").asText());

            String functionResult = "";

            switch (functionName) {
                case "search_doctors_by_specialty":
                    String specialty = arguments.get("specialty").asText();
                    functionResult = searchDoctorsBySpecialty(specialty);
                    functionResult = formatDoctorSearchResponse(functionResult, specialty);
                    break;

                case "check_doctor_availability":
                    String doctorName = arguments.get("doctor_name").asText();
                    String dateStr = arguments.has("date") ? arguments.get("date").asText() : null;
                    String timeStr = arguments.has("time") ? arguments.get("time").asText() : null;
                    functionResult = checkDoctorAvailability(doctorName, dateStr, timeStr);
                    functionResult = formatAvailabilityResponse(functionResult, doctorName, dateStr, timeStr);
                    break;

                case "book_appointment":
                    String doctorNameForBooking = arguments.get("doctor_name").asText();
                    String appointmentDate = arguments.get("date").asText();
                    String appointmentTime = arguments.get("time").asText();
                    String patientName = arguments.has("patient_name") ? arguments.get("patient_name").asText() : username;
                    String patientPhone = arguments.has("patient_phone") ? arguments.get("patient_phone").asText() : null;
                    String patientEmail = arguments.has("patient_email") ? arguments.get("patient_email").asText() : null;
                    functionResult = bookAppointment(doctorNameForBooking, appointmentDate, appointmentTime, 
                                                    patientName, patientPhone, patientEmail, chatRoomId);
                    break;

                case "transfer_to_human_agent":
                    functionResult = transferToHumanAgent(chatRoomId);
                    break;

                case "find_doctors_available_on_date":
                    String dateForSearch = arguments.get("date").asText();
                    functionResult = findDoctorsAvailableOnDate(dateForSearch);
                    break;

                case "get_all_available_slots":
                    functionResult = getAllAvailableSlotsFormatted();
                    break;

                case "check_appointments_by_patient_name":
                    String patientNameToCheck = arguments.has("patient_name") ? arguments.get("patient_name").asText() : username;
                    functionResult = checkAppointmentsByPatientName(patientNameToCheck);
                    break;

                default:
                    functionResult = "I'm not sure how to handle that request. Could you please rephrase?";
            }

            // Add assistant message with function_call object (not JSON string)
            Map<String, Object> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            Map<String, Object> functionCallObj = new HashMap<>();
            functionCallObj.put("name", functionName);
            functionCallObj.put("arguments", functionCall.get("arguments").asText());
            assistantMsg.put("function_call", functionCallObj);
            history.add(assistantMsg);
            
            // Add function result with function name
            addFunctionToHistory(history, functionName, functionResult);
            
            // Build system prompt with current availability
            String systemPrompt = buildSystemPrompt();
            
            // Get final response from OpenAI with function result
            String finalResponse = openAIService.getSimpleCompletion(
                "Function result: " + functionResult + "\nPlease respond naturally to the user based on this result.",
                history,
                systemPrompt
            );
            
            addToHistory(history, "assistant", finalResponse);
            conversationHistory.put(chatRoomId, history);

            return finalResponse;
        } catch (Exception e) {
            log.error("Error handling function call: ", e);
            return "I encountered an error processing your request. Please try again.";
        }
    }

    private String searchDoctorsBySpecialty(String specialty) {
        List<Doctor> doctors = doctorRepository.findBySpecialty(specialty);
        if (doctors.isEmpty()) {
            return "No doctors found with specialty: " + specialty;
        }
        return doctors.stream()
                .map(d -> d.getName() + " - " + d.getSpecialty())
                .collect(Collectors.joining(", "));
    }

    private String checkDoctorAvailability(String doctorName, String dateStr, String timeStr) {
        Optional<Doctor> doctorOpt = doctorRepository.findByName(doctorName);
        if (doctorOpt.isEmpty()) {
            return "Doctor " + doctorName + " not found.";
        }

        Doctor doctor = doctorOpt.get();
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr, DATE_FORMATTER) : LocalDate.now();
        
        List<DoctorAvailability> availabilities = availabilityRepository
                .findByDoctorAndDateAndIsAvailableTrue(doctor, date);

        if (availabilities.isEmpty()) {
            return "Doctor " + doctorName + " is not available on " + dateStr;
        }

        if (timeStr != null) {
            LocalTime requestedTime = LocalTime.parse(timeStr, TIME_FORMATTER);
            availabilities = availabilities.stream()
                    .filter(avail -> !requestedTime.isBefore(avail.getStartTime()) 
                            && !requestedTime.isAfter(avail.getEndTime()))
                    .collect(Collectors.toList());
            
            if (availabilities.isEmpty()) {
                return "Doctor " + doctorName + " is not available at " + timeStr + " on " + dateStr;
            }
        }

        return availabilities.stream()
                .map(avail -> avail.getStartTime().format(TIME_FORMATTER) + " - " + avail.getEndTime().format(TIME_FORMATTER))
                .collect(Collectors.joining(", "));
    }

    @Transactional
    private String bookAppointment(String doctorName, String dateStr, String timeStr, 
                                  String patientName, String patientPhone, String patientEmail, UUID chatRoomId) {
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findByName(doctorName);
            if (doctorOpt.isEmpty()) {
                return "Doctor " + doctorName + " not found.";
            }

            Doctor doctor = doctorOpt.get();
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);
            LocalDateTime appointmentDateTime = LocalDateTime.of(date, time);

            // Check if slot is available
            List<DoctorAvailability> availabilities = availabilityRepository
                    .findByDoctorAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqualAndIsAvailableTrue(
                            doctor, date, time, time);

            if (availabilities.isEmpty()) {
                return "Sorry, " + doctorName + " is not available at " + timeStr + " on " + dateStr + 
                       ". Please check available times.";
            }

            // Check if appointment already exists
            LocalDateTime start = appointmentDateTime.minusMinutes(30);
            LocalDateTime end = appointmentDateTime.plusMinutes(30);
            List<Appointment> existing = appointmentRepository
                    .findByDoctorAndAppointmentDateTimeBetween(doctor, start, end);
            
            if (!existing.isEmpty()) {
                return "Sorry, that time slot is already booked. Please choose another time.";
            }

            // Get patient user if exists
            Optional<User> patientUser = userRepository.findByUsername(patientName);
            
            log.info("Booking appointment - Patient: {}, Doctor: {}, Date: {}, Time: {}", 
                    patientName, doctorName, dateStr, timeStr);

            // Create appointment
            Appointment appointment = Appointment.builder()
                    .doctor(doctor)
                    .patient(patientUser.orElse(null))
                    .patientName(patientName != null ? patientName.trim() : null)
                    .patientPhone(patientPhone != null ? patientPhone.trim() : null)
                    .patientEmail(patientEmail != null ? patientEmail.trim() : null)
                    .appointmentDateTime(appointmentDateTime)
                    .chatRoomId(chatRoomId)
                    .createdAt(LocalDateTime.now())
                    .status("BOOKED")
                    .build();

            Appointment savedAppointment = appointmentRepository.save(appointment);
            
            // Flush to ensure immediate persistence
            entityManager.flush();
            
            log.info("Appointment saved successfully with ID: {}, Patient Name: '{}', Doctor: {}, Date: {}, Time: {}", 
                    savedAppointment.getId(), 
                    savedAppointment.getPatientName(),
                    savedAppointment.getDoctor().getName(),
                    savedAppointment.getAppointmentDateTime().toLocalDate(),
                    savedAppointment.getAppointmentDateTime().toLocalTime());
            
            // Verify the save
            Optional<Appointment> verifyAppointment = appointmentRepository.findById(savedAppointment.getId());
            if (verifyAppointment.isEmpty()) {
                log.error("ERROR: Appointment was not saved! ID: {}", savedAppointment.getId());
                return "Sorry, there was an error saving your appointment. Please try again.";
            }
            
            log.info("Appointment verified in database. Patient Name in DB: '{}'", 
                    verifyAppointment.get().getPatientName());

            return String.format("Great! Appointment successfully booked:\n" +
                    "Patient: %s\n" +
                    "Doctor: %s\n" +
                    "Date: %s\n" +
                    "Time: %s\n" +
                    "We'll send you a confirmation shortly.",
                    patientName, doctorName, dateStr, timeStr);
        } catch (Exception e) {
            log.error("Error booking appointment: ", e);
            return "Sorry, I encountered an error while booking your appointment. Please try again.";
        }
    }

    private String findDoctorsAvailableOnDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            List<DoctorAvailability> availabilities = availabilityRepository.findByDateAndIsAvailableTrue(date);
            
            if (availabilities.isEmpty()) {
                return "No doctors are available on " + dateStr + ". Please try another date.";
            }
            
            // Group by doctor and collect unique doctors with their specialties
            Map<Doctor, List<DoctorAvailability>> doctorMap = availabilities.stream()
                    .collect(Collectors.groupingBy(DoctorAvailability::getDoctor));
            
            StringBuilder result = new StringBuilder();
            result.append("The following doctors are available on ").append(dateStr).append(": ");
            
            List<String> doctorList = doctorMap.entrySet().stream()
                    .map(entry -> {
                        Doctor doctor = entry.getKey();
                        List<DoctorAvailability> slots = entry.getValue();
                        String times = slots.stream()
                                .map(slot -> slot.getStartTime().format(TIME_FORMATTER) + "-" + slot.getEndTime().format(TIME_FORMATTER))
                                .collect(Collectors.joining(", "));
                        return doctor.getName() + " (" + doctor.getSpecialty() + ") - Available at: " + times;
                    })
                    .collect(Collectors.toList());
            
            result.append(String.join("; ", doctorList));
            return result.toString();
        } catch (Exception e) {
            log.error("Error finding doctors available on date: ", e);
            return "Sorry, I encountered an error while searching for available doctors. Please try again.";
        }
    }

    private String checkAppointmentsByPatientName(String patientName) {
        try {
            log.info("Checking appointments for patient name: {}", patientName);
            
            // Search both by patient name and by username (if registered user)
            List<Appointment> appointmentsByName = appointmentRepository.findByPatientNameIgnoreCase(patientName);
            List<Appointment> appointmentsByUsername = appointmentRepository.findByPatient_UsernameIgnoreCase(patientName);
            
            log.info("Found {} appointments by name, {} by username", 
                    appointmentsByName.size(), appointmentsByUsername.size());
            
            // Also try searching with different case variations
            List<Appointment> allAppointmentsByName = new ArrayList<>(appointmentsByName);
            if (!patientName.equals(patientName.toLowerCase())) {
                allAppointmentsByName.addAll(appointmentRepository.findByPatientNameIgnoreCase(patientName.toLowerCase()));
            }
            if (!patientName.equals(patientName.toUpperCase())) {
                allAppointmentsByName.addAll(appointmentRepository.findByPatientNameIgnoreCase(patientName.toUpperCase()));
            }
            
            // Combine and deduplicate
            Set<UUID> seenIds = new HashSet<>();
            List<Appointment> allAppointments = new ArrayList<>();
            
            for (Appointment apt : allAppointmentsByName) {
                if (apt != null && apt.getId() != null && !seenIds.contains(apt.getId())) {
                    allAppointments.add(apt);
                    seenIds.add(apt.getId());
                }
            }
            
            for (Appointment apt : appointmentsByUsername) {
                if (apt != null && apt.getId() != null && !seenIds.contains(apt.getId())) {
                    allAppointments.add(apt);
                    seenIds.add(apt.getId());
                }
            }
            
            log.info("Total unique appointments found: {}", allAppointments.size());
            
            if (allAppointments.isEmpty()) {
                // Debug: Check all appointments in database
                long totalAppointments = appointmentRepository.count();
                log.warn("No appointments found for '{}'. Total appointments in database: {}", patientName, totalAppointments);
                return "No appointments found under the name: " + patientName;
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Found ").append(allAppointments.size()).append(" appointment(s) for ").append(patientName).append(":\n");
            
            for (Appointment apt : allAppointments) {
                result.append(String.format("- Doctor: %s, Date: %s, Time: %s\n",
                    apt.getDoctor().getName(),
                    apt.getAppointmentDateTime().toLocalDate().format(DATE_FORMATTER),
                    apt.getAppointmentDateTime().toLocalTime().format(TIME_FORMATTER)));
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error checking appointments by patient name: ", e);
            return "Sorry, I encountered an error while checking appointments. Please try again.";
        }
    }

    private String transferToHumanAgent(UUID chatRoomId) {
        List<HumanAgent> availableAgents = agentRepository.findByIsAvailableTrue();
        if (availableAgents.isEmpty()) {
            return "I apologize, but no human agents are currently available. Please try again later or continue chatting with me.";
        }

        HumanAgent agent = availableAgents.get(0);
        agent.setIsAvailable(false);
        agent.setCurrentChatRoomId(chatRoomId);
        agentRepository.save(agent);

        return "I'm transferring you to a human agent (" + agent.getName() + "). They will be with you shortly.";
    }

    private List<Map<String, Object>> getFunctionDefinitions() {
        List<Map<String, Object>> functions = new ArrayList<>();

        // Function 1: Search doctors by specialty
        Map<String, Object> searchDoctors = new HashMap<>();
        searchDoctors.put("name", "search_doctors_by_specialty");
        searchDoctors.put("description", "Search for doctors by their specialty or department");
        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("type", "object");
        Map<String, Object> specialtyProp = new HashMap<>();
        specialtyProp.put("type", "string");
        specialtyProp.put("description", "The medical specialty (e.g., Cardiology, Dermatology, Orthopedics)");
        searchParams.put("properties", Map.of("specialty", specialtyProp));
        searchParams.put("required", List.of("specialty"));
        searchDoctors.put("parameters", searchParams);
        functions.add(searchDoctors);

        // Function 2: Check doctor availability
        Map<String, Object> checkAvailability = new HashMap<>();
        checkAvailability.put("name", "check_doctor_availability");
        checkAvailability.put("description", "Check if a doctor is available on a specific date and time");
        Map<String, Object> availParams = new HashMap<>();
        availParams.put("type", "object");
        Map<String, Object> availProps = new HashMap<>();
        availProps.put("doctor_name", Map.of("type", "string", "description", "Name of the doctor"));
        availProps.put("date", Map.of("type", "string", "description", "Date in YYYY-MM-DD format"));
        availProps.put("time", Map.of("type", "string", "description", "Time in HH:mm format (optional)"));
        availParams.put("properties", availProps);
        availParams.put("required", List.of("doctor_name", "date"));
        checkAvailability.put("parameters", availParams);
        functions.add(checkAvailability);

        // Function 3: Book appointment
        Map<String, Object> bookAppt = new HashMap<>();
        bookAppt.put("name", "book_appointment");
        bookAppt.put("description", "Book an appointment with a doctor");
        Map<String, Object> bookParams = new HashMap<>();
        bookParams.put("type", "object");
        Map<String, Object> bookProps = new HashMap<>();
        bookProps.put("doctor_name", Map.of("type", "string", "description", "Name of the doctor"));
        bookProps.put("date", Map.of("type", "string", "description", "Date in YYYY-MM-DD format"));
        bookProps.put("time", Map.of("type", "string", "description", "Time in HH:mm format"));
        bookProps.put("patient_name", Map.of("type", "string", "description", "Patient name (optional)"));
        bookProps.put("patient_phone", Map.of("type", "string", "description", "Patient phone (optional)"));
        bookProps.put("patient_email", Map.of("type", "string", "description", "Patient email (optional)"));
        bookParams.put("properties", bookProps);
        bookParams.put("required", List.of("doctor_name", "date", "time"));
        bookAppt.put("parameters", bookParams);
        functions.add(bookAppt);

        // Function 4: Find doctors available on a date
        Map<String, Object> findDoctors = new HashMap<>();
        findDoctors.put("name", "find_doctors_available_on_date");
        findDoctors.put("description", "Find all doctors who have available slots on a specific date");
        Map<String, Object> findParams = new HashMap<>();
        findParams.put("type", "object");
        Map<String, Object> findProps = new HashMap<>();
        findProps.put("date", Map.of("type", "string", "description", "Date in YYYY-MM-DD format"));
        findParams.put("properties", findProps);
        findParams.put("required", List.of("date"));
        findDoctors.put("parameters", findParams);
        functions.add(findDoctors);

        // Function 5: Get all available slots
        Map<String, Object> getAllSlots = new HashMap<>();
        getAllSlots.put("name", "get_all_available_slots");
        getAllSlots.put("description", "Get all available appointment slots across all doctors and dates");
        getAllSlots.put("parameters", Map.of("type", "object", "properties", Map.of()));
        functions.add(getAllSlots);

        // Function 6: Check appointments by patient name
        Map<String, Object> checkAppointments = new HashMap<>();
        checkAppointments.put("name", "check_appointments_by_patient_name");
        checkAppointments.put("description", "Check all appointments booked under a patient's name");
        Map<String, Object> checkApptParams = new HashMap<>();
        checkApptParams.put("type", "object");
        Map<String, Object> checkApptProps = new HashMap<>();
        checkApptProps.put("patient_name", Map.of("type", "string", "description", "Patient name to search for (optional, defaults to logged-in user)"));
        checkApptParams.put("properties", checkApptProps);
        checkAppointments.put("parameters", checkApptParams);
        functions.add(checkAppointments);

        // Function 7: Transfer to human agent
        Map<String, Object> transfer = new HashMap<>();
        transfer.put("name", "transfer_to_human_agent");
        transfer.put("description", "Transfer the chat to a human agent when the user requests it");
        transfer.put("parameters", Map.of("type", "object", "properties", Map.of()));
        functions.add(transfer);

        return functions;
    }

    private String formatDoctorSearchResponse(String result, String specialty) {
        if (result.contains("No doctors found")) {
            return "Sorry, we do not have any doctors with the specialty '" + specialty + 
                   "' at our clinic. Is there anything else I could help you with?";
        }
        return "Here are the " + specialty + " doctors available: " + result;
    }

    private String formatAvailabilityResponse(String result, String doctorName, String date, String time) {
        if (result.contains("not available")) {
            return result;
        }
        if (time != null) {
            return doctorName + " is available at " + time + " on " + date + ". Would you like to book this appointment?";
        }
        return doctorName + " is available on " + date + " at the following times: " + result;
    }

    private void addToHistory(List<Map<String, Object>> history, String role, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        history.add(message);
    }

    private void addFunctionToHistory(List<Map<String, Object>> history, String functionName, String functionResult) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "function");
        message.put("name", functionName);
        message.put("content", functionResult);
        history.add(message);
    }

    private String buildSystemPrompt() {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DATE_FORMATTER);
        
        // Get all available slots
        String availability = getAllAvailableSlotsFormatted();
        
        // Get all doctor names
        String doctorList = getAllDoctorNames();
        
        return String.format(
            "You are a helpful healthcare assistant. Today's date is %s.\n" +
            "You help users book appointments with doctors based on available slots.\n" +
            "📋 Here are the current available appointment slots:\n%s\n\n" +
            "⚠️ Only accept doctor names from this list: %s.\n" +
            "If a user enters a name not in the list, respond with an error and ask them to choose a valid doctor.\n\n" +
            "🗓️ You must also verify that the doctor is available **exactly** on the requested date and time.\n" +
            "If the requested slot does not appear in the availability list above, respond with:\n" +
            "'❌ Sorry, Dr. <name> is not available on <date> at <time>. Please choose another available slot.'\n\n" +
            "If the user provides all required fields (Patient's name, Doctor's name, Date, and Time) **and the slot is available**, respond with:\n" +
            "Patient: <patient name>\nDoctor: Dr. <doctor name>\nDate: <appointment date>\nTime: <appointment time>\n" +
            "If any information is missing, only ask for the missing fields, and do not repeat already provided ones.\n\n" +
            "👤 IMPORTANT: If the user does not provide a patient name, use the logged-in username as the patient name. " +
            "Before final booking, confirm the patient name with the user by saying: 'I'll book this appointment for <username>. Is that correct?'",
            todayStr, availability, doctorList
        );
    }

    private String getAllAvailableSlotsFormatted() {
        try {
            // Get all available slots for the next 30 days
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(30);
            
            List<String> slotList = new ArrayList<>();
            LocalDate currentDate = startDate;
            
            while (!currentDate.isAfter(endDate)) {
                List<DoctorAvailability> availabilities = availabilityRepository.findByDateAndIsAvailableTrue(currentDate);
                if (!availabilities.isEmpty()) {
                    Map<Doctor, List<DoctorAvailability>> doctorMap = availabilities.stream()
                            .collect(Collectors.groupingBy(DoctorAvailability::getDoctor));
                    
                    for (Map.Entry<Doctor, List<DoctorAvailability>> entry : doctorMap.entrySet()) {
                        Doctor doctor = entry.getKey();
                        List<DoctorAvailability> slots = entry.getValue();
                        for (DoctorAvailability slot : slots) {
                            String timeSlot = slot.getStartTime().format(TIME_FORMATTER) + "-" + slot.getEndTime().format(TIME_FORMATTER);
                            slotList.add(String.format("- %s on %s at %s", doctor.getName(), currentDate.format(DATE_FORMATTER), timeSlot));
                        }
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
            
            if (slotList.isEmpty()) {
                return "No available slots found in the next 30 days.";
            }
            
            return String.join("\n", slotList);
        } catch (Exception e) {
            log.error("Error getting all available slots: ", e);
            return "Error retrieving availability. Please try again.";
        }
    }

    private String getAllDoctorNames() {
        try {
            List<Doctor> doctors = doctorRepository.findAll();
            if (doctors.isEmpty()) {
                return "No doctors found.";
            }
            return doctors.stream()
                    .map(Doctor::getName)
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.error("Error getting all doctors: ", e);
            return "Error retrieving doctor list.";
        }
    }

    public boolean detectHandover(String message) {
        String lower = message.toLowerCase();
        return lower.contains("human") || lower.contains("agent") || 
               lower.contains("speak to someone") || lower.contains("talk to a person");
    }

    public void clearConversationHistory(UUID chatRoomId) {
        conversationHistory.remove(chatRoomId);
    }
}
