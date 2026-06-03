package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private boolean success;
    private Object data;
    private String message;
    private ErrorDetails errorDetails;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    public static ApiResponse success(Object data, String message) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    public static ApiResponse fail(String code, String message) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(false);
        response.setErrorDetails(ErrorDetails.builder()
                .code(code)
                .message(message)
                .build());
        return response;
    }
}
