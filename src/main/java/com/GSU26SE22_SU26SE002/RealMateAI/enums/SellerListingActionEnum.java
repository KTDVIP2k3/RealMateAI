package com.GSU26SE22_SU26SE002.RealMateAI.enums;

/**
 * Hành động Seller có thể tự thực hiện trên bài đăng CỦA MÌNH sau khi
 * đã được Staff duyệt (APPROVED) — không liên quan tới quyết định duyệt
 * (đó là quyền của Staff/Admin qua POST /staff/listings/{id}/verify).
 *
 * PAUSE  → tạm ẩn tin (isActive = false), không hiển thị trên Chợ BĐS.
 * RESUME → bật lại tin (isActive = true) — CHỈ khi verification hiện tại = APPROVED.
 */
public enum SellerListingActionEnum {
    PAUSE,
    RESUME
}
