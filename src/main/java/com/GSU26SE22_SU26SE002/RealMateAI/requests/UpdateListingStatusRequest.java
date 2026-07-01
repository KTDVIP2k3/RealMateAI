package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingActionEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body của Seller khi tự đổi trạng thái hiển thị bài đăng của mình
 * (PATCH /seller/listings/{id}).
 *
 * action = PAUSE  → tạm ẩn tin đang hiển thị.
 * action = RESUME → bật lại tin đã tạm ẩn (bắt buộc verification hiện tại = APPROVED).
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateListingStatusRequest {

    @NotNull(message = "action không được để trống (PAUSE hoặc RESUME)")
    private SellerListingActionEnum action;
}
