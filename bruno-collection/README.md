# Choroid Reminder Service - Bruno API Testing Guide

Complete Bruno collection for testing the Choroid Reminder Service backend APIs.

## 📋 Prerequisites

1. **Install Bruno**: Download from [usebruno.com](https://www.usebruno.com/)
2. **Start the service**: Run on `http://localhost:8083`
   ```bash
   ./gradlew bootRun
   ```
3. **Database**: MySQL with session data (optional for basic testing)
4. **Email Config**: Gmail SMTP configured in application.properties

## 🚀 Getting Started

### 1. Open Collection in Bruno

1. Launch Bruno
2. Click "Open Collection"
3. Navigate to: `bruno-collection`
4. All API requests will load

### 2. Select Environment

Click "No Environment" → Select "**Local**"

Environment variables:
- `baseUrl`: http://localhost:8083
- `sessionId`: 1 (change to valid session ID)
- `testEmail`: test@example.com (change to your email)

## 📝 API Endpoints Overview

### 1. Health Check ✅
- **Method**: GET
- **Endpoint**: `/api/reminders/health`
- **Purpose**: Verify service is running

**Test Steps**:
1. Click "Health Check"
2. Click "Send"
3. Expect 200 OK with service info

---

### 2. Get Statistics 📊
- **Method**: GET
- **Endpoint**: `/api/reminders/stats`
- **Purpose**: View reminder statistics

**Test Steps**:
1. Click "Get Statistics"
2. Click "Send"
3. View reminder counts and metrics

**Response Example**:
```json
{
  "totalReminders": 150,
  "remindersSentToday": 25,
  "lastExecutionTime": "2024-03-20T14:25:00"
}
```

---

### 3. Get Configuration ⚙️
- **Method**: GET
- **Endpoint**: `/api/reminders/config`
- **Purpose**: View service configuration

**Test Steps**:
1. Click "Get Configuration"
2. Click "Send"
3. Review features and endpoints

**Use Cases**:
- Understand service capabilities
- Discover available reminder types
- Integration documentation

---

### 4. Get Help 📖
- **Method**: GET
- **Endpoint**: `/api/reminders/help`
- **Purpose**: API documentation

**Test Steps**:
1. Click "Get Help"
2. Click "Send"
3. View comprehensive API docs

---

### 5. Test Email 📧
- **Method**: POST
- **Endpoint**: `/api/reminders/test-email?email={{testEmail}}`
- **Purpose**: Verify email configuration

**Test Steps**:
1. Update `testEmail` in environment (your email)
2. Click "Test Email"
3. Click "Send"
4. Check your inbox

**Success Response**:
```json
{
  "message": "Test email sent successfully",
  "status": "success",
  "email": "test@example.com"
}
```

**Common Errors**:
- 400: Invalid email format
- 500: Email configuration issue (check SMTP settings)

**Troubleshooting**:
- Verify Gmail App Password in application.properties
- Check SMTP server connectivity
- Review service logs for detailed error

---

### 6. Trigger 30min Reminder ⏰
- **Method**: POST
- **Endpoint**: `/api/reminders/trigger/{{sessionId}}?reminderType=BEFORE_30_MIN`
- **Purpose**: Send session start reminders

**Test Steps**:
1. Set `sessionId` to valid session ID in environment
2. Click "Trigger 30min Reminder"
3. Click "Send"
4. Check emails for:
   - Session creator (conductor reminder)
   - All attendees (participant reminder)

**Success Response**:
```json
{
  "message": "Reminders sent successfully for session 123",
  "sessionId": "123",
  "reminderType": "BEFORE_30_MIN"
}
```

**Requirements**:
- Session must exist in database
- Session must have creator (CreatorID)
- Attendees should be in RARF table
- Users must have valid email addresses

---

### 7. Trigger Feedback Reminder 📝
- **Method**: POST
- **Endpoint**: `/api/reminders/trigger/{{sessionId}}?reminderType=AFTER_30_MIN_FEEDBACK`
- **Purpose**: Request feedback after session

**Test Steps**:
1. Set `sessionId` to completed session
2. Click "Trigger Feedback Reminder"
3. Click "Send"
4. Verify feedback request emails sent to all participants

**Success Response**:
```json
{
  "message": "Feedback reminders sent successfully for session 123",
  "sessionId": "123",
  "reminderType": "AFTER_30_MIN_FEEDBACK"
}
```

**Recipients**:
- Session creator
- All registered attendees

---

### 8. Trigger Scheduled Check 🔄
- **Method**: POST
- **Endpoint**: `/api/reminders/check-now`
- **Purpose**: Manually run scheduler logic

**Test Steps**:
1. Click "Trigger Scheduled Check"
2. Click "Send"
3. Check service logs for processing
4. Run "Get Statistics" to see results

**What It Does**:
- Checks for upcoming sessions (next 30-35 mins)
- Checks for completed sessions (past 30-60 mins)
- Sends appropriate reminders
- Logs all activity

**Use Cases**:
- Test without waiting for scheduler
- Debug reminder logic
- Integration testing

---

## 🔄 Complete Testing Workflow

### Quick Test (No Database Required)
```
1. Health Check → Verify service UP
2. Get Configuration → See capabilities
3. Get Help → View API docs
4. Test Email → Verify SMTP works
```

### Full Test (With Database)
```
1. Health Check → Service UP
2. Get Statistics → Initial counts
3. Trigger 30min Reminder → Test session reminder
4. Check email inbox → Verify received
5. Trigger Feedback Reminder → Test feedback request
6. Check email inbox → Verify received
7. Get Statistics → See updated counts
8. Trigger Scheduled Check → Test automation
9. Get Statistics → Final counts
```

## 🎯 Environment Variables

| Variable | Description | Default | Notes |
|----------|-------------|---------|-------|
| `baseUrl` | Service URL | http://localhost:8083 | Change if using different port |
| `sessionId` | Session ID for testing | 1 | Update with valid ID from DB |
| `testEmail` | Email for testing | test@example.com | Use your email |

**Editing Variables**:
1. Click environment dropdown
2. Select "Configure"
3. Edit values
4. Save

## 🧪 Testing Scenarios

### Scenario 1: Basic Health Check
```
1. Health Check → 200 OK
2. Get Configuration → View features
3. Get Statistics → Initial state
```

### Scenario 2: Email Configuration
```
1. Test Email (your email) → Check inbox
2. If failed: Check application.properties
3. Verify Gmail App Password
4. Check SMTP logs
```

### Scenario 3: Session Reminders
```
1. Create test session in database (30 mins in future)
2. Add attendees to RARF table
3. Trigger 30min Reminder
4. Verify emails to creator + attendees
5. Check email templates
```

### Scenario 4: Feedback Reminders
```
1. Create completed session in database
2. Add attendees to RARF table
3. Trigger Feedback Reminder
4. Verify feedback request emails
5. Verify feedback link in email
```

### Scenario 5: Automated Check
```
1. Set up sessions in DB (various times)
2. Trigger Scheduled Check
3. Review logs for processing
4. Get Statistics → See what was sent
5. Verify correct sessions triggered
```

## 🔧 Troubleshooting

### Service Not Responding
```bash
# Check if running
curl http://localhost:8083/api/reminders/health

# Start service
./gradlew bootRun

# Check logs
tail -f logs/application.log
```

### No Emails Being Sent

**Check**:
1. Gmail SMTP configuration in application.properties
2. App Password (not regular password)
3. Internet connectivity
4. Email addresses in database

**Test**:
```bash
# Send test email
curl -X POST "http://localhost:8083/api/reminders/test-email?email=your@email.com"
```

### Invalid Session ID

**Error**: Session not found or API unavailable

**Solutions**:
1. Verify session exists in database
2. Check database connection
3. Update `sessionId` environment variable
4. Review API endpoint configuration

### Reminders Not Triggering

**Check**:
1. Scheduler enabled (`reminder.scheduler.enabled=true`)
2. Session timing is correct
3. Users have valid emails
4. RARF table has attendees
5. Check logs for errors

## 📊 Expected HTTP Status Codes

| Endpoint | Success | Error Codes |
|----------|---------|-------------|
| Health Check | 200 OK | - |
| Get Statistics | 200 OK | 500 (Service error) |
| Get Configuration | 200 OK | - |
| Get Help | 200 OK | - |
| Test Email | 200 OK | 400 (Invalid email), 500 (SMTP error) |
| Trigger 30min Reminder | 200 OK | 400 (Invalid type), 500 (Processing error) |
| Trigger Feedback | 200 OK | 400 (Invalid type), 500 (Processing error) |
| Trigger Check | 200 OK | 500 (Processing error) |

## 🔐 Configuration Required

### Email Setup (Gmail)
```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
reminder.from-email=noreply@choroid.com
```

**Get Gmail App Password**:
1. Go to Google Account settings
2. Security → 2-Step Verification
3. App passwords → Generate
4. Use generated password

### Database Setup
```properties
spring.datasource.url=jdbc:mysql://localhost:3307/choroid_db
spring.datasource.username=root
spring.datasource.password=your_password
```

### API Gateway URLs
```properties
api.gateway.base-url=http://localhost:8080/api
api.session.get-upcoming-sessions=/gateway/sessions/upcoming
api.session.get-completed-sessions=/gateway/sessions/completed
api.rarf.get-registered-users=/gateway/rarf/users/{sessionID}
api.user.get-users-by-ids=/gateway/users/batch
```

## 📚 Additional Resources

- **Main README**: See `../README.md` for detailed service documentation
- **Email Templates**: Check `EmailService.java` for HTML templates
- **Scheduler Logic**: Review `ReminderSchedulerService.java`
- **Configuration**: See `application.properties`

## 🎓 Reminder Types

| Type | Description | When Sent | Recipients |
|------|-------------|-----------|------------|
| `BEFORE_30_MIN` | Session starting soon | 30 mins before start | Creator + Attendees |
| `AFTER_30_MIN_FEEDBACK` | Feedback request | 30 mins after end | Creator + Attendees |

## 🚀 Production Deployment Notes

Before production:
1. ✅ Replace placeholder API endpoints
2. ✅ Configure production email credentials
3. ✅ Test with real session data
4. ✅ Set up monitoring and logging
5. ✅ Configure appropriate scheduler interval
6. ✅ Set JVM memory limits
7. ✅ Test email delivery in production

---

**Happy Testing! 🎉**

For issues, check service logs:
```bash
./gradlew bootRun
# or
tail -f logs/application.log
```
