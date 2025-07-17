package org.kosa.commerceservice.config;

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
                .title("Commerce Service API")
                .version("v1.0.0")
                .description("전자상거래 서비스 REST API 문서\n\n" +
                        "이 API는 다음과 같은 기능을 제공합니다:\n" +
                        "- 상품 조회 및 관리\n" +
                        "- 장바구니 관리\n" +
                        "- 주문 처리\n" +
                        "- 결제 처리\n" +
                        "- 찜하기 기능\n" +
                        "- 카테고리 관리\n" +
                        "- 이미지 관리\n\n" +
                        "### 인증 방법\n" +
                        "JWT 토큰을 사용하여 인증합니다.\n" +
                        "우측 상단의 Authorize 버튼을 클릭하여 토큰을 입력하세요.")
                .contact(new Contact()
                        .name("Commerce Service Team")
                        .url("https://commerce.com")
                        .email("dev@commerce.com"))
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

    // 공개 API 그룹
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("공개 API")
                .pathsToMatch("/api/products/**", "/api/categories/**", "/api/images/**")
                .build();
    }

    // 👤 사용자 API 그룹
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .displayName("사용자 API")
                .pathsToMatch("/api/cart/**", "/api/orders/**", "/api/wishlist/**")
                .build();
    }

    // 결제 API 그룹
    @Bean
    public GroupedOpenApi paymentApi() {
        return GroupedOpenApi.builder()
                .group("payment")
                .displayName("결제 API")
                .pathsToMatch("/api/payments/**")
                .build();
    }

    //  관리자 API 그룹
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("관리자 API")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}