package com.mkwang.backend.modules.request.entity;

/**
 * Enum representing the type of a request.
 */
public enum RequestType {
  ADVANCE,        // Tạm ứng (Member xin tiền cất vào ví)
  EXPENSE,        // Thanh toán chi phí (Member xin tiền thanh toán cho nhà cung cấp)
  REIMBURSE,      // Hoàn ứng (Member nộp hóa đơn để cấn trừ nợ Tạm ứng)
  PROJECT_TOPUP,  // Xin cấp vốn Dự án (Team Leader gửi Manager)
  QUOTA_TOPUP     // Xin cấp vốn Phòng ban (Manager gửi Admin)
}
