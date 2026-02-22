package com.mkwang.backend.modules.wallet.entity;

public enum ReferenceType {
    REQUEST,            // Giao dịch sinh ra từ quá trình duyệt đơn (Tạm ứng, Thanh toán, Hoàn ứng)
    PAYSLIP,            // Giao dịch sinh ra từ kỳ lương (Nhận lương Net, Cấn trừ nợ tự động)
    SYSTEM_FUND,        // Giao dịch Nạp/Rút trực tiếp bằng tay vào Quỹ Công Ty (Top-up / Withdraw)
    EXTERNAL_DEPOSIT,   // Giao dịch Nhân viên tự nạp tiền vào ví qua MoMo/VNPay để trả nợ
    MANUAL_ADJUSTMENT   // Giao dịch kế toán điều chỉnh thủ công (Rollback, sửa sai)
}