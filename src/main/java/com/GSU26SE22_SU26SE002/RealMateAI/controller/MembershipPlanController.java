package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.MembershipPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.MembershipPlanServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/membership-plans")
@Tag(name = "Membership-Plan")
public class MembershipPlanController {

    @Autowired
    private MembershipPlanServiceInterface membershipPlanServiceInterface;

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = " Lấy danh sách các gói thành viên ĐANG HOẠT ĐỘNG",
            description = "Trả về danh sách các gói đã được lọc sẵn (isActive = true) dưới dạng DTO rút gọn để hiển thị trên giao diện mua hàng.")
    public ResponseEntity<ApiResponse> getMembershipPlanListIsActive() {
        return membershipPlanServiceInterface.getMembershipPlanListIsActive();
    }

    @GetMapping(value = "/{id}/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = " Xem chi tiết một gói thành viên theo ID",
            description = "Tìm kiếm thông tin chi tiết của một gói cụ thể, dữ liệu trả về đã được map qua DTO an toàn.")
    public ResponseEntity<ApiResponse> getMembershipPlanDetail(@PathVariable Integer id) {
        return membershipPlanServiceInterface.getMembershipPlanDetail(id);
    }

    @GetMapping(value = "/admin/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: Lấy TOÀN BỘ danh sách gói thành viên",
            description = "Trả về toàn bộ các gói dịch vụ bao gồm cả các gói đang hoạt động và các gói đã bị ẩn (Delete mềm). Trả về Entity gốc.")
    public ResponseEntity<ApiResponse> getMembershipPlanListByAdmin() {
        return membershipPlanServiceInterface.getMembershipPlanListByAdmin();
    }

    @PostMapping(value = "/admin/create")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: Tạo MỚI một gói thành viên",
            description = "Tiến hành kiểm tra trùng tên gói (không phân biệt hoa thường, khoảng trắng). Mặc định gói tạo mới sẽ có trạng thái isActive = true.")
    public ResponseEntity<ApiResponse> createMembershipPlan(@RequestBody MembershipPlanRequest membershipPlanRequest) {
        return membershipPlanServiceInterface.createMembershipPlan(membershipPlanRequest);
    }

    @PutMapping(value = "/admin/{id}/update")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: CẬP NHẬT thông tin gói thành viên",
            description = "Cập nhật các thông tin như tên, mô tả, giá dựa trên ID. Đồng thời ghi nhận thời gian chỉnh sửa (updatedAt).")
    public ResponseEntity<ApiResponse> updateMembershipPlan(@PathVariable Integer id, @RequestBody MembershipPlanRequest membershipPlanRequest) {
        return membershipPlanServiceInterface.updateMembershipPlan(id, membershipPlanRequest);
    }

    @DeleteMapping(value = "/admin/{id}/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: XÓA MỀM gói thành viên (Ẩn gói)",
            description = "Thay vì xóa vật lý khỏi database để tránh lỗi toàn vẹn dữ liệu, API này sẽ chuyển trạng thái isActive của gói thành false để ẩn khỏi UI của User.")
    public ResponseEntity<ApiResponse> deleteMembershipPlan(@PathVariable Integer id) {
        return membershipPlanServiceInterface.deleteMembershipPlan(id);
    }

    @PatchMapping(value = "/admin/{id}/toggle-active", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: TẠM NGƯNG hoặc TÁI KÍCH HOẠT gói thành viên",
            description = "Thay đổi trạng thái isActive của gói mà không làm mất dữ liệu. Truyền true để mở lại gói, truyền false để tạm ngưng kinh doanh gói đó.")
    public ResponseEntity<ApiResponse> toggleActiveMembershipPlan(
            @PathVariable Integer id,
            @RequestParam Boolean isActive) {
        return membershipPlanServiceInterface.toggleActiveMembershipPlan(id, isActive);
    }
}