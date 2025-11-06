package com.ddbs.choroid_reminder_service.service;

import com.ddbs.choroid_reminder_service.dto.ApiResponse;
import com.ddbs.choroid_reminder_service.dto.RarfDto;
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
import java.util.stream.Collectors;

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
    
    @Value("${api.session.search}")
    private String sessionSearchEndpoint;
    
    @Value("${api.rarf.get-session-records}")
    private String rarfSessionRecordsEndpoint;
    
    /**
     * Get upcoming sessions (starting within the next ~30-35 minutes)
     * Uses POST /choroid/sessions/search with search criteria
     * 
     * Expected response format: { "success": true, "data": [SessionDto...] }
     */
    public List<SessionDto> getUpcomingSessions() {
        log.info("Searching for upcoming sessions using: {}{}", gatewayBaseUrl, sessionSearchEndpoint);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(gatewayBaseUrl).build();
            
            // Calculate time range for upcoming sessions (now to now + 35 minutes)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime futureTime = now.plusMinutes(35);
            
            // Build search criteria for upcoming sessions
            var searchCriteria = java.util.Map.of(
                "startAfter", now.toString(),
                "startBefore", futureTime.toString()
            );
            
            String response = webClient.post()
                    .uri(sessionSearchEndpoint)
                    .header("Content-Type", "application/json")
                    .bodyValue(searchCriteria)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            // Parse the API response
            TypeReference<ApiResponse<List<SessionDto>>> typeRef = new TypeReference<>() {};
            ApiResponse<List<SessionDto>> apiResponse = objectMapper.readValue(response, typeRef);
            
            // Get data from either 'data' or 'items' field (API may not have 'success' field)
            List<SessionDto> sessions = apiResponse.getActualData();
            log.info("Successfully fetched {} upcoming sessions", sessions != null ? sessions.size() : 0);
            return sessions != null ? sessions : Collections.emptyList();
            
        } catch (WebClientResponseException e) {
            log.error("HTTP error searching upcoming sessions - Status: {}, Body: {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error searching upcoming sessions", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get recently completed sessions (ended within the last ~30-60 minutes)
     * Uses POST /choroid/sessions/search with search criteria for completed sessions
     * 
     * Expected response format: { "success": true, "data": [SessionDto...] }
     */
    public List<SessionDto> getRecentlyCompletedSessions() {
        log.info("Searching for recently completed sessions using: {}{}", gatewayBaseUrl, sessionSearchEndpoint);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(gatewayBaseUrl).build();
            
            // Calculate time range for completed sessions
            // We want sessions that started 60-120 minutes ago (assuming typical 60-min duration)
            // This catches sessions that would have ended 30 minutes ago (for 30-min feedback reminder)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime from = now.minusMinutes(120);  // Started 2 hours ago
            LocalDateTime to = now.minusMinutes(30);     // Started 30 minutes ago
            
            // Build search criteria for completed sessions (sessions that started and should have ended)
            var searchCriteria = java.util.Map.of(
                "startAfter", from.toString(),
                "startBefore", to.toString()
            );
            
            String response = webClient.post()
                    .uri(sessionSearchEndpoint)
                    .header("Content-Type", "application/json")
                    .bodyValue(searchCriteria)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            // Parse the API response
            TypeReference<ApiResponse<List<SessionDto>>> typeRef = new TypeReference<>() {};
            ApiResponse<List<SessionDto>> apiResponse = objectMapper.readValue(response, typeRef);
            
            // Get data from either 'data' or 'items' field (API may not have 'success' field)
            List<SessionDto> sessions = apiResponse.getActualData();
            log.info("Successfully fetched {} recently completed sessions", sessions != null ? sessions.size() : 0);
            return sessions != null ? sessions : Collections.emptyList();
            
        } catch (WebClientResponseException e) {
            log.error("HTTP error searching completed sessions - Status: {}, Body: {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error searching completed sessions", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all RARF records for a specific session
     * Returns usernames of attendees who registered for the session
     * Uses GET /choroid/rarf/session/{sessionId}/all
     * 
     * Actual response format: [{"sessionId": "...", "userId": "username", ...}, ...]
     * Returns array of RARF objects directly (not wrapped in ApiResponse)
     */
    public List<String> getRegisteredUsernamesBySession(String sessionID) {
        log.info("Fetching RARF usernames for session {} from: {}{}", sessionID, gatewayBaseUrl, rarfSessionRecordsEndpoint);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(gatewayBaseUrl).build();
            
            String endpoint = rarfSessionRecordsEndpoint.replace("{sessionId}", sessionID);
            
            String response = webClient.get()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            // Parse response - expecting direct array of RARF objects
            TypeReference<List<RarfDto>> typeRef = new TypeReference<>() {};
            List<RarfDto> rarfRecords = objectMapper.readValue(response, typeRef);
            
            // Extract usernames (userId field) from RARF records
            List<String> usernames = rarfRecords.stream()
                    .map(RarfDto::getUserId)
                    .filter(userId -> userId != null && !userId.isEmpty())
                    .collect(Collectors.toList());
            
            log.info("Successfully fetched {} usernames (RARF records) for session {}", usernames.size(), sessionID);
            return usernames;
            
        } catch (WebClientResponseException e) {
            log.error("HTTP error fetching RARF usernames for session {} - Status: {}, Body: {}", 
                     sessionID, e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching RARF usernames for session {}", sessionID, e);
            return Collections.emptyList();
        }
    }
}