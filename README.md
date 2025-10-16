# Choroid Reminder Service

An automated email reminder service for the Choroid Session Management System. This service sends timely reminders to session creators and participants about upcoming sessions and feedback requests.

## Overview

The Choroid Reminder Service operates by periodically checking session data and sending personalized email reminders:

- **Conductor Reminders**: Sent to session creators 30 and 15 minutes before their sessions start
- **Attendee Reminders**: Sent to registered participants 30 and 15 minutes before sessions start  
- **Feedback Reminders**: Sent to all participants (creator + attendees) 30 minutes after sessions end

## Architecture & Design

### 4-Endpoint Integration (Optimized)

The service integrates with your microservices using 4 optimized endpoints:

1. **Upcoming Sessions**: `GET /gateway/sessions/upcoming`
   - Retrieves sessions starting within ~30-35 minutes (pre-filtered by backend)
   - Returns: SessionID, CreatorID, Title, StartDateTime, Duration, Tags, MeetingLink, ResourcesLink

2. **Completed Sessions**: `GET /gateway/sessions/completed`
   - Retrieves sessions that ended within ~30-60 minutes (pre-filtered by backend)
   - Returns: Same session structure as upcoming sessions

3. **RARF Endpoint**: `GET /gateway/rarf/users/{sessionID}`
   - Retrieves registered user IDs from the RARF table for a specific session
   - Returns: List of UserID values

4. **Users Endpoint**: `GET /gateway/users/batch?userIDs=123,456,789`
   - Retrieves user details for a batch of user IDs from the Users table
   - Returns: UserID, Name, RollNumber, NITKEmail, PersonalEmail, Degree, Major, etc.

### Database Schema Integration

The service works with your existing database schema:

**Sessions Table**:
```
Sessions(SessionID, CreatorID, Title, StartDateTime, Duration, Tags, MeetingLink, ResourcesLink)
```

**Users Table**:
```
Users(UserID, Name, RollNumber, NITKEmail, PersonalEmail, Degree, Major, Minor, Skills, ResumeLink, TeachTags, LearnTags)
```

**RARF Table**:
```
RARF(SessionID, UserID, FeedbackFilled, Rating, A0, A1….An)
```

### Optimized Reminder Logic Flow

1. **Scheduler runs every 5 minutes** (configurable)
2. **Fetches pre-filtered sessions**:
   - Upcoming sessions (starting within 30-35 minutes) from `/gateway/sessions/upcoming`
   - Completed sessions (ended within 30-60 minutes) from `/gateway/sessions/completed`
3. **For each relevant session**:
   - Gets registered users from RARF table
   - Gets user details (including emails) from Users table
   - Distinguishes between creator and attendees
   - Sends appropriate reminder types
4. **Tracks sent reminders** to prevent duplicates
5. **Logs all activities** for monitoring

**Benefits of Filtered Endpoints**:
- ✅ **Reduced Network Traffic**: Only relevant sessions are transferred
- ✅ **Improved Performance**: Less data processing on the reminder service
- ✅ **Better Database Efficiency**: Filtering done at the source with optimized queries
- ✅ **Scalability**: Service performs better as session data grows

## Features

### Smart Reminder Timing
- **30-minute reminders**: First reminder with preparation time
- **15-minute reminders**: Final reminder before session starts
- **Feedback reminders**: Sent 30 minutes after session completion
- **Timing windows**: Flexible windows to account for scheduler intervals

### Personalized Emails
- **Conductor emails**: Include session preparation checklist and start button
- **Attendee emails**: Include session details and join button
- **Feedback emails**: Professional requests with feedback importance explanation
- **HTML templates**: Beautiful, responsive email templates

### Robust Operation
- **Duplicate prevention**: Tracks sent reminders to avoid spam
- **Error handling**: Graceful error handling with detailed logging
- **Email validation**: Validates email addresses before sending
- **Fallback logic**: Uses personal email, falls back to NITK email

### Configuration Flexibility
- **Environment variables**: All settings configurable via environment
- **Placeholder endpoints**: Easy to replace with actual URLs
- **Timing configuration**: Customizable reminder timing intervals
- **Profile support**: Test vs production configurations

## Quick Setup

