package org.kosa.livestreamingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 배치 작업을 위한 스케줄링 활성화
@EnableFeignClients
public class LiveSteamingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveSteamingServiceApplication.class, args);
        System.out.println("🚀 알림 서비스가 시작되었습니다!");
        System.out.println("📍 서버 주소: http://localhost:8096");
        System.out.println("🔍 Health Check: http://localhost:8096/api/notifications/health");
    }
}