package com.ddbs.choroid_reminder_service.service;

import com.ddbs.choroid_reminder_service.dto.ApiResponse;
import com.ddbs.choroid_reminder_service.dto.SessionDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Service for communicating with Session and RARF tables via API
 * 
 * NEW 3-ENDPOINT DESIGN:
 * 1. getAllSessions() - Get all sessions with SessionID and CreatorID from Sessions table
 * 2. getRegisteredUsersBySession() - Get registered UserIDs from RARF table using SessionID
 * 
 * PLACEHOLDER ENDPOINTS - Replace these with actual endpoints
 * TODO: Update these URLs when actual microservice endpoints are available
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionApiService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${api.gateway.base-url}")
    private String gatewayBaseUrl;
    
    @Value("${api.session.get-upcoming-sessions}")
    private String upcomingSessionsEndpoint;
    
    @Value("${api.session.get-completed-sessions}")
    private String completedSessionsEndpoint;
    
    @Value("${api.rarf.get-registered-users}")
    private String registeredUsersEndpoint;
    
    /**
     * Get upcoming sessions (starting within the next ~30-35 minutes)
     * This endpoint should return sessions filtered by the backend to only include those starting soon
     * 
     * PLACEHOLDER API CALL - Replace with actual endpoint
     * Expected response format: { "success": true, "data": [SessionDto...] }
     */
    public List<SessionDto> getUpcomingSessions() {
        log.info("Fetching upcoming sessions from: {}{}", gatewayBaseUrl, upcomingSessionsEndpoint);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(gatewayBaseUrl).build();
            
            String response = webClient.get()
                    .uri(upcomingSessionsEndpoint)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            // Parse the API response
            TypeReference<ApiResponse<List<SessionDto>>> typeRef = new TypeReference<>() {};
            ApiResponse<List<SessionDto>> apiResponse = objectMapper.readValue(response, typeRef);
            
            if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                List<SessionDto> sessions = apiResponse.getData();
                log.info("Successfully fetched {} upcoming sessions", sessions != null ? sessions.size() : 0);
                return sessions != null ? sessions : Collections.emptyList();
            } else {
                log.warn("API returned unsuccessful response: {}", apiResponse.getMessage());
                return Collections.emptyList();
            }
            
        } catch (WebClientResponseException e) {
            log.error("HTTP error fetching upcoming sessions - Status: {}, Body: {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching upcoming sessions", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get recently completed sessions (ended within the last ~30-60 minutes)
     * This endpoint should return sessions filtered by the backend to only include recently ended sessions
     * 
     * PLACEHOLDER API CALL - Replace with actual endpoint
     * Expected response format: { "success": true, "data": [SessionDto...] }
     */
    public List<SessionDto> getRecentlyCompletedSessions() {
        log.info("Fetching recently completed sessions from: {}{}", gatewayBaseUrl, completedSessionsEndpoint);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(gatewayBaseUrl).build();
            
            String response = webClient.get()
                    .uri(completedSessionsEndpoint)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            // Parse the API response
            TypeReference<ApiResponse<List<SessionDto>>> typeRef = new TypeReference<>() {};
            ApiResponse<List<SessionDto>> apiResponse = objectMapper.readValue(response, typeRef);
            
            if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                List<SessionDto> sessions = apiResponse.getData();
                log.info("Successfully fetched {} recently completed sessions", sessions != null ? sessions.size() : 0);
                return sessions != null ? sessions : Collections.emptyList();
            } else {
                log.warn("API returned unsuccessful response: {}", apiResponse.getMessage());
                return Collections.emptyList();
            }
            
        } catch (WebClientResponseException e) {
            log.error("HTTP error fetching completed sessions - Status: {}, Body: {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching completed sessions", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get registered user IDs for a specific session from RARF table
     * 
     * PLACEHOLDER API CALL - Replace with actual endpoint
     * Expected response format: { "success": true, "data": [123, 456, 789] }
     */
    public List<Long> getRegisteredUsersBySession(Long sessionID) {
        log.info("Fetching registered users for session {} from RARF table", sessionID);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(gatewayBaseUrl).build();
            
            String endpoint = registeredUsersEndpoint.replace("{sessionID}", sessionID.toString());
            
            String response = webClient.get()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            TypeReference<ApiResponse<List<Long>>> typeRef = new TypeReference<>() {};
            ApiResponse<List<Long>> apiResponse = objectMapper.readValue(response, typeRef);
            
            if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                List<Long> userIDs = apiResponse.getData();
                log.info("Successfully fetched {} registered users for session {}", 
                        userIDs != null ? userIDs.size() : 0, sessionID);
                return userIDs != null ? userIDs : Collections.emptyList();
            } else {
                log.warn("API returned unsuccessful response: {}", apiResponse.getMessage());
                return Collections.emptyList();
            }
            
        } catch (WebClientResponseException e) {
            log.error("HTTP error fetching registered users for session {} - Status: {}, Body: {}", 
                     sessionID, e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching registered users for session {}", sessionID, e);
            return Collections.emptyList();
        }
    }
}