package com.ddbs.choroid_reminder_service.service;

import com.ddbs.choroid_reminder_service.dto.ReminderStatus;
import com.ddbs.choroid_reminder_service.dto.SessionDto;
import com.ddbs.choroid_reminder_service.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main scheduler service that coordinates all reminder functionality
 * 
 * This service:
 * - Checks for upcoming sessions every 5 minutes
 * - Sends 30-minute reminders before sessions start
 * - Sends feedback reminders for sessions completed within past 10 minutes
 * - Prevents duplicate reminder sending
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "reminder.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderSchedulerService {
    
    private final SessionApiService sessionApiService;
    private final UserApiService userApiService;
    private final EmailService emailService;
    
    @Value("${reminder.before-session.first:30}")
    private int reminderMinutes;
    
    @Value("${reminder.after-session.feedback:30}")
    private int feedbackReminderMinutes;
    
    // In-memory storage for tracking sent reminders (in production, consider using a database)
    private final Map<String, ReminderStatus> sentReminders = new ConcurrentHashMap<>();
    
    /**
     * Main scheduler method - runs every 5 minutes
     * Checks for sessions that need reminders
     */
    @Scheduled(fixedDelayString = "${reminder.scheduler.session-check-interval:300000}")
    public void checkAndSendReminders() {
        log.info("Starting scheduled reminder check at {}", LocalDateTime.now());
        
        try {
            // Process upcoming sessions for pre-session reminders
            processUpcomingSessions();
            
            // Process completed sessions for feedback reminders
            processCompletedSessions();
            
            // Clean up old reminder statuses
            cleanupOldReminders();
            
        } catch (Exception e) {
            log.error("Error during scheduled reminder check", e);
        }
        
        log.info("Completed scheduled reminder check at {}", LocalDateTime.now());
    }
    
    /**
     * Process upcoming sessions and send pre-session reminders
     * UPDATED LOGIC: Use filtered endpoint to get only sessions starting within ~30-35 minutes
     */
    private void processUpcomingSessions() {
        log.info("Processing upcoming sessions for reminders");
        
        // Get pre-filtered upcoming sessions from the API
        List<SessionDto> upcomingSessions = sessionApiService.getUpcomingSessions();
        
        log.info("Found {} upcoming sessions from filtered endpoint", upcomingSessions.size());
        
        for (SessionDto session : upcomingSessions) {
            try {
                long minutesUntilStart = session.getMinutesUntilStart();
                log.info("Session '{}' (ID: {}) starts in {} minutes", session.getTitle(), session.getSessionID(), minutesUntilStart);
                
                // Check if we need to send reminder (30 minutes)
                if (shouldSendReminder(minutesUntilStart, reminderMinutes)) {
                    log.info("✓ Timing criteria met - sending reminders for session {}", session.getSessionID());
                    sendSessionReminders(session, reminderMinutes, ReminderStatus.ReminderType.BEFORE_30_MIN);
                } else {
                    log.info("✗ Timing criteria NOT met (need 10-30 mins, got {} mins) - skipping session {}", 
                            minutesUntilStart, session.getSessionID());
                }
                
            } catch (Exception e) {
                log.error("Error processing session {} for reminders", session.getSessionID(), e);
            }
        }
    }
    
    /**
     * Process completed sessions and send feedback reminders
     * UPDATED LOGIC: Use filtered endpoint to get sessions completed within past 10 minutes
     */
    private void processCompletedSessions() {
        log.info("Processing completed sessions for feedback reminders");
        
        // Get pre-filtered recently completed sessions from the API
        List<SessionDto> completedSessions = sessionApiService.getRecentlyCompletedSessions();
        
        log.info("Found {} recently completed sessions from filtered endpoint", completedSessions.size());
        
        for (SessionDto session : completedSessions) {
            try {
                long minutesSinceEnd = session.getMinutesSinceEnd();
                
                // Check if we need to send feedback reminder (30 minutes after end)
                if (shouldSendFeedbackReminder(minutesSinceEnd, feedbackReminderMinutes)) {
                    sendFeedbackReminders(session);
                }
                
            } catch (Exception e) {
                log.error("Error processing completed session {} for feedback reminders", session.getSessionID(), e);
            }
        }
    }
    
    /**
     * Send session reminders: conductor reminder to creator, attendee reminders to registered users
     * UPDATED LOGIC: Works with usernames from RARF table, fetches emails individually
     */
    private void sendSessionReminders(SessionDto session, int minutesBefore, ReminderStatus.ReminderType reminderType) {
        log.info("Sending {} reminders for session {}: {}", reminderType, session.getSessionID(), session.getTitle());
        
        // Step 1: Get registered usernames from RARF table
        List<String> registeredUsernames = sessionApiService.getRegisteredUsernamesBySession(session.getSessionID());
        
        // Step 2: Add the session creator username to the list (they should also get a reminder)
        List<String> allUsernames = new ArrayList<>(registeredUsernames);
        if (session.getCreatorUsername() != null && !allUsernames.contains(session.getCreatorUsername())) {
            allUsernames.add(session.getCreatorUsername());
        }
        
        if (allUsernames.isEmpty()) {
            log.warn("No usernames found for session {}, skipping reminders", session.getSessionID());
            return;
        }
        
        // Step 3: Get user details (with emails) for all usernames
        List<UserDto> allUsers = userApiService.getUsersByUsernames(allUsernames);
        
        int successCount = 0;
        int failureCount = 0;
        
        for (UserDto user : allUsers) {
            try {
                String reminderKey = generateReminderKey(session.getSessionID(), user.getUsername(), reminderType);
                
                // Check if reminder already sent
                if (sentReminders.containsKey(reminderKey)) {
                    log.debug("Reminder already sent: {}", reminderKey);
                    continue;
                }
                
                // Validate user has email
                if (!user.hasValidEmail()) {
                    log.warn("User {} has invalid email, skipping reminder", user.getUsername());
                    continue;
                }
                
                // Create reminder status
                ReminderStatus reminderStatus = ReminderStatus.create(
                    session.getSessionID(), 
                    user.getUsername(), 
                    reminderType, 
                    user.getPrimaryEmail()
                );
                
                // Send appropriate email based on role (conductor vs attendee)
                boolean emailSent;
                if (user.getUsername().equals(session.getCreatorUsername())) {
                    // This is the session creator - send conductor reminder
                    emailSent = emailService.sendConductorReminder(user, session, minutesBefore);
                    log.info("Conductor reminder sent to {} for session {}", user.getPrimaryEmail(), session.getSessionID());
                } else {
                    // This is a registered participant - send attendee reminder
                    emailSent = emailService.sendAttendeeReminder(user, session, minutesBefore);
                    log.info("Attendee reminder sent to {} for session {}", user.getPrimaryEmail(), session.getSessionID());
                }
                
                if (emailSent) {
                    reminderStatus.markAsSent();
                    successCount++;
                } else {
                    reminderStatus.markAsFailed("Email sending failed");
                    failureCount++;
                    log.warn("Failed to send reminder to {} for session {}", user.getPrimaryEmail(), session.getSessionID());
                }
                
                // Store reminder status
                sentReminders.put(reminderKey, reminderStatus);
                
            } catch (Exception e) {
                failureCount++;
                log.error("Error sending reminder to user {} for session {}", user.getUsername(), session.getSessionID(), e);
            }
        }
        
        log.info("Session reminder batch completed for session {}: {} sent, {} failed", 
                session.getSessionID(), successCount, failureCount);
    }
    
    /**
     * Send feedback reminders only to attendees (registered users, excluding the conductor)
     * UPDATED LOGIC: Works with usernames, excludes conductor from feedback
     */
    private void sendFeedbackReminders(SessionDto session) {
        log.info("Sending feedback reminders for session {}: {}", session.getSessionID(), session.getTitle());
        
        // Step 1: Get registered usernames from RARF table
        List<String> registeredUsernames = sessionApiService.getRegisteredUsernamesBySession(session.getSessionID());
        
        // Step 2: Remove the session creator/conductor from feedback recipients
        List<String> feedbackUsernames = new ArrayList<>(registeredUsernames);
        if (session.getCreatorUsername() != null) {
            feedbackUsernames.removeIf(username -> username.equals(session.getCreatorUsername()));
        }
        
        if (feedbackUsernames.isEmpty()) {
            log.info("No attendees found for session {} (excluding conductor), skipping feedback reminders", session.getSessionID());
            return;
        }
        
        // Step 3: Get user details (with emails) for attendees only
        List<UserDto> attendeeUsers = userApiService.getUsersByUsernames(feedbackUsernames);
        
        int successCount = 0;
        int failureCount = 0;
        
        for (UserDto user : attendeeUsers) {
            try {
                String reminderKey = generateReminderKey(session.getSessionID(), user.getUsername(), ReminderStatus.ReminderType.AFTER_30_MIN_FEEDBACK);
                
                // Check if reminder already sent
                if (sentReminders.containsKey(reminderKey)) {
                    log.debug("Feedback reminder already sent: {}", reminderKey);
                    continue;
                }
                
                // Validate user has email
                if (!user.hasValidEmail()) {
                    log.warn("User {} has invalid email, skipping feedback reminder", user.getUsername());
                    continue;
                }
                
                // Create reminder status
                ReminderStatus reminderStatus = ReminderStatus.create(
                    session.getSessionID(), 
                    user.getUsername(), 
                    ReminderStatus.ReminderType.AFTER_30_MIN_FEEDBACK, 
                    user.getPrimaryEmail()
                );
                
                // Send feedback reminder
                boolean emailSent = emailService.sendFeedbackReminder(user, session);
                
                if (emailSent) {
                    reminderStatus.markAsSent();
                    successCount++;
                    log.info("Feedback reminder sent to {} for session {}", user.getPrimaryEmail(), session.getSessionID());
                } else {
                    reminderStatus.markAsFailed("Email sending failed");
                    failureCount++;
                    log.warn("Failed to send feedback reminder to {} for session {}", user.getPrimaryEmail(), session.getSessionID());
                }
                
                // Store reminder status
                sentReminders.put(reminderKey, reminderStatus);
                
            } catch (Exception e) {
                failureCount++;
                log.error("Error sending feedback reminder to user {} for session {}", user.getUsername(), session.getSessionID(), e);
            }
        }
        
        log.info("Feedback reminder batch completed for session {}: {} sent, {} failed", 
                session.getSessionID(), successCount, failureCount);
    }
    
    /**
     * Check if we should send a reminder based on timing
     */
    private boolean shouldSendReminder(long minutesUntilStart, int targetMinutes) {
        // Send reminder if session is 10-30 minutes away (relaxed timing criteria)
        return minutesUntilStart <= 30 && minutesUntilStart >= 10;
    }
    
    /**
     * Check if we should send feedback reminder
     * Send for any session that completed within the past 10 minutes
     */
    private boolean shouldSendFeedbackReminder(long minutesSinceEnd, int targetMinutes) {
        // Send reminder if session ended within the past 10 minutes
        return minutesSinceEnd >= 0 && minutesSinceEnd <= 10;
    }
    
    /**
     * Generate unique key for reminder tracking
     * Uses username instead of user ID
     */
    private String generateReminderKey(String sessionId, String username, ReminderStatus.ReminderType reminderType) {
        return String.format("%s_%s_%s", sessionId, username, reminderType.name());
    }
    
    /**
     * Clean up old reminder statuses (older than 7 days)
     */
    @Scheduled(fixedDelayString = "${reminder.scheduler.cleanup-interval:3600000}")
    public void cleanupOldReminders() {
        log.info("Cleaning up old reminder statuses");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        
        sentReminders.entrySet().removeIf(entry -> {
            ReminderStatus status = entry.getValue();
            return status.getSentAt() != null && status.getSentAt().isBefore(cutoffDate);
        });
        
        log.info("Cleanup completed. Current reminder cache size: {}", sentReminders.size());
    }
    
    /**
     * Manual trigger for testing - send reminders for a specific session
     */
    public String triggerManualReminder(String sessionId, ReminderStatus.ReminderType reminderType) {
        log.info("Manual trigger requested for session {} with reminder type {}", sessionId, reminderType);
        
        try {
            // For manual triggers, we need to check both upcoming and completed sessions
            SessionDto targetSession = null;
            
            // First check upcoming sessions
            List<SessionDto> upcomingSessions = sessionApiService.getUpcomingSessions();
            targetSession = upcomingSessions.stream()
                    .filter(s -> s.getSessionID().equals(sessionId))
                    .findFirst()
                    .orElse(null);
            
            // If not found in upcoming, check completed sessions (for feedback reminders)
            if (targetSession == null && reminderType == ReminderStatus.ReminderType.AFTER_30_MIN_FEEDBACK) {
                List<SessionDto> completedSessions = sessionApiService.getRecentlyCompletedSessions();
                targetSession = completedSessions.stream()
                        .filter(s -> s.getSessionID().equals(sessionId))
                        .findFirst()
                        .orElse(null);
            }
            
            if (targetSession == null) {
                return "Session not found in relevant session list: " + sessionId;
            }
            
            // Send appropriate reminder type
            switch (reminderType) {
                case BEFORE_30_MIN:
                    sendSessionReminders(targetSession, 30, reminderType);
                    break;
                case AFTER_30_MIN_FEEDBACK:
                    sendFeedbackReminders(targetSession);
                    break;
                default:
                    return "Invalid reminder type: " + reminderType;
            }
            
            return "Manual reminder triggered successfully for session " + sessionId;
            
        } catch (Exception e) {
            log.error("Error triggering manual reminder for session {}", sessionId, e);
            return "Error triggering reminder: " + e.getMessage();
        }
    }
    
    /**
     * Get reminder statistics
     */
    public Map<String, Object> getReminderStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalReminders = sentReminders.size();
        long successfulReminders = sentReminders.values().stream()
                .mapToLong(status -> status.isSent() ? 1 : 0)
                .sum();
        long failedReminders = totalReminders - successfulReminders;
        
        stats.put("totalReminders", totalReminders);
        stats.put("successfulReminders", successfulReminders);
        stats.put("failedReminders", failedReminders);
        stats.put("cacheSize", sentReminders.size());
        
        // Count by type
        Map<String, Long> byType = new HashMap<>();
        sentReminders.values().forEach(status -> {
            String type = status.getReminderType().name();
            byType.put(type, byType.getOrDefault(type, 0L) + 1);
        });
        stats.put("remindersByType", byType);
        
        return stats;
    }
}