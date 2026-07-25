package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.NotificationTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface NotificationService {


    void notify(Account account, String content, NotificationTypeEnum type);

    ResponseEntity<ApiResponse> getMyNotifications(int page, int size);

    ResponseEntity<ApiResponse> getUnreadCount();

    ResponseEntity<ApiResponse> markAsRead(UUID notificationId);

    /** PATCH /notifications/read-all — đánh dấu TOÀN BỘ thông báo của tôi đã đọc. */
    ResponseEntity<ApiResponse> markAllAsRead();
}
