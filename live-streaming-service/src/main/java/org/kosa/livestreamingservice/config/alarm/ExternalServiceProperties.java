package org.kosa.livestreamingservice.config.alarm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "external-services")
public class ExternalServiceProperties {

    private String frontendBaseUrl = "http://localhost:3000";
    private String unsubscribeUrl = "http://localhost:3000/unsubscribe";
}