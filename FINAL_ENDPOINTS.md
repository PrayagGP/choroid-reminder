# Final API Endpoints Configuration

## ✅ All 3 Endpoints Configured

### **Gateway Base URL**
```
http://localhost:8100
```

---

### **1. Session Search** (POST)
**Endpoint:** `/choroid/sessions/search`

**For Upcoming Sessions:**
```json
POST http://localhost:8100/choroid/sessions/search
{
  "startTimeFrom": "2024-03-20T14:00:00",
  "startTimeTo": "2024-03-20T14:35:00"
}
```

**For Completed Sessions:**
```json
POST http://localhost:8100/choroid/sessions/search
{
  "endTimeFrom": "2024-03-20T12:55:00",
  "endTimeTo": "2024-03-20T13:35:00"
}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "sessionID": 123,
      "creatorID": 456,
      "creatorUsername": "john_doe",  // ⚠️ Confirm this field exists
      "title": "Session Title",
      "startDateTime": "2024-03-20T14:30:00",
      "duration": 90,
      ...
    }
  ]
}
```

---

### **2. RARF Records** (GET)
**Endpoint:** `/choroid/rarf/session/{sessionId}/all`

**Example:**
```
GET http://localhost:8100/choroid/rarf/session/123/all
```

**Response:**
```json
{
  "success": true,
  "data": ["john_doe", "jane_smith", "alice_wilson"]
}
```

---

### **3. User Email Lookup** (GET)
**Endpoint:** `/users/api/findemail/{username}`

**Example:**
```
GET http://localhost:8100/users/api/findemail/john_doe
```

**Response:**
```json
{
  "success": true,
  "data": "john.doe@example.com"
}
```

---

## 🔄 Data Flow

### Session Reminders (30 min before):
1. POST search → Get sessions starting soon
2. For each session → GET RARF usernames
3. Add creator username to list
4. For each username → GET email
5. Send conductor email to creator, attendee email to others

### Feedback Reminders (30 min after):
1. POST search → Get recently ended sessions
2. For each session → GET RARF usernames
3. Remove creator username
4. For each attendee username → GET email
5. Send feedback email

---

## ⚠️ Important Notes

1. **creatorUsername field:** Confirm that session search returns `creatorUsername` in response
2. **RARF format:** Confirm it returns array of strings (usernames), not objects
3. **Email format:** Confirm it returns plain string email, not object
4. **Datetime format:** Using ISO-8601 format: `2024-03-20T14:30:00`

---

## 🧪 Testing Commands

```bash
# Test session search
curl -X POST http://localhost:8100/choroid/sessions/search \
  -H "Content-Type: application/json" \
  -d '{"startTimeFrom":"2024-03-20T14:00:00","startTimeTo":"2024-03-20T14:35:00"}'

# Test RARF
curl http://localhost:8100/choroid/rarf/session/123/all

# Test email lookup
curl http://localhost:8100/users/api/findemail/john_doe
```
