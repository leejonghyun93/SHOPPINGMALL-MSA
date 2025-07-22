package org.kosa.commerceservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${image.upload.path:/app/resources/uploads/images/}")
    private String uploadPath;

    @Value("${app.icon.base-url:/icons}")
    private String iconBaseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 하드코딩된 절대 경로 사용
        String hardcodedPath = "/app/resources/uploads/images/";

        // 디렉토리가 없으면 생성
        File uploadDir = new File(hardcodedPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
            System.out.println("업로드 디렉토리 생성: " + hardcodedPath);
        }

        System.out.println("=".repeat(80));
        System.out.println("WebConfig 초기화 - 하드코딩된 경로 사용");
        System.out.println("업로드 경로: " + hardcodedPath);
        System.out.println("디렉토리 존재: " + uploadDir.exists());

        // 리소스 핸들러 등록
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + hardcodedPath)
                .setCachePeriod(31536000);

        registry.addResourceHandler("/api/images/products/**")
                .addResourceLocations("file:" + hardcodedPath)
                .setCachePeriod(31536000);

        System.out.println("리소스 핸들러 등록 완료");
        System.out.println("=".repeat(80));
    }
}