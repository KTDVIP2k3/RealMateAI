package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;


import com.GSU26SE22_SU26SE002.RealMateAI.enums.NotificationTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Notification;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.NotificationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.NotificationResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NotificationService;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImplement implements NotificationService {

    private static final int PAGE_SIZE = 10;

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthenUntil authenUntil;

    @Override
    @Transactional
    public void notify(Account account, String content, NotificationTypeEnum type) {
        if (account == null) return;
        try {
            Notification notification = Notification.builder()
                    .account(account)
                    .content(content)
                    .type(type)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            Notification saved = notificationRepository.save(notification);

            NotificationResponse payload = toResponse(saved);
            String destination = "/topic/notifications/" + account.getAccountId();
            messagingTemplate.convertAndSend(destination, payload);

            log.info("[NotificationService] Đã tạo + bắn WS thông báo tới accountId={} destination={}",
                    account.getAccountId(), destination);
        } catch (Exception e) {
            // Cố ý CHỈ log — lỗi gửi thông báo KHÔNG được làm hỏng luồng nghiệp vụ chính
            // (vd tạo tin đăng thành công vẫn phải trả 201 dù bắn thông báo lỗi).
            log.warn("[NotificationService] Gửi thông báo lỗi (bỏ qua): accountId={}",
                    account.getAccountId(), e);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyNotifications(int page, int size) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", "Cần đăng nhập"));
            }

            int effectiveSize = size > 0 ? size : PAGE_SIZE;
            Pageable pageable = PageRequest.of(Math.max(page, 0), effectiveSize);

            Page<Notification> notificationPage = notificationRepository
                    .findByAccount_AccountIdOrderByCreatedAtDesc(currentUser.getAccountId(), pageable);

            List<NotificationResponse> content = notificationPage.getContent().stream()
                    .map(this::toResponse).collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("page", notificationPage.getNumber());
            result.put("size", notificationPage.getSize());
            result.put("totalElements", notificationPage.getTotalElements());
            result.put("totalPages", notificationPage.getTotalPages());
            result.put("last", notificationPage.isLast());
            result.put("unreadCount", notificationRepository.countByAccount_AccountIdAndIsReadFalse(currentUser.getAccountId()));

            return ResponseEntity.ok(ApiResponse.success(result, "Danh sách thông báo của bạn"));
        } catch (Exception e) {
            log.error("[NotificationService] getMyNotifications lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getUnreadCount() {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", "Cần đăng nhập"));
            }
            long count = notificationRepository.countByAccount_AccountIdAndIsReadFalse(currentUser.getAccountId());
            return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count), "Số thông báo chưa đọc"));
        } catch (Exception e) {
            log.error("[NotificationService] getUnreadCount lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> markAsRead(UUID notificationId) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", "Cần đăng nhập"));
            }
            Notification notification = notificationRepository
                    .findByIdAndAccountId(notificationId, currentUser.getAccountId()).orElse(null);
            if (notification == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Thông báo không tồn tại hoặc không thuộc về bạn"));
            }
            notification.setIsRead(true);
            notificationRepository.save(notification);
            return ResponseEntity.ok(ApiResponse.success(toResponse(notification), "Đã đánh dấu đã đọc"));
        } catch (Exception e) {
            log.error("[NotificationService] markAsRead lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .content(n.getContent())
                .type(n.getType() != null ? n.getType().name() : null)
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
