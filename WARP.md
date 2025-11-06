# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Common Development Commands

### Build & Run
```bash
# Build the project
./gradlew build

# Run the service (development)
./gradlew bootRun

# Run with test profile (uses fake data, no external APIs required)
./gradlew bootRun --args="--spring.profiles.active=test"

# Clean build
./gradlew clean build

# Run tests
./gradlew test
```

### Testing Commands
```powershell
# Test email configuration
Invoke-WebRequest -Uri "http://localhost:8083/api/reminders/test-email?email=your-email@gmail.com" -Method POST

# Manual trigger 30-minute reminder for session
Invoke-WebRequest -Uri "http://localhost:8083/api/reminders/trigger/101?reminderType=BEFORE_30_MIN" -Method POST

# Manual trigger feedback reminder
Invoke-WebRequest -Uri "http://localhost:8083/api/reminders/trigger/201?reminderType=AFTER_30_MIN_FEEDBACK" -Method POST

# Check service health
Invoke-WebRequest -Uri "http://localhost:8083/api/reminders/health"

# View reminder statistics
Invoke-WebRequest -Uri "http://localhost:8083/api/reminders/stats"

# Manually trigger scheduler check
Invoke-WebRequest -Uri "http://localhost:8083/api/reminders/check-now" -Method POST
```

### Docker Commands
```bash
# Build Docker image
docker build -t choroid-reminder-service .

# Run container
docker run -p 8083:8083 \
  -e MAIL_USERNAME=your-email@gmail.com \
  -e MAIL_PASSWORD=your-app-password \
  -e GATEWAY_BASE_URL=http://your-gateway:8100 \
  choroid-reminder-service
```

## Architecture Overview

### Service Purpose
The Choroid Reminder Service is a **Spring Boot microservice** that sends automated email reminders for the Choroid Session Management System. It operates independently as a scheduled background service.

**Key Responsibilities:**
- Send session reminders to conductors and attendees (30 minutes before sessions)
- Send feedback requests to participants (30 minutes after sessions end)
- Track sent reminders to prevent duplicates
- Provide manual trigger APIs for testing

### Technology Stack
- **Framework:** Spring Boot 3.5.5
- **Language:** Java 24
- **Build Tool:** Gradle (Kotlin DSL)
- **Database:** MySQL 8.x (JDBC only, no JPA/Hibernate)
- **Email:** Spring Mail (Gmail SMTP)
- **Scheduling:** Spring Scheduler
- **HTTP Client:** Spring WebFlux WebClient
- **API Gateway:** Integrates with 3 gateway endpoints

### Project Structure
```
src/main/java/com/ddbs/choroid_reminder_service/
├── ChoroidReminderServiceApplication.java    # Main application entry point
├── config/
│   └── AppConfig.java                         # WebClient and bean configuration
├── controller/
│   └── ReminderController.java                # REST API endpoints (health, manual triggers, stats)
├── dto/
│   ├── ApiResponse.java                       # Standard API response wrapper
│   ├── ReminderStatus.java                    # Reminder tracking model
│   ├── SessionDto.java                        # Session data model
│   └── UserDto.java                           # User data model
├── service/
│   ├── EmailService.java                      # Email sending and HTML template generation
│   ├── ReminderSchedulerService.java          # Scheduled job execution (runs every 5 minutes)
│   ├── SessionApiService.java                 # Integration with session microservice
│   └── UserApiService.java                    # Integration with user microservice
└── test/                                      # Test-only fake data services
    ├── FakeSessionApiService.java
    └── FakeUserApiService.java
```

### Integration Architecture: 3-Endpoint Design

The service integrates with your microservices gateway using **3 optimized endpoints**:

#### 1. Session Search (POST)
**Endpoint:** `/choroid/sessions/search`
- **Purpose:** Search for sessions by time ranges
- **Used for:** Getting upcoming sessions (starting soon) and completed sessions (just ended)
- **Request:** POST with JSON body containing time filters
```json
{
  "startTimeFrom": "2024-03-20T14:00:00",
  "startTimeTo": "2024-03-20T14:35:00"
}
```
- **Returns:** List of sessions with SessionID, CreatorID, CreatorUsername, Title, StartDateTime, Duration, etc.

#### 2. RARF Records (GET)
**Endpoint:** `/choroid/rarf/session/{sessionId}/all`
- **Purpose:** Get all registered usernames for a session
- **Returns:** Array of usernames (attendees who registered)
```json
["john_doe", "jane_smith", "alice_wilson"]
```

