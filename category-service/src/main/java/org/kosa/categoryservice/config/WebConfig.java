package org.kosa.categoryservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /icons/** 요청을 classpath:/static/icons/ 폴더로 매핑
        registry.addResourceHandler("/icons/**")
                .addResourceLocations("classpath:/static/icons/");

        // 추가로 캐시 설정도 가능
        registry.addResourceHandler("/icons/**")
                .addResourceLocations("classpath:/static/icons/")
                .setCachePeriod(3600); // 1시간 캐시
    }
}