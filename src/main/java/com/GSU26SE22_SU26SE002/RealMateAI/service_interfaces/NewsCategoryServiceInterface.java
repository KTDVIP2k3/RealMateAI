package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.NewsCategoryRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface NewsCategoryServiceInterface {
    ResponseEntity<ApiResponse> getAllCategories();
    ResponseEntity<ApiResponse> getCategoryById(Integer id);
    ResponseEntity<ApiResponse> createCategory(NewsCategoryRequest request);
    ResponseEntity<ApiResponse> updateCategory(Integer id, NewsCategoryRequest request);
    ResponseEntity<ApiResponse> deleteCategory(Integer id);
}
