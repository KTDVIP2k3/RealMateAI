package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body của Seller khi tự đổi trạng thái hiển thị bài đăng của mình
 * (PATCH /seller/listings/{id}).
 *
 * "status" là trạng thái ĐÍCH Seller muốn chuyển tới — dùng CHUNG enum
 * SellerListingStatusEnum với chính cột lưu trữ Listing.status (không còn
 * enum "action" riêng để tránh trùng khái niệm, xem SellerListingStatusEnum).
 *
 * status = HIDDEN  → tạm ẩn tin đang hiển thị (có thể chuyển lại ACTIVE).
 * status = DELETED → xoá mềm VĨNH VIỄN (không thể sửa/chuyển lại — biến mất
 *                     hoàn toàn khỏi danh sách quản lý của Seller).
 * status = ACTIVE  → mở lại tin đang HIDDEN (bắt buộc verification hiện tại
 *                     = APPROVED). KHÔNG thể chuyển 1 tin đã DELETED.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateListingStatusRequest {

    @NotNull(message = "status không được để trống (ACTIVE, HIDDEN hoặc DELETED)")
    private SellerListingStatusEnum status;
}
