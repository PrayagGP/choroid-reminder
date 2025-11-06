package com.ddbs.choroid_reminder_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for tracking reminder status and preventing duplicate reminders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderStatus {
    
    private String sessionId;
    private String username;
    private ReminderType reminderType;
    private boolean sent;
    private LocalDateTime sentAt;
    private String emailAddress;
    private String errorMessage;
    private int retryCount;
    
    public enum ReminderType {
        BEFORE_30_MIN("Session starting in 30 minutes"),
        AFTER_30_MIN_FEEDBACK("Please provide session feedback");
        
        private final String description;
        
        ReminderType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Create a new reminder status
     */
    public static ReminderStatus create(String sessionId, String username, ReminderType type, String email) {
        ReminderStatus status = new ReminderStatus();
        status.setSessionId(sessionId);
        status.setUsername(username);
        status.setReminderType(type);
        status.setEmailAddress(email);
        status.setSent(false);
        status.setRetryCount(0);
        return status;
    }
    
    /**
     * Mark reminder as sent
     */
    public void markAsSent() {
        this.sent = true;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }
    
    /**
     * Mark reminder as failed
     */
    public void markAsFailed(String error) {
        this.sent = false;
        this.errorMessage = error;
        this.retryCount++;
    }
    
    /**
     * Check if reminder should be retried
     */
    public boolean shouldRetry() {
        return !sent && retryCount < 3;
    }
}