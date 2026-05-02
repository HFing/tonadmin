package com.hfing.tonadmin.common;

public enum PaymentStatus {
    UNPAID,          // chưa thanh toán
    PARTIALLY_PAID, // thanh toán một phần
    PAID,            // đã thanh toán đủ
    REFUNDED         // đã hoàn tiền
}