### 1. Configure Database Connection
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3307/choroid_db
spring.datasource.username=root
spring.datasource.password=your_password
```

### 2. Configure Email (Gmail)
```properties
# Email Configuration
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
reminder.from-email=noreply@choroid.com
reminder.from-name=Choroid Session System
```

### 3. Configure API Endpoints
```properties
# Replace these placeholder URLs with your actual endpoints
api.gateway.base-url=http://localhost:8080/api
api.session.get-upcoming-sessions=/gateway/sessions/upcoming
api.session.get-completed-sessions=/gateway/sessions/completed
api.rarf.get-registered-users=/gateway/rarf/users/{sessionID}
api.user.get-users-by-ids=/gateway/users/batch
```

### 4. Run the Service
```bash
./gradlew bootRun
```

## API Endpoints

### Health Check
```
GET /health
```

### Manual Triggers (for testing)
```
POST /api/reminders/trigger/{sessionId}?type=BEFORE_30_MIN
POST /api/reminders/trigger/{sessionId}?type=BEFORE_15_MIN  
POST /api/reminders/trigger/{sessionId}?type=AFTER_30_MIN_FEEDBACK
```

### Statistics
```
GET /api/reminders/stats
```

## Configuration Reference

### Required Endpoint Integration

Your microservices need to provide these 4 endpoints:

#### 1. Upcoming Sessions Endpoint
```http
GET /gateway/sessions/upcoming
Response: {
  "success": true,
  "data": [
    {
      "sessionID": 123,
      "creatorID": 456,
      "title": "Introduction to Machine Learning",
      "startDateTime": "2024-03-20T14:30:00",
      "duration": 90,
      "tags": "AI,ML,Tech",
      "meetingLink": "https://meet.google.com/abc-def-ghi",
      "resourcesLink": "https://drive.google.com/folder/xyz"
    }
  ]
}
```
**Filter Criteria**: Return sessions where StartDateTime is between NOW and NOW + 35 minutes

#### 2. Completed Sessions Endpoint
```http
GET /gateway/sessions/completed
Response: {
  "success": true,
  "data": [
    {
      "sessionID": 456,
      "creatorID": 789,
      "title": "React Fundamentals",
      "startDateTime": "2024-03-20T13:00:00",
      "duration": 120,
      "tags": "React,Frontend,JS",
      "meetingLink": null,
      "resourcesLink": "https://drive.google.com/folder/abc"
    }
  ]
}
```
**Filter Criteria**: Return sessions where (StartDateTime + Duration) is between NOW - 65 minutes and NOW - 25 minutes

### Backend Implementation Guide

**SQL Query Examples for Filtered Endpoints:**

```sql
-- Upcoming Sessions Endpoint Query
SELECT SessionID, CreatorID, Title, StartDateTime, Duration, Tags, MeetingLink, ResourcesLink
FROM Sessions 
WHERE StartDateTime BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 35 MINUTE)
ORDER BY StartDateTime ASC;

-- Completed Sessions Endpoint Query  
SELECT SessionID, CreatorID, Title, StartDateTime, Duration, Tags, MeetingLink, ResourcesLink
FROM Sessions 
WHERE DATE_ADD(StartDateTime, INTERVAL Duration MINUTE) BETWEEN 
      DATE_SUB(NOW(), INTERVAL 65 MINUTE) AND 
      DATE_SUB(NOW(), INTERVAL 25 MINUTE)
ORDER BY StartDateTime DESC;
```

#### 3. RARF Endpoint
```http
GET /gateway/rarf/users/123
Response: {
  "success": true,
  "data": [456, 789, 012] // Array of UserID values
}
```

#### 4. Users Endpoint
```http
GET /gateway/users/batch?userIDs=456,789,012
Response: {
  "success": true,
  "data": [
    {
      "userID": 456,
      "name": "John Doe",
      "rollNumber": "CS21B001", 
      "nitkEmail": "john@nitk.edu.in",
      "personalEmail": "john.doe@gmail.com",
      "degree": "B.Tech",
      "major": "Computer Science"
      // ... other fields
    }
  ]
}
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Service port | `8083` |
| `DB_URL` | Database URL | `jdbc:mysql://localhost:3307/choroid_db` |
| `MAIL_USERNAME` | Gmail username | Required |
| `MAIL_PASSWORD` | Gmail app password | Required |
| `GATEWAY_BASE_URL` | API Gateway base URL | `http://localhost:8080/api` |
| `SCHEDULER_ENABLED` | Enable/disable scheduler | `true` |
| `SESSION_CHECK_INTERVAL` | Check interval (ms) | `300000` (5 min) |

