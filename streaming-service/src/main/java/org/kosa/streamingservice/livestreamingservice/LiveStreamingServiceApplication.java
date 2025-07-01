package org.kosa.streamingservice.livestreamingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LiveStreamingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveStreamingServiceApplication.class, args);
    }

}
