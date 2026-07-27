package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageOrderRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PostingPackageOrderServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Posting Package Order", description = "Seller: Quản lý và thanh toán các gói dịch vụ đăng tin")
public class PostingPackageOrderController {

    @Autowired
    private PostingPackageOrderServiceInterface postingPackageOrderServiceInterface;

    @GetMapping("/seller/posting-package-orders")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Lấy danh sách lịch sử đơn hàng mua gói dịch vụ đăng tin của cá nhân")
    public ResponseEntity<ApiResponse> getPostingPackageOrders (
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "0") int size)
    {
        return postingPackageOrderServiceInterface.getPostingPackageOrders(page, size);
    }

    @PostMapping("/seller/posting-package-orders")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Thanh toán mua mới một gói dịch vụ đăng tin cho bài viết cụ thể bằng số dư ví")
    public ResponseEntity<ApiResponse> payPostingPackage(@RequestBody PostingPackageOrderRequest request) {
        return postingPackageOrderServiceInterface.payPostingPackage(request);
    }

    @PostMapping("/seller/posting-package-orders/{orderId}/renew")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Gia hạn gói dịch vụ đăng tin đã mua trước đó cho bài viết bằng số dư ví")
    public ResponseEntity<ApiResponse> renewPostingPackage(@PathVariable("orderId") Integer orderId) {
        return postingPackageOrderServiceInterface.renewPostingPackage(orderId);
    }
}