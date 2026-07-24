package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageCategoryRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface PostingPackageCategoryServiceInterface {
    ResponseEntity<ApiResponse> getAllCategories();
    ResponseEntity<ApiResponse> getCategoryById(Integer id);
    ResponseEntity<ApiResponse> createCategory(PostingPackageCategoryRequest request);
    ResponseEntity<ApiResponse> updateCategory(Integer id, PostingPackageCategoryRequest request);
    ResponseEntity<ApiResponse> deleteCategory(Integer id);
    ResponseEntity<ApiResponse> deleteAllCategories();
}