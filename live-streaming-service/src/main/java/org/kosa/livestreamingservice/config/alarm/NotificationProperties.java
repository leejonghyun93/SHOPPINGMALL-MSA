package org.kosa.livestreamingservice.config.alarm;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private Email email = new Email();
    private Websocket websocket = new Websocket();
    private Batch batch = new Batch();

    @Data
    public static class Email {
        private Boolean enabled = true;
        private Integer reminderMinutes = 10;
        private String fromEmail;
        private String fromName = "쇼핑몰 라이브방송";
        private Integer retryCount = 3;
        private Long retryDelay = 5000L;
        private String templatePath = "classpath:/templates/email/";
    }

    @Data
    public static class Websocket {
        private Boolean enabled = true;
        private Integer maxConnections = 1000;
        private Long heartbeatInterval = 30000L;
    }

    @Data
    public static class Batch {
        private String reminderJobCron = "0 */5 * * * *";
        private Boolean enabled = true;
        private Integer threadPoolSize = 5;
        private Integer batchSize = 100;
        private Integer maxRetryCount = 3;
    }
}