## Monitoring & Troubleshooting

### Logs
The service provides detailed logging:
- Session processing status
- Email sending results  
- API call results
- Error details with stack traces

### Common Issues

**No emails being sent:**
1. Check Gmail SMTP configuration
2. Verify app password (not regular password)
3. Check email addresses in database
4. Review error logs

**Missing sessions:**
1. Verify endpoint URLs are correct
2. Check database connectivity
3. Review session timing logic
4. Check API response format

**Duplicate reminders:**
1. Check if multiple service instances are running
2. Review reminder tracking logic
3. Verify session timing calculations

### Manual Testing

The service provides manual trigger endpoints for testing:

```bash
# Test upcoming session reminders
curl -X POST "http://localhost:8083/api/reminders/trigger/123?type=BEFORE_30_MIN"
curl -X POST "http://localhost:8083/api/reminders/trigger/123?type=BEFORE_15_MIN"

# Test feedback reminders
curl -X POST "http://localhost:8083/api/reminders/trigger/123?type=AFTER_30_MIN_FEEDBACK"

# Check service statistics
curl http://localhost:8083/api/reminders/stats
```

### Test Mode

For development/testing:
```properties
# Enable fake data for testing (no external APIs needed)
test.use-fake-data=true

# Disable scheduler for manual control  
reminder.scheduler.enabled=false

# Shorter intervals for testing
reminder.before-session.first=2  # 2 minutes instead of 30
reminder.before-session.second=1 # 1 minute instead of 15
```

## Production Deployment

### Docker Support
```dockerfile
FROM openjdk:21-jre-slim
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: choroid-reminder-service
spec:
  replicas: 1  # Single instance to avoid duplicate reminders
  selector:
    matchLabels:
      app: choroid-reminder-service
  template:
    spec:
      containers:
      - name: reminder-service
        image: choroid-reminder-service:latest
        env:
        - name: DB_URL
          value: "jdbc:mysql://mysql-service:3306/choroid_db"
        - name: GATEWAY_BASE_URL  
          value: "http://api-gateway-service:8080/api"
```

## Contributing

1. Update endpoint placeholders with actual URLs
2. Test with real data
3. Monitor email delivery
4. Adjust timing windows as needed
5. Add custom email templates if required

## License

This service is part of the Choroid Session Management System.


## REST API

The reminder service provides these REST endpoints:

### Health & Monitoring

```http
GET /api/reminders/health
GET /api/reminders/stats
GET /api/reminders/config
GET /api/reminders/help
```

### Manual Triggers

```http
# Trigger reminder for specific session
POST /api/reminders/trigger/{sessionId}?reminderType=BEFORE_30_MIN

# Test email configuration
POST /api/reminders/test-email?email=test@example.com

# Manually run scheduled check
POST /api/reminders/check-now
```

### Example Requests

```bash
# Check service health
curl http://localhost:8083/api/reminders/health

# Send test email
curl -X POST "http://localhost:8083/api/reminders/test-email?email=test@example.com"

# Manual trigger 30-minute reminder for session 123
curl -X POST "http://localhost:8083/api/reminders/trigger/123?reminderType=BEFORE_30_MIN"

# Get service statistics
curl http://localhost:8083/api/reminders/stats
```

## Email Templates

The service sends three types of emails:

### 1. Session Reminder (30 minutes before)
- **Subject**: "Session Reminder: [Title] - Starting in 30 minutes"
- **Content**: Session details, instructor info, meeting link
- **Action**: Join Session button (if meeting link available)

### 2. Session Reminder (15 minutes before)  
- **Subject**: "Session Reminder: [Title] - Starting in 15 minutes"
- **Content**: Similar to 30-minute reminder with urgency
- **Action**: Join Session button (if meeting link available)

