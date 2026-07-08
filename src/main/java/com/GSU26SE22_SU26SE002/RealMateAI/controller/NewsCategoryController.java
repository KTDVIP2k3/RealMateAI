package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.NewsCategoryRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NewsCategoryServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/news-categories")
public class NewsCategoryController {

    @Autowired
    private NewsCategoryServiceInterface newsCategoryServiceInterface;

    @GetMapping
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Lấy danh sách tất cả danh mục tin tức")
    public ResponseEntity<ApiResponse> getAllCategories() {
        return newsCategoryServiceInterface.getAllCategories();
    }

    @GetMapping("/{id}")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Lấy thông tin chi tiết của một danh mục theo ID")
    public ResponseEntity<ApiResponse> getCategoryById(@PathVariable Integer id) {
        return newsCategoryServiceInterface.getCategoryById(id);
    }

    @PostMapping
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Tạo mới một danh mục tin tức (isActive tự động gán true)")
    public ResponseEntity<ApiResponse> createCategory(@RequestBody NewsCategoryRequest request) {
        return newsCategoryServiceInterface.createCategory(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Cập nhật tên danh mục tin tức theo ID")
    public ResponseEntity<ApiResponse> updateCategory(@PathVariable Integer id, @RequestBody NewsCategoryRequest request) {
        return newsCategoryServiceInterface.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Xóa một danh mục tin tức theo ID")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Integer id) {
        return newsCategoryServiceInterface.deleteCategory(id);
    }
}