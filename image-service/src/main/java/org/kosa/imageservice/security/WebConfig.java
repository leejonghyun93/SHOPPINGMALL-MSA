package org.kosa.imageservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${image.upload.path:/uploads/images/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String currentDir = System.getProperty("user.dir");
        String fullUploadPath;

        // 절대 경로인지 확인
        File uploadDir = new File(uploadPath);
        if (uploadDir.isAbsolute()) {
            fullUploadPath = uploadPath;
        } else {
            // 상대 경로인 경우 현재 디렉토리 기준으로 생성
            File imageServiceDir = new File(currentDir, "image-service");
            if (imageServiceDir.exists()) {
                fullUploadPath = currentDir + File.separator + "image-service" + File.separator + uploadPath;
            } else {
                fullUploadPath = currentDir + File.separator + uploadPath;
            }
        }

        // 경로 정규화
        fullUploadPath = fullUploadPath.replace("\\", "/");
        if (!fullUploadPath.endsWith("/")) {
            fullUploadPath += "/";
        }

        System.out.println("=".repeat(80));
        System.out.println("🔍 WebConfig 초기화 중...");
        System.out.println("📁 현재 작업 디렉토리: " + currentDir);
        System.out.println("📁 설정된 업로드 경로: " + uploadPath);
        System.out.println("📁 최종 업로드 경로: " + fullUploadPath);

        File finalUploadDir = new File(fullUploadPath);
        System.out.println("📁 업로드 디렉토리 존재 여부: " + finalUploadDir.exists());

        if (finalUploadDir.exists()) {
            File[] files = finalUploadDir.listFiles();
            if (files != null) {
                System.out.println("📁 디렉토리 내 파일 수: " + files.length);
                for (int i = 0; i < Math.min(files.length, 10); i++) {
                    File file = files[i];
                    System.out.println("  - " + file.getName() + " (" + file.length() + " bytes)");
                }
                if (files.length > 10) {
                    System.out.println("  ... 및 " + (files.length - 10) + "개 파일 더");
                }
            }
        } else {
            System.out.println("❌ 업로드 디렉토리가 존재하지 않습니다!");
        }

        // 🔥 API Gateway를 통한 접근 경로 (주요 경로)
        registry.addResourceHandler("/api/images/products/**")
                .addResourceLocations("file:" + fullUploadPath)
                .setCachePeriod(31536000);

        // 🔥 직접 접근 경로 (백업용)
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + fullUploadPath)
                .setCachePeriod(31536000);

        System.out.println("✅ 리소스 핸들러 등록 완료");
        System.out.println("🔗 주요 URL 매핑: /api/images/products/** -> file:" + fullUploadPath);
        System.out.println("🔗 백업 URL 매핑: /images/** -> file:" + fullUploadPath);
        System.out.println("=".repeat(80));
    }
}