package org.kosa.apigatewayservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 *  Circuit Breaker Fallback Controller
 * 각 서비스별로 적절한 fallback 응답을 제공
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class CircuitBreakerFallbackController {

    /**
     *  장바구니 서비스 Fallback
     */
    @RequestMapping("/cart/**")
    public ResponseEntity<Map<String, Object>> cartServiceFallback() {
        log.warn(" 장바구니 서비스 Circuit Breaker 동작 - Fallback 응답 제공");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "장바구니 서비스가 일시적으로 이용할 수 없습니다. 잠시 후 다시 시도해주세요.");
        fallbackResponse.put("errorCode", "CART_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());
        fallbackResponse.put("data", null);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  결제 서비스 Fallback (가장 중요!)
     */
    @RequestMapping("/payments/**")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        log.error(" 결제 서비스 Circuit Breaker 동작 - 긴급 Fallback 응답!");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "결제 시스템에 문제가 발생했습니다. 고객센터로 문의해주세요.");
        fallbackResponse.put("errorCode", "PAYMENT_SERVICE_CRITICAL_ERROR");
        fallbackResponse.put("timestamp", LocalDateTime.now());
        fallbackResponse.put("supportPhone", "1588-1234");
        fallbackResponse.put("data", null);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  주문 서비스 Fallback
     */
    @RequestMapping("/orders/**")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        log.warn(" 주문 서비스 Circuit Breaker 동작 - Fallback 응답 제공");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "주문 처리 서비스가 일시적으로 이용할 수 없습니다. 장바구니에 담아두시고 잠시 후 시도해주세요.");
        fallbackResponse.put("errorCode", "ORDER_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());
        fallbackResponse.put("data", null);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * 🛍 상품 서비스 Fallback
     */
    @RequestMapping("/products/**")
    public ResponseEntity<Map<String, Object>> productServiceFallback() {
        log.warn(" 상품 서비스 Circuit Breaker 동작 - 캐시된 데이터 또는 기본 응답 제공");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "상품 정보를 불러오는 중 문제가 발생했습니다. 페이지를 새로고침 해주세요.");
        fallbackResponse.put("errorCode", "PRODUCT_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());
        fallbackResponse.put("data", new HashMap<>()); // 빈 상품 목록

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * ️ 카테고리 서비스 Fallback
     */
    @RequestMapping("/categories/**")
    public ResponseEntity<Map<String, Object>> categoryServiceFallback() {
        log.warn(" 카테고리 서비스 Circuit Breaker 동작");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "카테고리 정보를 불러올 수 없습니다.");
        fallbackResponse.put("errorCode", "CATEGORY_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());
        fallbackResponse.put("data", new HashMap<>());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * 🖼 이미지 서비스 Fallback
     */
    @RequestMapping("/images/**")
    public ResponseEntity<Map<String, Object>> imageServiceFallback() {
        log.warn(" 이미지 서비스 Circuit Breaker 동작");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "이미지를 불러올 수 없습니다.");
        fallbackResponse.put("errorCode", "IMAGE_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());
        fallbackResponse.put("defaultImageUrl", "/images/no-image.png");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * ️ 찜하기 서비스 Fallback
     */
    @RequestMapping("/wishlist/**")
    public ResponseEntity<Map<String, Object>> wishlistServiceFallback() {
        log.warn(" 찜하기 서비스 Circuit Breaker 동작");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "찜하기 기능이 일시적으로 이용할 수 없습니다.");
        fallbackResponse.put("errorCode", "WISHLIST_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());
        fallbackResponse.put("data", null);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  사용자 서비스 Fallback
     */
    @RequestMapping("/users/**")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn(" 사용자 서비스 Circuit Breaker 동작");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "사용자 서비스가 일시적으로 이용할 수 없습니다.");
        fallbackResponse.put("errorCode", "USER_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  인증 서비스 Fallback
     */
    @RequestMapping("/auth/**")
    public ResponseEntity<Map<String, Object>> authServiceFallback() {
        log.error(" 인증 서비스 Circuit Breaker 동작 - 중요한 서비스!");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "인증 서비스에 문제가 발생했습니다. 로그인을 다시 시도해주세요.");
        fallbackResponse.put("errorCode", "AUTH_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());
        fallbackResponse.put("redirectUrl", "/login");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  방송 서비스 Fallback
     */
    @RequestMapping("/broadcast/**")
    public ResponseEntity<Map<String, Object>> broadcastServiceFallback() {
        log.warn(" 방송 서비스 Circuit Breaker 동작");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "라이브 방송 서비스가 일시적으로 이용할 수 없습니다.");
        fallbackResponse.put("errorCode", "BROADCAST_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  채팅 서비스 Fallback
     */
    @RequestMapping("/chat/**")
    public ResponseEntity<Map<String, Object>> chatServiceFallback() {
        log.warn(" 채팅 서비스 Circuit Breaker 동작");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "채팅 서비스가 일시적으로 이용할 수 없습니다.");
        fallbackResponse.put("errorCode", "CHAT_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  스트리밍 서비스 Fallback
     */
    @RequestMapping("/streaming/**")
    public ResponseEntity<Map<String, Object>> streamingServiceFallback() {
        log.warn(" 스트리밍 서비스 Circuit Breaker 동작");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "스트리밍 서비스가 일시적으로 이용할 수 없습니다.");
        fallbackResponse.put("errorCode", "STREAMING_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  알림 서비스 Fallback
     */
    @RequestMapping("/notifications/**")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        log.warn(" 알림 서비스 Circuit Breaker 동작");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "알림 서비스가 일시적으로 이용할 수 없습니다.");
        fallbackResponse.put("errorCode", "NOTIFICATION_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  Q&A 서비스 Fallback
     */
    @RequestMapping("/qna/**")
    public ResponseEntity<Map<String, Object>> qnaServiceFallback() {
        log.warn(" Q&A 서비스 Circuit Breaker 동작");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "Q&A 서비스가 일시적으로 이용할 수 없습니다.");
        fallbackResponse.put("errorCode", "QNA_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  게시판 서비스 Fallback
     */
    @RequestMapping("/board/**")
    public ResponseEntity<Map<String, Object>> boardServiceFallback() {
        log.warn(" 게시판 서비스 Circuit Breaker 동작");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "게시판 서비스가 일시적으로 이용할 수 없습니다.");
        fallbackResponse.put("errorCode", "BOARD_SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     *  전역 Fallback (다른 fallback이 매칭되지 않는 경우)
     */
    @RequestMapping("/**")
    public ResponseEntity<Map<String, Object>> globalFallback() {
        log.error(" 전역 Circuit Breaker 동작 - 알 수 없는 서비스 오류");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "서비스가 일시적으로 이용할 수 없습니다. 잠시 후 다시 시도해주세요.");
        fallbackResponse.put("errorCode", "SERVICE_UNAVAILABLE");
        fallbackResponse.put("timestamp", LocalDateTime.now());
        fallbackResponse.put("supportEmail", "support@shopmall.com");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }
}