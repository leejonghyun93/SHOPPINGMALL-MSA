package org.kosa.livestreamingservice.config.alarm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "external-services")
public class ExternalServiceProperties {


    private String frontendBaseUrl = "${PROD_FRONTEND_BASE_URL:https://shopmall.com}";
    private String unsubscribeUrl = "${PROD_UNSUBSCRIBE_URL:https://shopmall.com/unsubscribe}";
    private String userServiceDetailUrl = "${PROD_USER_SERVICE_DETAIL_URL:http://user-service:8103/api/users}";
    private String broadcastServiceUrl = "${PROD_BROADCAST_SERVICE_URL:http://live-streaming-service:8096/api/broadcasts}";
}