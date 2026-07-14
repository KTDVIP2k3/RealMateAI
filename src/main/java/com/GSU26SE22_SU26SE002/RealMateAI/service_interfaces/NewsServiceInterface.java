package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.PageRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface NewsServiceInterface {
    ResponseEntity<ApiResponse> getAllNewsPaged(int page, int size );
    ResponseEntity<ApiResponse> getNewsByCategoryIdPaged(Integer categoryId, PageRequest pageRequest);
    void autoCrawlNewsData();
}