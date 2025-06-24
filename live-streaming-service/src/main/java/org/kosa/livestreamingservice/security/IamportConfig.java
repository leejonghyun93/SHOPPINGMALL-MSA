package org.kosa.livestreamingservice.security;

import com.siot.IamportRestClient.IamportClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class IamportConfig {

    @Value("${iamport.api-key}")
    private String apiKey;

    @Value("${iamport.api-secret}")
    private String apiSecret;

    @Bean
    public IamportClient iamportClient() {
        return new IamportClient(apiKey, apiSecret);
    }
}
