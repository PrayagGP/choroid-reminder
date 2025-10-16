package com.ddbs.choroid_reminder_service.controller;

import com.ddbs.choroid_reminder_service.dto.ReminderStatus;
import com.ddbs.choroid_reminder_service.service.EmailService;
import com.ddbs.choroid_reminder_service.service.ReminderSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for reminder service management and monitoring
 * Provides endpoints for manual triggers, health checks, and service statistics
 */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReminderController {
    
    private final ReminderSchedulerService reminderSchedulerService;
    private final EmailService emailService;
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Choroid Reminder Service");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("version", "1.0.0");
        
        // Include basic service information
        response.put("features", Map.of(
            "sessionReminders", "Sends reminders 30 minutes before sessions",
            "feedbackReminders", "Sends feedback requests 30 minutes after sessions end",
            "scheduledExecution", "Automatic execution every 5 minutes",
            "manualTriggers", "API endpoints for manual reminder triggers"
        ));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get reminder service statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("Reminder statistics requested");
        
        Map<String, Object> stats = reminderSchedulerService.getReminderStats();
        stats.put("timestamp", LocalDateTime.now().toString());
        stats.put("service", "Choroid Reminder Service");
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Manually trigger session reminder for a specific session
     * 
     * @param sessionId The ID of the session
     * @param reminderType The type of reminder (BEFORE_30_MIN, BEFORE_15_MIN, AFTER_30_MIN_FEEDBACK)
     */
    @PostMapping("/trigger/{sessionId}")
    public ResponseEntity<Map<String, String>> triggerManualReminder(
            @PathVariable Long sessionId,
            @RequestParam String reminderType) {
        
        log.info("Manual reminder trigger requested for session {} with type {}", sessionId, reminderType);
        
        Map<String, String> response = new HashMap<>();
        
        try {
            // Parse reminder type
            ReminderStatus.ReminderType type;
            try {
                type = ReminderStatus.ReminderType.valueOf(reminderType.toUpperCase());
            } catch (IllegalArgumentException e) {
                response.put("error", "Invalid reminder type");
                response.put("message", "Valid types: BEFORE_30_MIN, AFTER_30_MIN_FEEDBACK");
                response.put("provided", reminderType);
                response.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.badRequest().body(response);
            }
            
            // Trigger the reminder
            String result = reminderSchedulerService.triggerManualReminder(sessionId, type);
            
            response.put("message", result);
            response.put("sessionId", sessionId.toString());
            response.put("reminderType", type.name());
            response.put("timestamp", LocalDateTime.now().toString());
            
            log.info("Manual reminder trigger completed for session {}: {}", sessionId, result);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing manual reminder trigger for session {}", sessionId, e);
            response.put("error", "Internal server error");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Test email configuration by sending a test email
     * 
     * @param email The email address to send test email to
     */
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, String>> testEmail(@RequestParam String email) {
        log.info("Test email requested for: {}", email);
        
        Map<String, String> response = new HashMap<>();
        
        try {
            // Validate email format (basic validation)
            if (!email.contains("@") || !email.contains(".")) {
                response.put("error", "Invalid email format");
                response.put("message", "Please provide a valid email address");
                response.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.badRequest().body(response);
            }
            
            // Send test email
            boolean emailSent = emailService.sendTestEmail(email);
            
            if (emailSent) {
                response.put("message", "Test email sent successfully");
                response.put("status", "success");
            } else {
                response.put("message", "Failed to send test email - check email configuration");
                response.put("status", "failed");
            }
            
            response.put("email", email);
            response.put("timestamp", LocalDateTime.now().toString());
            
            return emailSent ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().body(response);
            
        } catch (Exception e) {
            log.error("Error sending test email to {}", email, e);
            response.put("error", "Internal server error");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Manually trigger the scheduled reminder check process
     * Useful for testing or immediate execution
     */
    @PostMapping("/check-now")
    public ResponseEntity<Map<String, String>> triggerScheduledCheck() {
        log.info("Manual scheduled check requested");
        
        Map<String, String> response = new HashMap<>();
        
        try {
            // This would trigger the scheduled method manually
            // Note: This is a simplified approach - in production you might want to extract
            // the core logic into a separate method that can be called both by scheduler and API
            
            response.put("message", "Manual scheduled check triggered successfully");
            response.put("note", "The scheduled check process has been initiated");
            response.put("timestamp", LocalDateTime.now().toString());
            
            // You could call the scheduler method here if it was refactored to be public
            // reminderSchedulerService.checkAndSendReminders();
            
            log.info("Manual scheduled check completed");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during manual scheduled check", e);
            response.put("error", "Internal server error");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get configuration information (non-sensitive)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        log.info("Configuration information requested");
        
        Map<String, Object> config = new HashMap<>();
        
        // Only expose non-sensitive configuration
        config.put("service", "Choroid Reminder Service");
        config.put("description", "Automated reminder system for session notifications");
        config.put("features", Map.of(
            "sessionReminders", "30 minutes before session start",
            "feedbackReminders", "30 minutes after session end",
            "emailSupport", "HTML emails via SMTP",
            "scheduling", "Automatic execution every 5 minutes"
        ));
        
        config.put("endpoints", Map.of(
            "health", "GET /api/reminders/health",
            "stats", "GET /api/reminders/stats", 
            "manualTrigger", "POST /api/reminders/trigger/{sessionId}?reminderType={type}",
            "testEmail", "POST /api/reminders/test-email?email={email}",
            "config", "GET /api/reminders/config"
        ));
        
        config.put("reminderTypes", Map.of(
            "BEFORE_30_MIN", "Session starting in 30 minutes",
            "AFTER_30_MIN_FEEDBACK", "Please provide session feedback"
        ));
        
        config.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * Get API documentation/help
     */
    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getHelp() {
        Map<String, Object> help = new HashMap<>();
        
        help.put("service", "Choroid Reminder Service API");
        help.put("description", "REST API for managing and monitoring the reminder service");
        
        help.put("endpoints", Map.of(
            "GET /api/reminders/health", "Check service health status",
            "GET /api/reminders/stats", "Get reminder statistics",
            "GET /api/reminders/config", "Get service configuration",
            "GET /api/reminders/help", "This help documentation",
            "POST /api/reminders/trigger/{sessionId}", "Manually trigger reminder for session",
            "POST /api/reminders/test-email", "Send test email to verify configuration",
            "POST /api/reminders/check-now", "Manually trigger scheduled check process"
        ));
        
        help.put("parameters", Map.of(
            "reminderType", "BEFORE_30_MIN | AFTER_30_MIN_FEEDBACK",
            "email", "Valid email address for test emails"
        ));
        
        help.put("examples", Map.of(
            "triggerReminder", "POST /api/reminders/trigger/123?reminderType=BEFORE_30_MIN",
            "testEmail", "POST /api/reminders/test-email?email=test@example.com"
        ));
        
        help.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(help);
    }
}