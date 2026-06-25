package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PostingPackageServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posting-packages")
@Tag(name = "Posting-Package Management", description = "API quản lý các gói dịch vụ đăng tin dành cho cả Seller và Admin")
public class PostingPackageController {

    @Autowired
    private PostingPackageServiceInterface postingPackageServiceInterface;


    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = " Lấy danh sách các gói đăng tin ĐANG HOẠT ĐỘNG",
            description = "Trả về danh sách các gói đã được lọc sẵn (isActive = true) dưới dạng DTO rút gọn để hiển thị trên giao diện mua hàng.")
    public ResponseEntity<ApiResponse> getPostingPackageListIsActive() {
        return postingPackageServiceInterface.getPostingPackageListIsActive();
    }

    @GetMapping(value = "/{id}/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = " Xem chi tiết một gói đăng tin theo ID",
            description = "Tìm kiếm thông tin chi tiết của một gói cụ thể, dữ liệu trả về đã được map qua DTO an toàn.")
    public ResponseEntity<ApiResponse> getPostingPackageDetail(@PathVariable Integer id) {
        return postingPackageServiceInterface.getPostingPackageDetail(id);
    }


    @GetMapping(value = "/admin/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: Lấy TOÀN BỘ danh sách gói đăng tin",
            description = "Trả về toàn bộ các gói dịch vụ bao gồm cả các gói đang hoạt động và các gói đã bị ẩn (Delete mềm). Trả về Entity gốc.")
    public ResponseEntity<ApiResponse> getPostingPackageListByAdmin() {
        return postingPackageServiceInterface.getPostingPackageListByAdmin();
    }

    @PostMapping(value = "/admin/create")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: Tạo MỚI một gói dịch vụ đăng tin",
            description = "Tiến hành kiểm tra trùng tên gói (không phân biệt hoa thường, khoảng trắng). Mặc định gói tạo mới sẽ có trạng thái isActive = true.")
    public ResponseEntity<ApiResponse> createPostingPackage(@RequestBody PostingPackageRequest postingPackageRequest) {
        return postingPackageServiceInterface.createPostingPackage(postingPackageRequest);
    }

    @PutMapping(value = "/admin/{id}/update")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: CẬP NHẬT thông tin gói dịch vụ đăng tin",
            description = "Cập nhật các thông tin như tên, mô tả, giá dựa trên ID. Đồng thời ghi nhận thời gian chỉnh sửa (updatedAt).")
    public ResponseEntity<ApiResponse> updatePostingPackage(@PathVariable Integer id, @RequestBody PostingPackageRequest postingPackageRequest) {
        return postingPackageServiceInterface.updatePostingPackage(id, postingPackageRequest);
    }

    @DeleteMapping(value = "/admin/{id}/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: XÓA MỀM gói dịch vụ đăng tin (Ẩn gói)",
            description = "Thay vì xóa vật lý khỏi database để tránh lỗi toàn vẹn dữ liệu, API này sẽ chuyển trạng thái isActive của gói thành false để ẩn khỏi UI của User.")
    public ResponseEntity<ApiResponse> deletePostingPackage(@PathVariable Integer id) {
        return postingPackageServiceInterface.deletePostingPackage(id);
    }
}