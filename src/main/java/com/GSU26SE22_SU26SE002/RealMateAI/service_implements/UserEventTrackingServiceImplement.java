package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.UserEventTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.ActiveLog;
import com.GSU26SE22_SU26SE002.RealMateAI.model.AuditLog;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ActiveLogRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AuditLogRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ViewedListingProjection;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.UserEventTrackingService;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ghi nhận User Event (lượt xem tin đăng) qua audit_log/active_log — xem
 * giải thích thiết kế đầy đủ ở AuditLog/ActiveLog.
 *
 * MỖI EVENT = 1 AuditLog (context: ai xem, IP nào) + 1 ActiveLog con
 * (eventType/listingId). Hệ thống dùng JWT stateless nên không gộp nhiều
 * event vào cùng 1 AuditLog theo phiên — mỗi lượt xem là 1 audit_log riêng,
 * đơn giản và đủ dùng cho việc đếm/join.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventTrackingServiceImplement implements UserEventTrackingService {

    private static final int PAGE_SIZE = 10;

    private final AuditLogRepository auditLogRepository;
    private final ActiveLogRepository activeLogRepository;
    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final AuthenUntil authenUntil;

    // REQUIRES_NEW: ghi log là tác vụ PHỤ, không được làm rollback transaction
    // nghiệp vụ chính (vd Investor vẫn phải xem được tin dù ghi log lỗi).
    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void recordSilently(Account account, UserEventTypeEnum eventType, Integer listingId) {
        // SỬA (fix bug đã báo cáo — "get listing detail không đếm count"): TRƯỚC
        // ĐÂY bỏ qua HOÀN TOÀN nếu account=null ("khách ẩn danh không có giá trị
        // thống kê") — nhưng thực tế phần LỚN traffic xem tin trên 1 trang BĐS
        // công khai là KHÁCH VÃNG LAI CHƯA ĐĂNG NHẬP. Bỏ qua nhóm này khiến
        // "Tin nổi bật" (GET /listings/featured, đếm từ ActiveLog) gần như
        // luôn rỗng/sai lệch nặng trong thực tế test — nhìn như bug "không đếm
        // count" dù code có chạy, chỉ là chỉ đếm được rất ít traffic (mỗi lượt
        // xem của user đã đăng nhập). Nay vẫn ghi nhận cho khách ẩn danh
        // (account=null -> lưu AuditLog.account=null, userName="ANONYMOUS",
        // vẫn có ipAddress để tra soát nếu cần) — CHỈ bỏ qua khi thiếu eventType.
        if (eventType == null) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            String ip = resolveClientIp();

            AuditLog auditLog = AuditLog.builder()
                    .account(account)
                    .userName(account != null ? account.getUsername() : "ANONYMOUS")
                    .apiName("USER_EVENT:" + eventType)
                    .ipAddress(ip)
                    .createdAt(now)
                    .build();
            AuditLog savedAuditLog = auditLogRepository.save(auditLog);

            ActiveLog activeLog = ActiveLog.builder()
                    .auditLog(savedAuditLog)
                    .action("TRACK_EVENT")
                    .ipAddress(ip)
                    .createdAt(now)
                    .eventType(eventType)
                    .listingId(listingId)
                    .build();
            activeLogRepository.save(activeLog);

        } catch (Exception e) {
            // Cố ý CHỈ log — xem javadoc UserEventTrackingService#recordSilently.
            log.warn("[UserEventTrackingService] Ghi event lỗi (bỏ qua): accountId={}, eventType={}, listingId={}",
                    account != null ? account.getAccountId() : "ANONYMOUS", eventType, listingId, e);
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getViewCount(Integer listingId) {
        try {
            long count = activeLogRepository.countByListingIdAndEventType(listingId, UserEventTypeEnum.VIEW);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("listingId", listingId);
            result.put("viewCount", count);
            return ResponseEntity.ok(ApiResponse.success(result, "Số lượt xem của tin đăng"));
        } catch (Exception e) {
            log.error("[UserEventTrackingService] getViewCount lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyViewedListings(int page, int size) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", "Cần đăng nhập"));
            }

            int effectiveSize = size > 0 ? size : PAGE_SIZE;
            Pageable pageable = PageRequest.of(Math.max(page, 0), effectiveSize);

            Page<ViewedListingProjection> viewedPage = activeLogRepository
                    .findViewedListingsByAccount(currentUser.getAccountId(), UserEventTypeEnum.VIEW, pageable);

            List<Integer> listingIds = viewedPage.getContent().stream()
                    .map(ViewedListingProjection::getListingId)
                    .collect(Collectors.toList());

            Map<Integer, Listing> listingById = listingRepository.findAllByListingIdInWithDetails(listingIds)
                    .stream().collect(Collectors.toMap(Listing::getListingId, l -> l, (a, b) -> a));

            List<Map<String, Object>> content = viewedPage.getContent().stream()
                    .map(v -> {
                        Listing l = listingById.get(v.getListingId());
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("listingId", v.getListingId());
                        item.put("lastViewedAt", v.getLastViewedAt());
                        item.put("viewCount", v.getViewCount());
                        item.put("listing", l != null ? listingMapper.toListingSummary(l, false) : null);
                        return item;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("page", viewedPage.getNumber());
            result.put("size", viewedPage.getSize());
            result.put("totalElements", viewedPage.getTotalElements());
            result.put("totalPages", viewedPage.getTotalPages());
            result.put("last", viewedPage.isLast());

            return ResponseEntity.ok(ApiResponse.success(result, "Danh sách tin đăng bạn đã xem"));
        } catch (Exception e) {
            log.error("[UserEventTrackingService] getMyViewedListings lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }


    private static final java.util.Set<UserEventTypeEnum> CLIENT_REPORTABLE_EVENTS =
            java.util.Set.of(UserEventTypeEnum.CLICK, UserEventTypeEnum.SHARE, UserEventTypeEnum.CONTACT);

    @Override
    public ResponseEntity<ApiResponse> trackClientEvent(UserEventTypeEnum eventType, Integer listingId) {
        try {
            if (eventType == null || !CLIENT_REPORTABLE_EVENTS.contains(eventType)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(
                        "Bad_Request", "eventType chỉ nhận CLICK, SHARE hoặc CONTACT"));
            }
            if (listingId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(
                        "Bad_Request", "listingId không được để trống"));
            }

            Account currentUser = authenUntil.getCurrentUSer();
            // Khách ẩn danh gọi API này vẫn trả 200 (không chặn UX phía FE) —
            // recordSilently tự bỏ qua nếu account=null, không ghi được gì cả.
            recordSilently(currentUser, eventType, listingId);

            return ResponseEntity.ok(ApiResponse.success(null, "Đã ghi nhận sự kiện"));
        } catch (Exception e) {
            log.error("[UserEventTrackingService] trackClientEvent lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}