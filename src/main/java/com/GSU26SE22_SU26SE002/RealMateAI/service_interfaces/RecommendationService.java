package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface RecommendationService {

    /**
     * GET /api/recommendations/{userId} — đúng theo API design trong
     * RealMateAI_AI_Architecture.md. CHỈ ĐỌC bảng recommendation_result đã
     * được batch job Python ghi sẵn — không gọi AI service trực tiếp (giai
     * đoạn "local trước", chưa có FastAPI serving realtime).
     */
    ResponseEntity<ApiResponse> getRecommendationsForUser(Integer userId);
}
