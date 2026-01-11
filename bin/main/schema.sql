CREATE TABLE IF NOT EXISTS users (
                                     id CHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS chat_rooms (
                                          id CHAR(36) PRIMARY KEY,
    user1_id CHAR(36) NOT NULL,
    user2_id CHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user1_id, user2_id)
    );

CREATE TABLE IF NOT EXISTS messages (
                                        id CHAR(36) PRIMARY KEY,
    chat_room_id CHAR(36) NOT NULL,
    sender_id CHAR(36) NOT NULL,
    receiver_id CHAR(36) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20),
    message_type VARCHAR(20)
    );
