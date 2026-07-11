package com.GSU26SE22_SU26SE002.RealMateAI.enums;
public enum TransactionTypeEnum {
    WALLET_DEPOSIT(41, "Nạp tiền cho ví"),
    WALLET_WITHDRAWAL(42, "Rút tiền cho ví"),
    POSTING_PACKAGE_PAYMENT(43, "Thanh toán gói dịch vụ đăng tin"),
    POSTING_PACKAGE_RENEWAL(44, "Gia hạn gói dịch vụ đăng tin"),
    MEMBERSHIP_PAYMENT(45, "Thanh toán gói thành viên"),
    MEMBERSHIP_RENEWAL(46, "Gia hạn gói thành viên");

    private final int code;
    private final String description;

    TransactionTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
