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
        String currentDir = System.getProperty("user.dir");
        String fullUploadPath;

        File uploadDir = new File(uploadPath);
        if (uploadDir.isAbsolute()) {
            fullUploadPath = uploadPath;
        } else {
            fullUploadPath = currentDir + File.separator + uploadPath;
        }

        fullUploadPath = fullUploadPath.replace("\\", "/");
        if (!fullUploadPath.endsWith("/")) {
            fullUploadPath += "/";
        }

        System.out.println("=".repeat(80));
        System.out.println(" WebConfig 초기화 중...");
        System.out.println(" 현재 작업 디렉토리: " + currentDir);
        System.out.println(" 설정된 업로드 경로: " + uploadPath);
        System.out.println(" 최종 업로드 경로: " + fullUploadPath);

        File finalUploadDir = new File(fullUploadPath);
        System.out.println(" 업로드 디렉토리 존재 여부: " + finalUploadDir.exists());

        if (finalUploadDir.exists()) {
            File[] files = finalUploadDir.listFiles();
            if (files != null) {
                System.out.println(" 디렉토리 내 파일 수: " + files.length);
                for (int i = 0; i < Math.min(files.length, 10); i++) {
                    File file = files[i];
                    System.out.println("  - " + file.getName() + " (" + file.length() + " bytes)");
                }
                if (files.length > 10) {
                    System.out.println("  ... 및 " + (files.length - 10) + "개 파일 더");
                }
            }
        } else {
            System.out.println("업로드 디렉토리가 존재하지 않습니다!");
        }

        //  API Gateway를 통한 접근 경로 (주요 경로)
        registry.addResourceHandler("/api/images/products/**")
                .addResourceLocations("file:" + fullUploadPath)
                .setCachePeriod(31536000);

        //  직접 접근 경로 (백업용)
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + fullUploadPath)
                .setCachePeriod(31536000);

        //  카테고리 아이콘 경로
        registry.addResourceHandler("/icons/**")
                .addResourceLocations("classpath:/static/icons/")
                .setCachePeriod(3600);

        System.out.println(" 리소스 핸들러 등록 완료");
        System.out.println(" 주요 URL 매핑: /api/images/products/** -> file:" + fullUploadPath);
        System.out.println(" 백업 URL 매핑: /images/** -> file:" + fullUploadPath);
        System.out.println(" 아이콘 URL 매핑: /icons/** -> classpath:/static/icons/");
        System.out.println("=".repeat(80));
    }
}