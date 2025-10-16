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
 * NEW 3-ENDPOINT DESIGN:
 * 3. getUsersByIds() - Get user details (especially PersonalEmail) from Users table using list of UserIDs
 * 
 * PLACEHOLDER ENDPOINTS - Replace these with actual endpoints
 * TODO: Update these URLs when actual microservice endpoints are available
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserApiService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${api.gateway.base-url}")
    private String gatewayBaseUrl;
    
    @Value("${api.user.get-users-by-ids}")
    private String usersByIdsEndpoint;
    
    /**
     * Get multiple users by their IDs from Users table
     * Returns user details including PersonalEmail for sending notifications
     * 
     * PLACEHOLDER API CALL - Replace with actual endpoint
     * Expected response format: { "success": true, "data": [UserDto...] }
     */
    public List<UserDto> getUsersByIds(List<Long> userIDs) {
        if (userIDs == null || userIDs.isEmpty()) {
            return Collections.emptyList();
        }
        
        log.info("Fetching {} users by IDs from Users table", userIDs.size());
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(gatewayBaseUrl).build();
            
            // Convert userIDs list to comma-separated string for query parameter
            String userIdParams = userIDs.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
            
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(usersByIdsEndpoint)
                            .queryParam("userIDs", userIdParams)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            TypeReference<ApiResponse<List<UserDto>>> typeRef = new TypeReference<>() {};
            ApiResponse<List<UserDto>> apiResponse = objectMapper.readValue(response, typeRef);
            
            if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                List<UserDto> users = apiResponse.getData();
                log.info("Successfully fetched {} users from Users table", 
                        users != null ? users.size() : 0);
                return users != null ? users : Collections.emptyList();
            } else {
                log.warn("API returned unsuccessful response: {}", apiResponse.getMessage());
                return Collections.emptyList();
            }
            
        } catch (WebClientResponseException e) {
            log.error("HTTP error fetching users by IDs - Status: {}, Body: {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching users by IDs: {}", userIDs, e);
            return Collections.emptyList();
        }
    }
}