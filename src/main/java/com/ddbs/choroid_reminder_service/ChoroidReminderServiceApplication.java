package com.ddbs.choroid_reminder_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Choroid Reminder Service
 * 
 * This service handles:
 * - Sending session reminders 30 and 15 minutes before sessions start
 * - Sending feedback form reminders 30 minutes after sessions end
 * - Integration with session and user microservices
 */
@SpringBootApplication
@EnableScheduling
public class ChoroidReminderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChoroidReminderServiceApplication.class, args);
	}
}