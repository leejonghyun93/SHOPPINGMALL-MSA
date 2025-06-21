package org.kosa.productservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 🔥 Redis 연결 테스트 컨트롤러
 */
@RestController
@RequestMapping("/api/redis-test")
@Slf4j
public class RedisTestController {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/ping")
    public Map<String, Object> testRedis() {
        Map<String, Object> result = new HashMap<>();

        if (redisTemplate == null) {
            result.put("success", false);
            result.put("error", "RedisTemplate Bean이 생성되지 않았습니다!");
            result.put("message", "Redis 의존성이나 설정을 확인하세요");
            return result;
        }

        try {
            log.info("🔥 Redis 연결 테스트 시작");

            // Redis에 테스트 데이터 저장
            redisTemplate.opsForValue().set("test:key", "Hello Redis!");
            log.info("✅ Redis에 데이터 저장 성공");

            // Redis에서 데이터 조회
            String value = (String) redisTemplate.opsForValue().get("test:key");
            log.info("✅ Redis에서 데이터 조회 성공: {}", value);

            result.put("success", true);
            result.put("message", "Redis 연결 성공!");
            result.put("testValue", value);
            result.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("❌ Redis 연결 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }

        return result;
    }

    @GetMapping("/info")
    public Map<String, Object> getRedisInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("redisTemplateAvailable", redisTemplate != null);

        if (redisTemplate != null) {
            try {
                // Redis 정보 확인
                info.put("connectionFactoryType", redisTemplate.getConnectionFactory().getClass().getSimpleName());
                info.put("message", "RedisTemplate이 정상적으로 주입되었습니다");
            } catch (Exception e) {
                info.put("error", e.getMessage());
            }
        } else {
            info.put("message", "RedisTemplate Bean을 찾을 수 없습니다");
            info.put("suggestion", "Redis 의존성과 설정을 확인하세요");
        }

        return info;
    }
}