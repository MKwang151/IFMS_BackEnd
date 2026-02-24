package com.mkwang.backend.modules.wallet.entity;

public enum ReferenceType {
    REQUEST,            // Giao dịch sinh ra từ quá trình duyệt đơn (Tạm ứng, Thanh toán, Hoàn ứng)
    PAYSLIP     // Giao dịch sinh ra từ kỳ lương (Nhận lương Net, Cấn trừ nợ tự động)
}