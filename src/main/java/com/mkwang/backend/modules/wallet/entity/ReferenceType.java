package com.mkwang.backend.modules.wallet.entity;

/**
 * Enum representing the source entity type that triggered a transaction.
 * Used for polymorphic reference: referenceType + referenceId.
 */
public enum ReferenceType {
    REQUEST,        // Giao dịch sinh ra từ quá trình duyệt đơn (Tạm ứng, Thanh toán, Hoàn ứng)
    PAYSLIP,        // Giao dịch sinh ra từ kỳ lương (Nhận lương Net, Cấn trừ nợ tự động)
    PROJECT,        // Giao dịch cấp vốn cho Dự án (Manager duyệt PROJECT_TOPUP)
    DEPARTMENT,     // Giao dịch cấp vốn cho Phòng ban (Admin duyệt QUOTA_TOPUP)
    SYSTEM          // Điều chỉnh hệ thống (Nạp tiền Quỹ Tổng, System Adjustment)
}