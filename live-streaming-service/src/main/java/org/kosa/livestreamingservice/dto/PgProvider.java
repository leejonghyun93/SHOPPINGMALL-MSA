package org.kosa.livestreamingservice.dto;

public enum PgProvider {
    KAKAOPAY("kakao", "카카오페이"),
    INICIS("html5_inicis", "이니시스"),
    KG("kcp", "KG이니시스"),
    TOSS("toss", "토스페이먼츠"),
    NICE("nice", "나이스페이"),
    PAYCO("payco", "페이코"),
    PAYPAL("paypal", "페이팔"),
    NAVERPAY("naverpay", "네이버페이"),
    SMILEPAY("smilepay", "스마일페이");

    private final String pgCode;
    private final String displayName;

    PgProvider(String pgCode, String displayName) {
        this.pgCode = pgCode;
        this.displayName = displayName;
    }

    public String getPgCode() {
        return pgCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PgProvider fromCode(String pgCode) {
        for (PgProvider provider : values()) {
            if (provider.pgCode.equals(pgCode)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 PG사: " + pgCode);
    }
}