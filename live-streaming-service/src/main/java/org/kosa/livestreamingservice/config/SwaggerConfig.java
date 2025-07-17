package org.kosa.livestreamingservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Live Streaming Service API")
                .version("v1.0.0")
                .description("라이브 스트리밍 서비스 REST API 문서\n\n" +
                        "이 API는 다음과 같은 기능을 제공합니다:\n" +
                        "- 방송 관리 및 조회\n" +
                        "- 실시간 채팅\n" +
                        "- 알림 관리\n" +
                        "- 알림 구독 관리\n" +
                        "- 방송 시청자 기능\n\n" +
                        "### 인증 방법\n" +
                        "JWT 토큰을 사용하여 인증합니다.\n" +
                        "우측 상단의 Authorize 버튼을 클릭하여 토큰을 입력하세요.")
                .contact(new Contact()
                        .name("Live Streaming Service Team")
                        .url("https://livestreaming.com")
                        .email("dev@livestreaming.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));

        String jwtSchemeName = "bearerAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    // 전체 API (기본 그룹) - 맨 위에 배치해서 기본 선택되게 함
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("전체 API")
                .pathsToMatch("/api/**")
                .build();
    }

    // 방송 관리 API 그룹
    @Bean
    public GroupedOpenApi broadcastApi() {
        return GroupedOpenApi.builder()
                .group("broadcast")
                .displayName("방송 관리 API")
                .pathsToMatch("/api/broadcasts/**", "/api/broadcast/**")
                .build();
    }

    // 채팅 API 그룹
    @Bean
    public GroupedOpenApi chatApi() {
        return GroupedOpenApi.builder()
                .group("chat")
                .displayName("채팅 API")
                .pathsToMatch("/api/chat/**")
                .build();
    }

    // 알림 API 그룹
    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("notification")
                .displayName("알림 API")
                .pathsToMatch("/api/notifications/**")
                .build();
    }

    // 시청자 API 그룹
    @Bean
    public GroupedOpenApi viewerApi() {
        return GroupedOpenApi.builder()
                .group("viewer")
                .displayName("시청자 API")
                .pathsToMatch("/api/broadcast/**")
                .build();
    }

    // 관리자 API 그룹
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("관리자 API")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}