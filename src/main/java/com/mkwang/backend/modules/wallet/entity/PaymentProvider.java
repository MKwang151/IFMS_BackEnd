package com.mkwang.backend.modules.wallet.entity;

public enum PaymentProvider {
    PAYOS,          // Cổng VietQR
    MOMO,           // Ví điện tử MoMo
    VNPAY,          // Cổng VNPay
    ZALOPAY,        // Ví ZaloPay
    INTERNAL_WALLET // Giao dịch nội bộ (ví dụ: hoàn ứng, trả lương)
}