#### 3. User Email Lookup (GET)
**Endpoint:** `/users/api/findemail/{username}`
- **Purpose:** Get email address for a specific username
- **Returns:** Email string
```json
"john.doe@example.com"
```

### Reminder Logic Flow

**Scheduled Job (Runs every 5 minutes):**
1. **Search for upcoming sessions** (starting in 25-35 minutes)
   - POST to `/sessions/search` with time filters
   - Get sessions and their creator usernames
2. **For each upcoming session:**
   - GET registered attendees from `/rarf/session/{id}/all`
   - Combine creator + attendees into recipient list
   - GET email addresses for each username
   - Send **conductor reminder** to creator (includes preparation checklist)
   - Send **attendee reminder** to registered users (includes join button)
3. **Search for completed sessions** (ended 25-35 minutes ago)
   - POST to `/sessions/search` with end time filters
4. **For each completed session:**
   - GET registered attendees from RARF
   - GET email addresses
   - Send **feedback request** emails to all attendees
5. **Track reminders** in `reminder_status` table to prevent duplicates

### Database Schema

**Primary Table:** `reminder_status`
```sql
CREATE TABLE reminder_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    reminder_type ENUM('BEFORE_30_MIN', 'AFTER_30_MIN_FEEDBACK') NOT NULL,
    recipient_username VARCHAR(255) NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_reminder (session_id, reminder_type, recipient_username)
);
```
- Tracks which reminders have been sent
- Prevents duplicate emails
- Index on (session_id, reminder_type, recipient_username) for fast lookups

## Configuration Guide

### Environment Variables

All configuration can be overridden via environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Service port | `8083` |
| `DB_URL` | MySQL database URL | `jdbc:mysql://localhost:3307/choroid_db` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | Required |
| `MAIL_USERNAME` | Gmail email address | Required |
| `MAIL_PASSWORD` | Gmail app password | Required |
| `GATEWAY_BASE_URL` | API Gateway base URL | `http://25.7.141.58:8100` |
| `SCHEDULER_ENABLED` | Enable/disable scheduler | `true` |
| `SESSION_CHECK_INTERVAL` | Scheduler interval (ms) | `300000` (5 min) |
| `REMINDER_BEFORE_FIRST` | Minutes before session | `30` |
| `REMINDER_AFTER_FEEDBACK` | Minutes after session | `30` |

### Gmail Setup (Required)

**Gmail requires an App Password for SMTP:**

1. Enable 2-Factor Authentication on your Google account
2. Go to: Google Account → Security → 2-Step Verification → App passwords
3. Generate app password for "Mail"
4. Update `application.properties`:
```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-char-app-password
```

**⚠️ Never commit actual credentials to version control!** Use environment variables in production.

### Test Mode (Fake Data)

For development/testing without external microservices:

1. Create/use `application-test.properties`
2. Set `test.use-fake-data=true`
3. Run with test profile: `./gradlew bootRun --args="--spring.profiles.active=test"`

**Benefits:**
- No external API dependencies
- Faster testing cycles (30-second intervals)
- Pre-configured test scenarios with 17 fake users and 5 fake sessions
- Real email delivery testing

## Development Guidelines

### Adding New Reminder Types

1. **Update `ReminderStatus.ReminderType` enum:**
```java
public enum ReminderType {
    BEFORE_30_MIN,
    AFTER_30_MIN_FEEDBACK,
    YOUR_NEW_TYPE  // Add here
}
```

2. **Update `ReminderSchedulerService` to handle new type:**
```java
// Add new logic in checkAndSendReminders()
```

3. **Add email template in `EmailService`:**
```java
public String buildYourNewReminderHtml(SessionDto session, UserDto user) {
    // Create HTML template
}
```

4. **Add manual trigger support in `ReminderController`:**
```java
// Add case for YOUR_NEW_TYPE
```

### Email Template Customization

Email templates are in `EmailService.java`:
- `buildSessionReminderHtml()` - Session reminder (30 min before)
- `buildFeedbackReminderHtml()` - Feedback request (30 min after)

**Templates are responsive HTML with:**
- Inline CSS for email client compatibility
- Professional branding
- Clear call-to-action buttons
- Session details and timing information

### Scheduler Configuration

**Default:** Runs every 5 minutes (300,000 ms)

**To adjust:**
```properties
reminder.scheduler.session-check-interval=180000  # 3 minutes
```

**To disable for manual testing:**
```properties
reminder.scheduler.enabled=false
```

### API Integration Changes

If your microservice endpoints change:

