# 🧪 Testing the Reminder Service with Fake Data

This guide shows how to test the reminder service using fake data without needing your friend's microservices running.

## 🚀 Quick Test Setup

### 1. Configure Gmail for Testing

Edit `src/main/resources/application-test.properties`:

```properties
# Replace these with YOUR actual Gmail credentials:
spring.mail.username=your-email@gmail.com
spring.mail.password=your-gmail-app-password
```

**⚠️ IMPORTANT**: 
- Use your Gmail **App Password**, not your regular password
- Enable 2-factor authentication on Gmail first
- Generate App Password from: Google Account > Security > 2-Step Verification > App passwords

### 2. Update Email Addresses (Optional)

If you want to receive test emails, edit `FakeUserApiService.java` and replace the fake emails with your real email:

```java
// Replace example.com emails with your email for testing
users.put(201L, new UserDto(201L, "john_doe", "your-email@gmail.com", "John", "Doe", ...));
users.put(202L, new UserDto(202L, "jane_smith", "your-email@gmail.com", "Jane", "Smith", ...));
// etc.
```

### 3. Run with Test Profile

```bash
./gradlew bootRun --args="--spring.profiles.active=test"
```

## 📧 What Will Happen

The fake data creates sessions with realistic timing:

### Upcoming Sessions (Pre-session reminders):
- **Session 101**: "Spring Boot Advanced Workshop" - starts in **30 minutes**
  - Should trigger 30-minute reminder emails
  - Participants: 3 fake users
  
- **Session 102**: "React.js Fundamentals" - starts in **15 minutes**  
  - Should trigger 15-minute reminder emails
  - Participants: 4 fake users
  
- **Session 103**: "Database Design Principles" - starts in **2 hours**
  - No reminders yet (too early)
  - Participants: 2 fake users

### Completed Sessions (Feedback reminders):
- **Session 201**: "Python Data Science Workshop" - ended **30 minutes ago**
  - Should trigger feedback reminder emails
  - Participants: 5 fake users
  
- **Session 202**: "Machine Learning Basics" - ended **1 hour ago**
  - Feedback window missed
  - Participants: 3 fake users

## 📊 Testing Workflow

### 1. Start the Service
```bash
./gradlew bootRun --args="--spring.profiles.active=test"
```

### 2. Watch the Logs
You should see:
- 🧪 Fake data messages
- Scheduler running every 30 seconds
- Email sending attempts

### 3. Check Your Email
- **30-minute reminders** for Spring Boot workshop
- **15-minute reminders** for React.js workshop  
- **Feedback reminders** for Python workshop

### 4. Test Manual Triggers
```bash
# Test email configuration
curl -X POST "http://localhost:8083/api/reminders/test-email?email=your-email@gmail.com"

# Manually trigger 30-min reminder for session 101
curl -X POST "http://localhost:8083/api/reminders/trigger/101?reminderType=BEFORE_30_MIN"

# Manually trigger feedback reminder for session 201
curl -X POST "http://localhost:8083/api/reminders/trigger/201?reminderType=AFTER_30_MIN_FEEDBACK"
```

### 5. Check Service Health
```bash
curl http://localhost:8083/api/reminders/health
curl http://localhost:8083/api/reminders/stats
```

## 🔍 Expected Email Examples

### Session Reminder Email:
```
Subject: Session Reminder: Spring Boot Advanced Workshop - Starting in 30 minutes

Hello John Doe!

This is a friendly reminder that your session is starting in 30 minutes.

📅 Session Details:
Title: Spring Boot Advanced Workshop
Instructor: Dr. John Smith
Date & Time: [current time + 30 minutes]
Description: Learn advanced Spring Boot concepts...

[Join Session Button]
```

### Feedback Reminder Email:
```
Subject: Feedback Request: Python Data Science Workshop - Your input matters!

Hello Helen Black!

Thank you for participating in our session. We hope you found it valuable...

📅 Session You Attended:
Title: Python Data Science Workshop  
Instructor: Dr. Lisa Wang
Date: [session date]

[Give Feedback Button]
```

## ⚙️ Customizing Test Data

### Change Session Timing:
Edit `FakeSessionApiService.java`:
```java
// Change timing to test different scenarios
now.plusMinutes(5), // Session starts in 5 minutes
now.minusMinutes(45) // Session ended 45 minutes ago
```

### Add More Users:
Edit `FakeUserApiService.java` to add more test users with your email addresses.

### Speed Up Testing:
The test profile already sets scheduler to run every 30 seconds instead of 5 minutes.

## 🐛 Troubleshooting

### No Emails Received:
1. Check Gmail credentials in `application-test.properties`
2. Verify Gmail App Password (not regular password)
3. Check spam folder
4. Look at service logs for SMTP errors

### Service Won't Start:
1. Ensure no other service is running on port 8083
2. Check Java version (should be Java 24)
3. Verify Gradle build was successful

### No Fake Data:
1. Ensure you're running with test profile: `--spring.profiles.active=test`
2. Look for 🧪 emoji in logs indicating fake data is being used

## 📝 Test Results

After testing, you should see:
- ✅ Service starts successfully with fake data
- ✅ Scheduler runs every 30 seconds
- ✅ Emails sent for appropriate sessions
- ✅ No real API calls made
- ✅ Manual triggers work via REST API

This confirms your reminder service is working correctly and ready for integration with real microservices!