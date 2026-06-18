package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VerifyListingRequest — Body của Staff/Admin khi duyệt 1 Listing.
 *
 * decision = APPROVED  → cả NỘI DUNG (title, description, giá, thông số Property...)
 *                        VÀ ẢNH (property.images) đã được xem xét và đạt yêu cầu.
 * decision = REJECTED  → bắt buộc có reviewerNote nêu rõ lý do (nội dung sai,
 *                        ảnh không đạt, ảnh không khớp tài sản, v.v.) để Seller sửa lại.
 *
 * KHÔNG có endpoint "duyệt ảnh riêng" và "duyệt nội dung riêng" — vì ảnh và
 * nội dung thuộc về CÙNG MỘT bài đăng, Staff phải xem xét đồng thời cả 2.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class VerifyListingRequest {

    @NotNull(message = "Quyết định duyệt không được để trống (APPROVED hoặc REJECTED)")
    private ListingStatusEnum decision;

    /** Bắt buộc khi decision = REJECTED, không bắt buộc khi APPROVED */
    private String reviewerNote;
}
