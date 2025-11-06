package com.ddbs.choroid_reminder_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing RARF (Registration and Feedback) record
 * Matches actual API response from /choroid/rarf/session/{sessionId}/all
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RarfDto {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("userId")
    private String userId; // This is the username
    
    @JsonProperty("feedbackFilled")
    private Boolean feedbackFilled;
    
    @JsonProperty("rating")
    private Integer rating;
    
    @JsonProperty("understandableScore")
    private Integer understandableScore;
    
    @JsonProperty("confidenceScore")
    private Integer confidenceScore;
    
    @JsonProperty("expectationsScore")
    private Integer expectationsScore;
    
    @JsonProperty("engagementScore")
    private Integer engagementScore;
    
    @JsonProperty("organizationScore")
    private Integer organizationScore;
    
    @JsonProperty("relevanceScore")
    private Integer relevanceScore;
    
    @JsonProperty("presenterScore")
    private Integer presenterScore;
    
    @JsonProperty("paceScore")
    private Integer paceScore;
    
    @JsonProperty("mostValuable")
    private String mostValuable;
    
    @JsonProperty("suggestions")
    private String suggestions;
}
