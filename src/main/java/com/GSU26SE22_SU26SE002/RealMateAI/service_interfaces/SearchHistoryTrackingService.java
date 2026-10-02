package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;

/**
 * MỚI: tách riêng khỏi ListingServiceImplement#searchListings — là 1 BEAN
 * RIÊNG (không phải method nội bộ cùng class) để @Transactional(REQUIRES_NEW)
 * có hiệu lực thật (Spring AOP proxy không áp dụng khi tự gọi method cùng
 * class qua "this" — xem pattern tương tự ở UserEventTrackingService).
 *
 * Mục đích REQUIRES_NEW: nếu ghi lịch sử tìm kiếm lỗi (VD keyword quá dài,
 * constraint DB), lỗi đó chỉ rollback transaction CON này — KHÔNG lan ra
 * transaction CHA của searchListings (trước đây chung transaction nên lỗi
 * save() gây UnexpectedRollbackException, khiến search trả 500 dù đã có kết
 * quả xong).
 */
public interface SearchHistoryTrackingService {

    /**
     * Ghi/cập nhật 1 dòng lịch sử tìm kiếm cho account, kèm dọn bớt nếu vượt
     * SEARCH_HISTORY_CAP. KHÔNG throw exception ra ngoài — lỗi chỉ log, không
     * được làm hỏng kết quả search chính.
     *
     * @param account tài khoản đã đăng nhập (bắt buộc khác null — caller tự
     *                 kiểm tra trước khi gọi)
     * @param keyword từ khoá đã trim, caller đảm bảo không blank
     */
    void recordSearchHistory(Account account, String keyword);
}
