package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageCategoryRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PostingPackageCategoryServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posting-package-categories")
@Tag(name = "Posting Package Category ", description = "APIs quản lý danh mục gói tin")
public class PostingPackageCategoryController {

    @Autowired
    private PostingPackageCategoryServiceInterface postingPackageCategoryServiceInterface;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả danh mục gói tin")
    public ResponseEntity<ApiResponse> getAllCategories() {
        return postingPackageCategoryServiceInterface.getAllCategories();
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Lấy thông tin chi tiết của một danh mục gói tin theo ID")
    public ResponseEntity<ApiResponse> getCategoryById(@PathVariable("categoryId") Integer id) {
        return postingPackageCategoryServiceInterface.getCategoryById(id);
    }

    @PostMapping
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Tạo mới một danh mục gói tin")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> createCategory(@RequestBody PostingPackageCategoryRequest request) {
        return postingPackageCategoryServiceInterface.createCategory(request);
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Cập nhật thông tin danh mục gói tin theo ID")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> updateCategory(@PathVariable("categoryId") Integer id, @RequestBody PostingPackageCategoryRequest request) {
        return postingPackageCategoryServiceInterface.updateCategory(id, request);
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Xóa một danh mục gói tin theo ID")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable("categoryId") Integer id) {
        return postingPackageCategoryServiceInterface.deleteCategory(id);
    }

    @DeleteMapping
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Xóa toàn bộ danh mục gói tin")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> deleteAllCategories() {
        return postingPackageCategoryServiceInterface.deleteAllCategories();
    }
}