1. Update endpoint paths in `application.properties`:
```properties
api.gateway.base-url=http://new-gateway-url:8100
api.session.search=/new/path/to/search
api.rarf.get-session-records=/new/rarf/path
api.user.find-email-by-username=/new/user/email/path
```

2. Update request/response mappings in:
   - `SessionApiService.java` - Session search logic
   - `UserApiService.java` - User lookup logic

### Timing Adjustments

**Reminder timing is flexible via configuration:**

```properties
# Send first reminder 45 minutes before (instead of 30)
reminder.before-session.first=45

# Send feedback 60 minutes after (instead of 30)
reminder.after-session.feedback=60
```

**Timing windows are calculated as:** `±5 minutes` to account for scheduler intervals.

### Error Handling

The service is designed for resilience:
- **Email failures:** Logged but don't stop other reminders
- **API failures:** Gracefully handled with error logging
- **Missing data:** Validation checks before sending
- **Duplicate prevention:** Database constraints ensure no duplicate reminders

## Testing Strategy

### Manual Testing Workflow

1. **Start service in test mode:**
```bash
./gradlew bootRun --args="--spring.profiles.active=test"
```

2. **Verify fake data is loaded** (check console for 🧪 emoji)

3. **Wait for scheduler** (runs every 30 seconds in test mode)

4. **Check email inbox** for 12 test emails:
   - 3 emails for session 101 (30-min reminder)
   - 4 emails for session 102 (15-min reminder)
   - 5 emails for session 201 (feedback)

5. **Test manual triggers:**
```powershell
Invoke-WebRequest -Uri "http://localhost:8083/api/reminders/trigger/101?reminderType=BEFORE_30_MIN" -Method POST
```

### Production Testing

Before deploying:
1. Test with actual gateway endpoints (not fake data)
2. Verify database connection
3. Test email delivery to production addresses
4. Confirm scheduler timing is appropriate
5. Monitor logs for errors
6. Test manual trigger endpoints

## Deployment Considerations

### Single Instance Only
**⚠️ Run only ONE instance of this service** to prevent duplicate reminder emails. The reminder tracking system is not designed for multi-instance deployments without external coordination.

### Database Requirement
The service requires MySQL for reminder tracking. Ensure:
- `choroid_db` database exists
- `reminder_status` table is created
- Database credentials are configured

### Production Checklist
- [ ] Replace test Gmail credentials with production email
- [ ] Update `api.gateway.base-url` with actual gateway URL
- [ ] Verify all 3 endpoint paths are correct
- [ ] Set appropriate `SESSION_CHECK_INTERVAL` (5 minutes recommended)
- [ ] Configure logging levels (`INFO` for production)
- [ ] Set up monitoring for email failures
- [ ] Test end-to-end with real sessions
- [ ] Verify duplicate prevention works
- [ ] Ensure only one service instance runs

### Monitoring & Logs

**Key metrics to monitor:**
- Email send success/failure rates
- Scheduler execution frequency
- API call response times
- Database connection health

**Log files contain:**
- Scheduled job executions
- Email sending results
- API call failures
- Duplicate reminder prevention events

**Health check endpoint:** `GET /api/reminders/health`

## Common Issues & Solutions

### Emails Not Sending
1. Verify Gmail app password (not regular password)
2. Check SMTP configuration in logs
3. Test with `/test-email` endpoint first
4. Verify internet connectivity

### No Reminders Being Sent
1. Check if scheduler is enabled (`reminder.scheduler.enabled=true`)
2. Verify sessions exist in gateway with appropriate timing
3. Check logs for API call failures
4. Ensure database connection is working

### Duplicate Reminders
1. Verify only ONE service instance is running
2. Check `reminder_status` table for duplicates
3. Review scheduler timing configuration

### API Integration Failures
1. Verify gateway base URL is correct
2. Test endpoint URLs manually with curl/Postman
3. Check response format matches expected DTOs
4. Review logs for detailed error messages

## API Endpoints Summary

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/reminders/health` | GET | Health check |
| `/api/reminders/stats` | GET | View reminder statistics |
| `/api/reminders/config` | GET | View current configuration |
| `/api/reminders/help` | GET | API documentation |
| `/api/reminders/test-email?email={email}` | POST | Test email configuration |
| `/api/reminders/trigger/{sessionId}?reminderType={type}` | POST | Manual reminder trigger |
| `/api/reminders/check-now` | POST | Manually run scheduler |

## Security Notes

- Database password is hardcoded in `application.properties` - **change for production**
- Gmail credentials are in properties file - **use environment variables in production**
- No authentication on API endpoints - **add security layer if exposing publicly**
- Service intended for internal network use only
