package com.ddbs.choroid_reminder_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic DTO for handling API responses from external services
 * @param <T> The type of data contained in the response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private T data;
    
    @JsonProperty("errors")
    private List<String> errors;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    /**
     * Create successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage("Success");
        return response;
    }
    
    /**
     * Create successful response with message
     */
    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }
    
    /**
     * Create error response
     */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
    
    /**
     * Create error response with errors list
     */
    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrors(errors);
        return response;
    }
}