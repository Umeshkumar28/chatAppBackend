# Appointment Database Schema and Queries

## Database Information
- **Database Name**: `chatapp`
- **Port**: `3307` (Docker MySQL container)
- **Connection**: `localhost:3307`

## Tables Used for Appointments

### 1. `appointments` Table
This is the main table where appointment data is stored.

**Schema:**
```sql
CREATE TABLE appointments (
    id BINARY(16) PRIMARY KEY,
    appointment_date_time DATETIME(6) NOT NULL,
    chat_room_id BINARY(16),
    created_at DATETIME(6) NOT NULL,
    patient_email VARCHAR(100),
    patient_name VARCHAR(100),
    patient_phone VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    patient_id BINARY(16)
);
```

### 2. `doctors` Table
Stores doctor information.

### 3. `users` Table
Stores user/patient information (if patient is a registered user).

## Useful SELECT Queries

### 1. View All Appointments with Doctor Names
```sql
SELECT 
    a.id,
    a.patient_name,
    a.patient_email,
    a.patient_phone,
    d.name AS doctor_name,
    d.specialty AS doctor_specialty,
    a.appointment_date_time,
    a.status,
    a.created_at,
    a.chat_room_id
FROM appointments a
JOIN doctors d ON a.doctor_id = d.id
ORDER BY a.appointment_date_time DESC;
```

### 2. View Appointments for a Specific Patient (by name)
```sql
SELECT 
    a.id,
    a.patient_name,
    d.name AS doctor_name,
    d.specialty,
    a.appointment_date_time,
    DATE(a.appointment_date_time) AS appointment_date,
    TIME(a.appointment_date_time) AS appointment_time,
    a.status,
    a.created_at
FROM appointments a
JOIN doctors d ON a.doctor_id = d.id
WHERE LOWER(a.patient_name) = LOWER('Umesh')
ORDER BY a.appointment_date_time DESC;
```

### 3. View Appointments for a Specific Patient (by username - if registered user)
```sql
SELECT 
    a.id,
    a.patient_name,
    u.username AS patient_username,
    d.name AS doctor_name,
    d.specialty,
    a.appointment_date_time,
    a.status,
    a.created_at
FROM appointments a
JOIN doctors d ON a.doctor_id = d.id
LEFT JOIN users u ON a.patient_id = u.id
WHERE LOWER(u.username) = LOWER('umesh')
ORDER BY a.appointment_date_time DESC;
```

### 4. View All Appointments with Patient and Doctor Details
```sql
SELECT 
    a.id,
    a.patient_name,
    COALESCE(u.username, 'Not registered') AS patient_username,
    d.name AS doctor_name,
    d.specialty AS doctor_specialty,
    DATE_FORMAT(a.appointment_date_time, '%Y-%m-%d') AS appointment_date,
    DATE_FORMAT(a.appointment_date_time, '%H:%i') AS appointment_time,
    a.status,
    a.patient_email,
    a.patient_phone,
    a.created_at
FROM appointments a
JOIN doctors d ON a.doctor_id = d.id
LEFT JOIN users u ON a.patient_id = u.id
ORDER BY a.appointment_date_time DESC;
```

### 5. Count Appointments by Patient Name
```sql
SELECT 
    patient_name,
    COUNT(*) AS total_appointments
FROM appointments
GROUP BY patient_name
ORDER BY total_appointments DESC;
```

### 6. View Appointments for a Specific Date
```sql
SELECT 
    a.patient_name,
    d.name AS doctor_name,
    TIME(a.appointment_date_time) AS appointment_time,
    a.status
FROM appointments a
JOIN doctors d ON a.doctor_id = d.id
WHERE DATE(a.appointment_date_time) = '2026-01-12'
ORDER BY a.appointment_date_time;
```

### 7. View Appointments for a Specific Doctor
```sql
SELECT 
    a.patient_name,
    a.appointment_date_time,
    DATE_FORMAT(a.appointment_date_time, '%Y-%m-%d %H:%i') AS formatted_datetime,
    a.status,
    a.patient_email,
    a.patient_phone
FROM appointments a
JOIN doctors d ON a.doctor_id = d.id
WHERE d.name = 'Dr. Jones'
ORDER BY a.appointment_date_time;
```

### 8. View Recent Appointments (Last 10)
```sql
SELECT 
    a.id,
    a.patient_name,
    d.name AS doctor_name,
    DATE_FORMAT(a.appointment_date_time, '%Y-%m-%d %H:%i') AS appointment_datetime,
    a.status,
    a.created_at
FROM appointments a
JOIN doctors d ON a.doctor_id = d.id
ORDER BY a.created_at DESC
LIMIT 10;
```

## Quick Connection Commands

### Connect to MySQL Container
```bash
docker exec -it chatappbackend-mysql-1 mysql -u root -pUmesh@123 chatapp
```

### Or from MySQL Shell (if using MySQL Shell)
```sql
\connect root@localhost:3307/chatapp
```

## Notes

1. **UUID Storage**: IDs are stored as `BINARY(16)` (UUID format). To see them in readable format, use:
   ```sql
   SELECT BIN_TO_UUID(id) AS readable_id, patient_name FROM appointments;
   ```

2. **Patient Name vs Username**: 
   - `patient_name` is always stored (even for registered users)
   - `patient_id` is only set if the patient is a registered user
   - Search should check both `patient_name` and `users.username`

3. **Appointment Status**: Can be `BOOKED`, `CANCELLED`, or `COMPLETED`

4. **Date/Time Format**: 
   - Stored as `DATETIME(6)` (includes microseconds)
   - Use `DATE()` and `TIME()` functions to extract date/time parts
   - Use `DATE_FORMAT()` for custom formatting
