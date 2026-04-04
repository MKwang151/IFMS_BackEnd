package com.mkwang.backend.modules.notification.controller;

import com.mkwang.backend.common.dto.ApiResponse;
import com.mkwang.backend.modules.auth.security.UserDetailsAdapter;
import com.mkwang.backend.modules.notification.dto.response.NotificationDto;
import com.mkwang.backend.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /notifications
     * Lấy danh sách notifications của user hiện tại (mới nhất trước).
     * Query param: unreadOnly=true để chỉ lấy chưa đọc.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getNotifications(
            @AuthenticationPrincipal UserDetailsAdapter principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificationDto> page = notificationService.getNotifications(
                principal.getUser().getId(), unreadOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * GET /notifications/unread-count
     * Số lượng notification chưa đọc — dùng cho badge icon trên UI.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetailsAdapter principal) {

        long count = notificationService.getUnreadCount(principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * PATCH /notifications/{id}/read
     * Đánh dấu 1 notification đã đọc. Chỉ owner mới được phép.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsAdapter principal) {

        NotificationDto dto = notificationService.markAsRead(id, principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * PATCH /notifications/read-all
     * Đánh dấu tất cả notifications của user hiện tại là đã đọc.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetailsAdapter principal) {

        notificationService.markAllAsRead(principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
