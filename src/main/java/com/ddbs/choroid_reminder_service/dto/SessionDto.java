package com.ddbs.choroid_reminder_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing session data from Sessions table
 * Sessions(SessionID, CreatorID, Title, StartDateTime, Duration, Tags, MeetingLink, ResourcesLink)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    
    @JsonProperty("sessionID")
    private Long sessionID;
    
    @JsonProperty("creatorID")
    private Long creatorID;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("startDateTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateTime;
    
    @JsonProperty("duration")
    private Integer duration; // Duration in minutes
    
    @JsonProperty("tags")
    private String tags;
    
    @JsonProperty("meetingLink")
    private String meetingLink;
    
    @JsonProperty("resourcesLink")
    private String resourcesLink;
    
    /**
     * Get calculated end time based on start time and duration
     */
    public LocalDateTime getEndDateTime() {
        if (startDateTime != null && duration != null) {
            return startDateTime.plusMinutes(duration);
        }
        return null;
    }
    
    /**
     * Check if session is upcoming (scheduled but not started)
     */
    public boolean isUpcoming() {
        return startDateTime != null && LocalDateTime.now().isBefore(startDateTime);
    }
    
    /**
     * Check if session has ended
     */
    public boolean hasEnded() {
        LocalDateTime endTime = getEndDateTime();
        return endTime != null && LocalDateTime.now().isAfter(endTime);
    }
    
    /**
     * Get minutes until session starts
     */
    public long getMinutesUntilStart() {
        LocalDateTime now = LocalDateTime.now();
        if (startDateTime != null && now.isBefore(startDateTime)) {
            return java.time.Duration.between(now, startDateTime).toMinutes();
        }
        return 0;
    }
    
    /**
     * Get minutes since session ended
     */
    public long getMinutesSinceEnd() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = getEndDateTime();
        if (endTime != null && now.isAfter(endTime)) {
            return java.time.Duration.between(endTime, now).toMinutes();
        }
        return 0;
    }
}