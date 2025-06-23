package org.kosa.paymentservice.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "payment.pg")
@Data
public class PgProperties {
    private Map<String, PgConfig> providers;

    @Data
    public static class PgConfig {
        private String apiKey;
        private String apiSecret;
        private String merchantId;
        private boolean enabled;
        private String webhookUrl;
        private int timeout = 30000;
    }
}