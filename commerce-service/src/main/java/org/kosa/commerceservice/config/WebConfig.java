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
        String actualImagePath = "ui-service/public/images/banners/products/";

        // 디렉토리가 없으면 생성
        File uploadDir = new File(hardcodedPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
            System.out.println("업로드 디렉토리 생성: " + hardcodedPath);
        }

        System.out.println("=".repeat(80));
        System.out.println("WebConfig 초기화 - 다중 경로 설정");
        System.out.println("업로드 경로: " + hardcodedPath);
        System.out.println("실제 이미지 경로: " + actualImagePath);
        System.out.println("디렉토리 존재: " + uploadDir.exists());

        // 리소스 핸들러 등록 - 다중 경로 지원
        registry.addResourceHandler("/images/**")
                .addResourceLocations(
                        "file:" + hardcodedPath,
                        "file:" + actualImagePath
                )
                .setCachePeriod(31536000);

        registry.addResourceHandler("/api/images/products/**")
                .addResourceLocations(
                        "file:" + hardcodedPath,
                        "file:" + actualImagePath
                )
                .setCachePeriod(31536000);

        System.out.println("리소스 핸들러 등록 완료 - 다중 경로");
        System.out.println("=".repeat(80));
    }
}