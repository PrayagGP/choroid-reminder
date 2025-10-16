package com.ddbs.choroid_reminder_service.service;

import com.ddbs.choroid_reminder_service.dto.SessionDto;
import com.ddbs.choroid_reminder_service.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

/**
 * Email service for sending session reminders and feedback requests
 * Uses Spring Mail with Gmail SMTP
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${reminder.from-email}")
    private String fromEmail;
    
    @Value("${reminder.from-name}")
    private String fromName;
    
    /**
     * Send conductor reminder email (for session creator)
     */
    public boolean sendConductorReminder(UserDto conductor, SessionDto session, int minutesBefore) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(conductor.getPrimaryEmail());
            helper.setSubject("Conductor Reminder: " + session.getTitle() + " - Starting in " + minutesBefore + " minutes");
            
            String htmlContent = buildConductorReminderHtml(conductor, session, minutesBefore);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Conductor reminder sent successfully to {} for session {}", conductor.getPrimaryEmail(), session.getSessionID());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send conductor reminder to {} for session {}", conductor.getPrimaryEmail(), session.getSessionID(), e);
            return false;
        }
    }
    
    /**
     * Send attendee reminder email (for registered participants)
     */
    public boolean sendAttendeeReminder(UserDto attendee, SessionDto session, int minutesBefore) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(attendee.getPrimaryEmail());
            helper.setSubject("Session Reminder: " + session.getTitle() + " - Starting in " + minutesBefore + " minutes");
            
            String htmlContent = buildAttendeeReminderHtml(attendee, session, minutesBefore);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Attendee reminder sent successfully to {} for session {}", attendee.getPrimaryEmail(), session.getSessionID());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send attendee reminder to {} for session {}", attendee.getPrimaryEmail(), session.getSessionID(), e);
            return false;
        }
    }
    
    /**
     * Send feedback request email (30 minutes after session ends)
     */
    public boolean sendFeedbackReminder(UserDto user, SessionDto session) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(user.getPrimaryEmail());
            helper.setSubject("Feedback Request: " + session.getTitle() + " - Your input matters!");
            
            String htmlContent = buildFeedbackReminderHtml(user, session);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Feedback reminder sent successfully to {} for session {}", user.getPrimaryEmail(), session.getSessionID());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send feedback reminder to {} for session {}", user.getPrimaryEmail(), session.getSessionID(), e);
            return false;
        }
    }
    
    /**
     * Build HTML content for conductor reminder email
     */
    private String buildConductorReminderHtml(UserDto conductor, SessionDto session, int minutesBefore) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");
        String formattedDateTime = session.getStartDateTime().format(formatter);
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Conductor Reminder</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #2196F3; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 5px 5px; }
                    .session-details { background: white; padding: 15px; margin: 15px 0; border-radius: 5px; border-left: 4px solid #2196F3; }
                    .cta-button { display: inline-block; background: #2196F3; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .conductor-tasks { background: #e3f2fd; padding: 15px; margin: 15px 0; border-radius: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>👨‍🏫 Time to Conduct Your Session!</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>This is a reminder that it's time to <strong>conduct your session</strong> in <strong>%d minutes</strong>.</p>
                        
                        <div class="session-details">
                            <h3>📅 Your Session Details:</h3>
                            <p><strong>Title:</strong> %s</p>
                            <p><strong>Date & Time:</strong> %s</p>
                            <p><strong>Duration:</strong> %d minutes</p>
                            %s
                            %s
                            %s
                        </div>
                        
                        <div class="conductor-tasks">
                            <h4>📋 Pre-session Checklist:</h4>
                            <ul>
                                <li>✓ Review your session materials and agenda</li>
                                <li>✓ Test your audio/video setup</li>
                                <li>✓ Prepare any screen sharing content</li>
                                <li>✓ Join the meeting room a few minutes early</li>
                                <li>✓ Welcome participants as they join</li>
                            </ul>
                        </div>
                        
                        %s
                        
                        <p>Good luck with your session! Your participants are looking forward to learning from you.</p>
                        
                        <div class="footer">
                            <p>This is an automated reminder from the Choroid Session System.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            conductor.getDisplayName(),
            minutesBefore,
            session.getTitle(),
            formattedDateTime,
            session.getDuration() != null ? session.getDuration() : 60,
            session.getTags() != null ? "<p><strong>Tags:</strong> " + session.getTags() + "</p>" : "",
            session.getMeetingLink() != null ? "<p><strong>Meeting Link:</strong> " + session.getMeetingLink() + "</p>" : "",
            session.getResourcesLink() != null ? "<p><strong>Resources:</strong> " + session.getResourcesLink() + "</p>" : "",
            session.getMeetingLink() != null ? 
                "<a href=\"" + session.getMeetingLink() + "\" class=\"cta-button\">Start Session</a>" : 
                "<p><em>Meeting link will be available before the session.</em></p>"
        );
    }
    
    /**
     * Build HTML content for attendee reminder email
     */
    private String buildAttendeeReminderHtml(UserDto attendee, SessionDto session, int minutesBefore) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");
        String formattedDateTime = session.getStartDateTime().format(formatter);
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Session Reminder</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 5px 5px; }
                    .session-details { background: white; padding: 15px; margin: 15px 0; border-radius: 5px; border-left: 4px solid #4CAF50; }
                    .cta-button { display: inline-block; background: #4CAF50; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔔 Session Reminder</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>This is a friendly reminder that the session you registered for is starting in <strong>%d minutes</strong>.</p>
                        
                        <div class="session-details">
                            <h3>📅 Session Details:</h3>
                            <p><strong>Title:</strong> %s</p>
                            <p><strong>Date & Time:</strong> %s</p>
                            <p><strong>Duration:</strong> %d minutes</p>
                            %s
                            %s
                            %s
                        </div>
                        
                        <p>Please make sure you're ready to join the session. We recommend joining a few minutes early.</p>
                        
                        %s
                        
                        <p>Looking forward to seeing you in the session!</p>
                        
                        <div class="footer">
                            <p>This is an automated reminder from the Choroid Session System.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            attendee.getDisplayName(),
            minutesBefore,
            session.getTitle(),
            formattedDateTime,
            session.getDuration() != null ? session.getDuration() : 60,
            session.getTags() != null ? "<p><strong>Tags:</strong> " + session.getTags() + "</p>" : "",
            session.getMeetingLink() != null ? "<p><strong>Meeting Link:</strong> " + session.getMeetingLink() + "</p>" : "",
            session.getResourcesLink() != null ? "<p><strong>Resources:</strong> " + session.getResourcesLink() + "</p>" : "",
            session.getMeetingLink() != null ? 
                "<a href=\"" + session.getMeetingLink() + "\" class=\"cta-button\">Join Session</a>" : 
                "<p><em>Meeting link will be shared closer to the session time.</em></p>"
        );
    }
    
    /**
     * Build HTML content for feedback reminder email
     */
    private String buildFeedbackReminderHtml(UserDto user, SessionDto session) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String formattedDate = session.getStartDateTime().format(formatter);
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Feedback Request</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #FF9800; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 5px 5px; }
                    .session-details { background: white; padding: 15px; margin: 15px 0; border-radius: 5px; border-left: 4px solid #FF9800; }
                    .cta-button { display: inline-block; background: #FF9800; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .feedback-points { background: white; padding: 15px; margin: 15px 0; border-radius: 5px; }
                    .feedback-points ul { margin: 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📝 We Value Your Feedback!</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>Thank you for participating in our session. We hope you found it valuable and informative.</p>
                        
                        <div class="session-details">
                            <h3>📅 Session You Attended:</h3>
                            <p><strong>Title:</strong> %s</p>
                            <p><strong>Instructor:</strong> %s</p>
                            <p><strong>Date:</strong> %s</p>
                        </div>
                        
                        <p>Your feedback is incredibly important to us and helps us improve our sessions for everyone. Please take a few minutes to share your thoughts.</p>
                        
                        <div class="feedback-points">
                            <h4>Your feedback helps us understand:</h4>
                            <ul>
                                <li>What worked well in the session</li>
                                <li>Areas where we can improve</li>
                                <li>Topics you'd like to see covered in future sessions</li>
                                <li>Overall satisfaction with the learning experience</li>
                            </ul>
                        </div>
                        
                        <div style="text-align: center;">
                            <p><em>Please click the button below to provide your feedback:</em></p>
                            <a href="#" class="cta-button">Give Feedback</a>
                            <p><small>Note: Replace this placeholder link with your actual feedback form URL</small></p>
                        </div>
                        
                        <p>Thank you for taking the time to help us improve. Your input makes a real difference!</p>
                        
                        <div class="footer">
                            <p>This is an automated reminder from the Choroid Session System.</p>
                            <p>If you've already provided feedback, thank you! You can ignore this reminder.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            user.getDisplayName(),
            session.getTitle(),
            "Session Conductor", // No instructor field in new schema
            formattedDate
        );
    }
    
    /**
     * Test email configuration by sending a test email
     */
    public boolean sendTestEmail(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Test Email - Choroid Reminder Service");
            
            String htmlContent = """
                <html>
                <body>
                    <h2>Email Configuration Test</h2>
                    <p>This is a test email from the Choroid Reminder Service.</p>
                    <p>If you're reading this, your email configuration is working correctly!</p>
                    <p>✅ SMTP connection successful</p>
                    <p>✅ HTML email rendering working</p>
                </body>
                </html>
                """;
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Test email sent successfully to {}", toEmail);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send test email to {}", toEmail, e);
            return false;
        }
    }
}