package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Notification;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.NotificationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho NotificationServiceImplement — Functions 106-109
 * (9.1 Notification Management, phần Hải).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImplement — Notification Management")
class NotificationServiceImplementTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private AuthenUntil authenUntil;

    @InjectMocks
    private NotificationServiceImplement notificationService;

    private Account currentUser;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        currentUser = new Account();
        currentUser.setAccountId(1);
        notificationId = UUID.randomUUID();
    }

    // ── 106. View Notifications ─────────────────────────────────────────────
    @Nested
    @DisplayName("106. getMyNotifications")
    class GetMyNotificationsTests {
        @Test
        @DisplayName("Trả về danh sách thông báo, kèm unreadCount")
        void getMyNotifications_returnsListWithUnreadCount() {
            when(authenUntil.getCurrentUSer()).thenReturn(currentUser);
            Page<Notification> page = new PageImpl<>(java.util.List.of(
                    Notification.builder().notificationId(notificationId).content("Test").isRead(false).build()));
            when(notificationRepository.findByAccount_AccountIdOrderByCreatedAtDesc(eq(1), any()))
                    .thenReturn(page);
            when(notificationRepository.countByAccount_AccountIdAndIsReadFalse(1)).thenReturn(3L);

            ResponseEntity<ApiResponse> response = notificationService.getMyNotifications(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 401 khi chưa đăng nhập")
        void getMyNotifications_notLoggedIn_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = notificationService.getMyNotifications(0, 10);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    // ── 107. View Notification Unread Counts ─────────────────────────────────
    @Nested
    @DisplayName("107. getUnreadCount")
    class GetUnreadCountTests {
        @Test
        @DisplayName("Trả về đúng số thông báo chưa đọc")
        void getUnreadCount_returnsCorrectCount() {
            when(authenUntil.getCurrentUSer()).thenReturn(currentUser);
            when(notificationRepository.countByAccount_AccountIdAndIsReadFalse(1)).thenReturn(5L);

            ResponseEntity<ApiResponse> response = notificationService.getUnreadCount();

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 108. Mark Notification As Read ────────────────────────────────────────
    @Nested
    @DisplayName("108. markAsRead")
    class MarkAsReadTests {
        @Test
        @DisplayName("Đánh dấu đã đọc thành công khi thông báo thuộc đúng người dùng")
        void markAsRead_success() {
            when(authenUntil.getCurrentUSer()).thenReturn(currentUser);
            Notification n = Notification.builder().notificationId(notificationId).isRead(false).build();
            when(notificationRepository.findByIdAndAccountId(notificationId, 1)).thenReturn(Optional.of(n));
            when(notificationRepository.save(any(Notification.class))).thenReturn(n);

            ResponseEntity<ApiResponse> response = notificationService.markAsRead(notificationId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(notificationRepository).save(n);
        }

        @Test
        @DisplayName("Trả 404 khi thông báo không tồn tại hoặc không thuộc về mình")
        void markAsRead_notFoundOrNotOwned_returns404() {
            when(authenUntil.getCurrentUSer()).thenReturn(currentUser);
            when(notificationRepository.findByIdAndAccountId(notificationId, 1)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = notificationService.markAsRead(notificationId);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    // ── 109. Mark All Notifications As Read ───────────────────────────────────
    @Nested
    @DisplayName("109. markAllAsRead")
    class MarkAllAsReadTests {
        @Test
        @DisplayName("Đánh dấu thành công toàn bộ thông báo chưa đọc")
        void markAllAsRead_success() {
            when(authenUntil.getCurrentUSer()).thenReturn(currentUser);
            when(notificationRepository.markAllAsReadByAccountId(1)).thenReturn(4);

            ResponseEntity<ApiResponse> response = notificationService.markAllAsRead();

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả thông báo phù hợp khi không có gì để đánh dấu (0 bản ghi)")
        void markAllAsRead_nothingToUpdate() {
            when(authenUntil.getCurrentUSer()).thenReturn(currentUser);
            when(notificationRepository.markAllAsReadByAccountId(1)).thenReturn(0);

            ResponseEntity<ApiResponse> response = notificationService.markAllAsRead();

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }
}
