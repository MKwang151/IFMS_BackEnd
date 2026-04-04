package com.mkwang.backend.modules.notification.service;

import com.mkwang.backend.common.exception.ResourceNotFoundException;
import com.mkwang.backend.common.exception.UnauthorizedException;
import com.mkwang.backend.modules.notification.dto.response.NotificationDto;
import com.mkwang.backend.modules.notification.entity.Notification;
import com.mkwang.backend.modules.notification.entity.NotificationType;
import com.mkwang.backend.modules.notification.mapper.NotificationMapper;
import com.mkwang.backend.modules.notification.publisher.NotificationEvent;
import com.mkwang.backend.modules.notification.publisher.NotificationPublisher;
import com.mkwang.backend.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPublisher  notificationPublisher;
    private final NotificationMapper     notificationMapper;

    // ── Internal publish (gọi bởi các service khác) ─────────────

    @Override
    public void notify(Long userId, String userEmail, NotificationType type,
                       String title, String message, Long refId, String refType) {
        notificationPublisher.publish(
                new NotificationEvent(userId, userEmail, type.name(), title, message, refId, refType)
        );
    }

    // ── REST ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public Page<NotificationDto> getNotifications(Long userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(notificationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public NotificationDto markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Cannot mark another user's notification as read");
        }

        notification.markAsRead();
        return notificationMapper.toDto(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    @Transactional
    public int deleteReadNotifications() {
        return notificationRepository.deleteAllRead();
    }
}
