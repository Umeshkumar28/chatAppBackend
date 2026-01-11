# How to Run the Chat Application

## Prerequisites
- Docker Desktop installed and running
- Java 21 installed
- Node.js and npm installed
- OpenAI API key configured (already set in application.properties)

## Step 1: Start Docker Services

Navigate to the backend directory and start all required services:

```powershell
cd C:\Users\User\Documents\chatBotWithCursorBackend\chatAppBackend
docker-compose up -d
```

This will start:
- MySQL on port 3307
- Redis on port 6379
- Zookeeper on port 2181
- Kafka on port 9092

**Note:** If you get a port conflict error (e.g., Redis port 6379 already in use), you can:
- Stop the existing container: `docker stop chatappbackend-redis-1`
- Or check what's using the port: `docker ps` and stop conflicting containers

**Verify services are running:**
```powershell
docker ps
```

You should see all 4 containers running (mysql, redis, zookeeper, kafka).

## Step 2: Start the Backend

In the backend directory:

```powershell
cd C:\Users\User\Documents\chatBotWithCursorBackend\chatAppBackend
.\gradlew bootRun
```

Or if using Windows Command Prompt:
```cmd
gradlew.bat bootRun
```

**What happens:**
- Spring Boot will start on port 8080
- Database tables will be created automatically (Hibernate DDL auto-update)
- The `DataInitializer` will create:
  - DoctorAssistant bot user
  - Sample doctors (Dr. Smith, Dr. Jones, Dr. Williams, Dr. Brown)
  - Sample availability slots
  - Human agents

**Wait for:** `Started ChatApplication` message in the console

## Step 3: Start the Frontend

Open a **new terminal** and navigate to the frontend directory:

```powershell
cd C:\Users\User\Documents\chatBotWithCursor\chatAppFrontEnd
npm install
npm start
```

**What happens:**
- React app will start on http://localhost:3000
- Browser should open automatically

## Step 4: Test the Application

1. **Register/Login:**
   - If you don't have an account, register a new user
   - Or login with existing credentials

2. **See DoctorAssistant Bot:**
   - After login, you should see "DoctorAssistant" at the **top** of the user list
   - It will have a "Bot" badge

3. **Chat with the Bot:**
   - Click on "DoctorAssistant"
   - Try these sample messages:
     - "Hello"
     - "I need an appointment with a cardiologist"
     - "Can I see Dr. Smith tomorrow at 10 AM?"
     - "I want to book an appointment"
     - "Is Dr. Jones available?"

4. **Chat with Other Users:**
   - Select any other user from the list
   - Send messages (they should appear in real-time)

## Troubleshooting

### Backend won't start:
- **Check Docker:** Make sure all containers are running (`docker ps`)
- **Check ports:** Ensure ports 8080, 3307, 6379, 9092, 2181 are not in use
- **Check logs:** Look for error messages in the console

### Kafka connection errors:
- Make sure Zookeeper is running first
- Wait a few seconds after starting Zookeeper before starting Kafka
- Check: `docker logs chatappbackend-kafka-1`

### Frontend can't connect:
- Verify backend is running on http://localhost:8080
- Check browser console for errors
- Verify `.env.development` file exists with correct URLs

### Bot not responding:
- Check OpenAI API key is set correctly in `application.properties`
- Check backend logs for OpenAI API errors
- Verify the model is correct (gpt-3.5-turbo or gpt-4)

## Stopping the Application

1. **Stop Frontend:** Press `Ctrl+C` in the frontend terminal

2. **Stop Backend:** Press `Ctrl+C` in the backend terminal

3. **Stop Docker Services:**
```powershell
cd C:\Users\User\Documents\chatBotWithCursorBackend\chatAppBackend
docker-compose down
```

## Quick Start Commands

```powershell
# Terminal 1: Start Docker services
cd C:\Users\User\Documents\chatBotWithCursorBackend\chatAppBackend
docker-compose up -d

# Terminal 2: Start Backend
cd C:\Users\User\Documents\chatBotWithCursorBackend\chatAppBackend
.\gradlew bootRun

# Terminal 3: Start Frontend
cd C:\Users\User\Documents\chatBotWithCursor\chatAppFrontEnd
npm start
```

## Environment Variables

The frontend uses `.env.development` file (should already exist):
```
REACT_APP_API_BASE=http://localhost:8080/api
REACT_APP_WS_URL=http://localhost:8080/ws
REACT_APP_STOMP_APP_DEST=/app/chat.send
REACT_APP_STOMP_TOPIC_PREFIX=/topic/messages/
```

Backend OpenAI key is in `application.properties` (already configured).
