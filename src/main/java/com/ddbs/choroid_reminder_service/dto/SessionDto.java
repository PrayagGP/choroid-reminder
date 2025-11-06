package com.ddbs.choroid_reminder_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing session data from Sessions table
 * Matches actual API response structure:
 * {id, creatorId, title, start, duration, tags[], meetingLink, resourcesLink}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    
    @JsonProperty("id")
    @JsonAlias("sessionID") // Accept "sessionID" as an alias
    private String id;
    
    @JsonProperty("creatorId")
    @JsonAlias({"creatorID", "creatorUsername"}) // Accept alternatives
    private String creatorId; // This is the username, not a numeric ID
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("start")
    @JsonAlias("startDateTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime start;
    
    @JsonProperty("duration")
    private Integer duration; // Duration in minutes
    
    @JsonProperty("tags")
    private List<String> tags; // Array of tag strings
    
    @JsonProperty("meetingLink")
    private String meetingLink;
    
    @JsonProperty("resourcesLink")
    private String resourcesLink;
    
    /**
     * Get calculated end time based on start time and duration
     */
    public LocalDateTime getEndDateTime() {
        if (start != null && duration != null) {
            return start.plusMinutes(duration);
        }
        return null;
    }
    
    /**
     * Check if session is upcoming (scheduled but not started)
     */
    public boolean isUpcoming() {
        return start != null && LocalDateTime.now().isBefore(start);
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
        if (start != null && now.isBefore(start)) {
            return java.time.Duration.between(now, start).toMinutes();
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
    
    // Convenience methods for backward compatibility
    public String getSessionID() {
        return id;
    }
    
    public void setSessionID(String sessionID) {
        this.id = sessionID;
    }
    
    public String getCreatorUsername() {
        return creatorId; // creatorId is actually the username
    }
    
    public void setCreatorUsername(String creatorUsername) {
        this.creatorId = creatorUsername;
    }
    
    public LocalDateTime getStartDateTime() {
        return start;
    }
    
    public void setStartDateTime(LocalDateTime startDateTime) {
        this.start = startDateTime;
    }
    
    public String getTagsAsString() {
        return tags != null ? String.join(", ", tags) : null;
    }
}
