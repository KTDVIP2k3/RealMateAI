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

    private Boolean success;
    private String code;
    private String message;
    private Object data;


    public static ApiResponse success(Object data, String message) {
        return ApiResponse.builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }


    public static ApiResponse fail(String code, String message) {
        return ApiResponse.builder()
                .message(message)
                .build();
    }
}
