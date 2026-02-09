package com.mkwang.backend.modules.request.entity;

/**
 * Enum representing the type of a request.
 */
public enum RequestType {
  ADVANCE, // Tạm ứng (Vay trước)
  EXPENSE, // Thanh toán chi phí (Có chứng từ)
  REIMBURSE // Hoàn ứng (Trả lại tiền đã ứng)
}
