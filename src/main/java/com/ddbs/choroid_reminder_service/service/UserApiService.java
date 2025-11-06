package com.ddbs.choroid_reminder_service.service;

import com.ddbs.choroid_reminder_service.dto.ApiResponse;
import com.ddbs.choroid_reminder_service.dto.UserDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for communicating with Users table via API
 * 
 * ENDPOINT:
 * GET /users/api/findemail/{username} - Get email address for a specific username
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserApiService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${api.gateway.base-url}")
    private String gatewayBaseUrl;
    
    @Value("${api.user.find-email-by-username}")
    private String findEmailEndpoint;
    
    /**
     * Get email address for a specific username
     * Uses GET /users/api/findemail/{username}
     * 
     * Actual response format: Plain text email address (e.g., "user@example.com")
     */
    public Optional<String> getEmailByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("Username is null or empty");
            return Optional.empty();
        }
        
        log.info("Fetching email for username: {}", username);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(gatewayBaseUrl).build();
            
            String endpoint = findEmailEndpoint.replace("{username}", username);
            
            String email = webClient.get()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            // Response is plain text email address
            if (email != null && !email.trim().isEmpty() && email.contains("@")) {
                log.info("Successfully fetched email for username {}: {}", username, email.trim());
                return Optional.of(email.trim());
            } else {
                log.warn("No valid email found for username: {} (received: {})", username, email);
                return Optional.empty();
            }
            
        } catch (WebClientResponseException e) {
            log.error("HTTP error fetching email for username {} - Status: {}, Body: {}", 
                     username, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching email for username: {}", username, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get user details with email for multiple usernames
     * Fetches email for each username and builds UserDto objects
     */
    public List<UserDto> getUsersByUsernames(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Collections.emptyList();
        }
        
        log.info("Fetching user details for {} usernames", usernames.size());
        
        List<UserDto> users = new java.util.ArrayList<>();
        
        for (String username : usernames) {
            Optional<String> emailOpt = getEmailByUsername(username);
            
            if (emailOpt.isPresent()) {
                // Create UserDto with available information
                UserDto user = UserDto.builder()
                        .username(username)
                        .personalEmail(emailOpt.get())
                        .name(username) // Using username as display name
                        .build();
                users.add(user);
            } else {
                log.warn("Skipping username {} - no email found", username);
            }
        }
        
        log.info("Successfully created {} user objects from {} usernames", users.size(), usernames.size());
        return users;
    }
}
