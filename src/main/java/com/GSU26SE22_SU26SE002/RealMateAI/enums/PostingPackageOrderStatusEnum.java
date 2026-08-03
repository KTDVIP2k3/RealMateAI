package com.GSU26SE22_SU26SE002.RealMateAI.enums;

/**
 * MỚI: Trạng thái thanh toán của PostingPackageOrder — tách biệt với field
 * `isActive` đã có sẵn (isActive = gói dịch vụ có đang THỰC SỰ chạy cho
 * Listing hay không, chỉ true khi thanh toán SUCCESS và Listing đã được
 * Staff duyệt/kích hoạt).
 *
 *   PENDING : vừa tạo, đang chờ xử lý (hiếm khi tồn tại lâu vì thanh toán
 *             luôn được thử ngay trong cùng request tạo order).
 *   SUCCESS : thanh toán thành công — Listing đã chuyển WAITING_PAYMENT
 *             -> PENDING (hoặc kích hoạt ngay nếu Listing đã APPROVED).
 *   FAILED  : thanh toán thất bại (ví chưa có / không đủ tiền) — Listing
 *             VẪN ở WAITING_PAYMENT, order này có thể gọi lại
 *             POST /seller/posting-package-orders/{orderId}/retry-pay để
 *             thanh toán lại mà KHÔNG cần tạo order mới.
 */
public enum PostingPackageOrderStatusEnum {
    PENDING,
    SUCCESS,
    FAILED
}
