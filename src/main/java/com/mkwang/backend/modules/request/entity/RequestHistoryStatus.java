package com.mkwang.backend.modules.request.entity;

public enum RequestHistoryStatus {
    PENDING,    // Đang chờ xử lý
    APPROVED,   // Đã duyệt
    REJECTED,   // Đã từ chối
    CANCELED    // Đã hủy (Nhân viên tự hủy trước khi duyệt)
}