### 3. Feedback Request (30 minutes after)
- **Subject**: "Feedback Request: [Title] - Your input matters!"
- **Content**: Thank you message, feedback importance
- **Action**: Give Feedback button (placeholder - update with your feedback URL)

All emails are responsive HTML with professional styling.

## Customization

### Changing Reminder Timing

Update in `application.properties`:

```properties
reminder.before-session.first=45    # 45 minutes before
reminder.before-session.second=10   # 10 minutes before  
reminder.after-session.feedback=60  # 60 minutes after
```

### Modifying Email Templates

Edit the HTML templates in `EmailService.java`:
- `buildSessionReminderHtml()` - Session reminder template
- `buildFeedbackReminderHtml()` - Feedback request template

### Adding New Reminder Types

1. Add to `ReminderStatus.ReminderType` enum
2. Update `ReminderSchedulerService` logic
3. Add new email template method
4. Update API controller for manual triggers

## Deployment

### Docker (Recommended)

Create `Dockerfile`:

```dockerfile
FROM openjdk:21-jre-slim

WORKDIR /app
COPY build/libs/choroid-reminder-service-*.jar app.jar

EXPOSE 8083

ENV JAVA_OPTS="-Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

Build and run:

```bash
./gradlew build
docker build -t choroid-reminder-service .
docker run -p 8083:8083 \
  -e MAIL_USERNAME=your-email@gmail.com \
  -e MAIL_PASSWORD=your-app-password \
  -e GATEWAY_BASE_URL=http://your-gateway:8080/api \
  choroid-reminder-service
```

### Production Checklist

- [ ] Replace all placeholder API endpoints with actual URLs
- [ ] Configure production email credentials (use environment variables)
- [ ] Set up proper database for reminder tracking (optional)
- [ ] Configure logging levels appropriately
- [ ] Set up monitoring and alerting
- [ ] Test email delivery in production environment
- [ ] Verify all microservice integrations
- [ ] Set appropriate JVM memory limits

## Troubleshooting

### Common Issues

#### 1. Emails Not Sending

**Check**:
- Gmail credentials and App Password
- SMTP configuration
- Internet connectivity
- Email service logs

```bash
# Test email configuration
curl -X POST "http://localhost:8083/api/reminders/test-email?email=your-email@gmail.com"
```

#### 2. API Integration Failures

**Check**:
- API endpoint URLs in configuration
- Network connectivity to session/user services
- API response formats match expected structure
- Authentication/authorization if required

#### 3. No Reminders Being Sent

**Check**:
- Scheduler is enabled (`reminder.scheduler.enabled=true`)
- Session data contains future/past sessions as expected
- User data contains valid email addresses
- Check service logs for errors

### Viewing Logs

```bash
# Application logs
tail -f logs/application.log

# Or check console output for errors
./gradlew bootRun
```

### Debug Mode

Enable debug logging in `application.properties`:

```properties
logging.level.com.ddbs.choroid_reminder_service=DEBUG
logging.level.org.springframework.mail=DEBUG
logging.level.org.springframework.scheduling=DEBUG
```

## Monitoring

### Health Checks

The service provides health endpoints for monitoring:

- **Liveness**: `GET /api/reminders/health`
- **Statistics**: `GET /api/reminders/stats`

### Key Metrics

Monitor these metrics:
- Email sending success/failure rates
- API call response times and errors
- Scheduler execution frequency
- Memory and CPU usage

### Logging

The service logs:
- Scheduled execution runs
- Email sending attempts and results
- API call failures
- Configuration issues

## Security Considerations

- Store email credentials as environment variables, not in code
- Use App Passwords for Gmail (not main account password)
- Consider rate limiting for manual trigger APIs
- Validate all external API responses
- Implement proper error handling to avoid information disclosure

## Support

For issues or questions:

1. Check the logs for error messages
2. Verify all configuration settings
3. Test individual components (email, API calls)
4. Check microservice dependencies are running

## License

[Your License Here]

---

**⚠️ Important Reminders:**

1. **Replace all placeholder API endpoints** in `application.properties`
2. **Configure Gmail credentials** properly with App Password
3. **Update email templates** with your branding and feedback URLs
4. **Test thoroughly** in your environment before production deployment

The service is designed to be resilient and will continue running even if some external dependencies are temporarily unavailable.