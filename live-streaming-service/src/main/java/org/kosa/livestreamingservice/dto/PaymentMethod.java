package org.kosa.livestreamingservice.dto;

public enum PaymentMethod {
    // 신용카드
    CARD("card", "신용카드"),

    // 계좌이체
    TRANS("trans", "실시간계좌이체"),

    // 가상계좌
    VBANK("vbank", "가상계좌"),

    // 휴대폰
    PHONE("phone", "휴대폰소액결제"),

    // 간편결제
    KAKAOPAY("kakaopay", "카카오페이"),
    NAVERPAY("naverpay", "네이버페이"),
    PAYCO("payco", "페이코"),
    TOSS("toss", "토스페이"),
    SMILEPAY("smilepay", "스마일페이"),

    // 해외결제
    PAYPAL("paypal", "페이팔"),

    // 기타
    POINT("point", "포인트결제"),
    CULTURELAND("cultureland", "문화상품권");

    private final String code;
    private final String displayName;

    PaymentMethod(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentMethod fromCode(String code) {
        for (PaymentMethod method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 결제수단: " + code);
    }
}