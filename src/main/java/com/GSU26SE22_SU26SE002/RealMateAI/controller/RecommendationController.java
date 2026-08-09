package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recommendation")
@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/api/recommendations/{userId}")
    @Operation(summary = "Gợi ý bất động sản dành riêng cho user (LightFM, sinh sẵn qua batch job offline)")
    public ResponseEntity<ApiResponse> getRecommendations(@PathVariable("userId") Integer userId) {
        return recommendationService.getRecommendationsForUser(userId);
    }
}
