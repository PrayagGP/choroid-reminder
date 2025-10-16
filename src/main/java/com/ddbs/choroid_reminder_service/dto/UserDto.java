package com.ddbs.choroid_reminder_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing user data from Users table
 * Users(UserID, Name, RollNumber, NITKEmail, PersonalEmail, Degree, Major, Minor, Skills, ResumeLink, TeachTags, LearnTags)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    
    @JsonProperty("userID")
    private Long userID;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("rollNumber")
    private String rollNumber;
    
    @JsonProperty("nitkEmail")
    private String nitkEmail;
    
    @JsonProperty("personalEmail")
    private String personalEmail;
    
    @JsonProperty("degree")
    private String degree;
    
    @JsonProperty("major")
    private String major;
    
    @JsonProperty("minor")
    private String minor;
    
    @JsonProperty("skills")
    private String skills;
    
    @JsonProperty("resumeLink")
    private String resumeLink;
    
    @JsonProperty("teachTags")
    private String teachTags;
    
    @JsonProperty("learnTags")
    private String learnTags;
    
    /**
     * Get display name of the user
     */
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        } else if (rollNumber != null && !rollNumber.trim().isEmpty()) {
            return rollNumber;
        } else {
            return "User " + userID;
        }
    }
    
    /**
     * Get the primary email for notifications (personal email preferred)
     */
    public String getPrimaryEmail() {
        if (personalEmail != null && !personalEmail.trim().isEmpty()) {
            return personalEmail;
        } else if (nitkEmail != null && !nitkEmail.trim().isEmpty()) {
            return nitkEmail;
        }
        return null;
    }
    
    /**
     * Check if user has valid email for notifications
     */
    public boolean hasValidEmail() {
        String email = getPrimaryEmail();
        return email != null && 
               email.trim().length() > 0 && 
               email.contains("@") && 
               email.contains(".");
    }
}