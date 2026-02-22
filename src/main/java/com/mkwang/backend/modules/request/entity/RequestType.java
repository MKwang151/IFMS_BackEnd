package com.mkwang.backend.modules.request.entity;

/**
 * Enum representing the type of a request.
 */
public enum RequestType {
  ADVANCE,      // Tạm ứng (Nhân viên xin tiền cất vào ví)
  EXPENSE,      // Thanh toán chi phí (Nhân viên xin tiền thanh toán thẳng cho nhà cung cấp)
  REIMBURSE,    // Hoàn ứng (Nhân viên nộp hóa đơn để cấn trừ nợ Tạm ứng)

  // THÊM MỚI:
  QUOTA_TOPUP   // Xin cấp vốn/Cấp thêm hạn mức cho Phòng ban (Manager gửi Admin)
}
