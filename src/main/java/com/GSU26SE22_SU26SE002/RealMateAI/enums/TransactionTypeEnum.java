package com.GSU26SE22_SU26SE002.RealMateAI.enums;
public enum TransactionTypeEnum {
    WALLET_DEPOSIT(41, "Nạp tiền cho ví"),
    WALLET_WITHDRAWAL(42, "Rút tiền cho ví");

    private final int code;
    private final String description;

    TransactionTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
