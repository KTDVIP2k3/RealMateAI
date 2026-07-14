package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.PageRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NewsServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/news")
@Tag(name = "News API", description = "Các API lấy danh sách bài viết tin tức cho người dùng công cộng")
public class NewsController {

    @Autowired
    private NewsServiceInterface newsServiceInterface;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả bài viết tin tức (Có phân trang cụm 10 bài)")
    public ResponseEntity<ApiResponse> getAllNews(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                                  @RequestParam(name = "size", required = false, defaultValue = "10") int size)
    {
        return newsServiceInterface.getAllNewsPaged(page, size);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Lấy danh sách bài viết tin tức theo ID danh mục chỉ định (Có phân trang)")
    public ResponseEntity<ApiResponse> getNewsByCategory(@PathVariable("categoryId") Integer categoryId, @ModelAttribute PageRequest pageRequest) {
        return newsServiceInterface.getNewsByCategoryIdPaged(categoryId, pageRequest);
    }
}