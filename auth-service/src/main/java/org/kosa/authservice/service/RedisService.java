package org.kosa.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 값 저장 (만료시간 포함)
     */
    public void setValueWithExpiry(String key, String value, long seconds) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
            log.debug("Redis에 값 저장 완료: key={}, expiry={}초", key, seconds);
        } catch (Exception e) {
            log.error("Redis 값 저장 실패: key={}, error={}", key, e.getMessage());
            throw new RuntimeException("Redis 저장 실패", e);
        }
    }

    /**
     * 값 조회
     */
    public String getValue(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            log.debug("Redis에서 값 조회: key={}, value존재={}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("Redis 값 조회 실패: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 키 삭제
     */
    public boolean deleteKey(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Redis 키 삭제: key={}, result={}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis 키 삭제 실패: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 키 존재 여부 확인
     */
    public boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis 키 존재 확인 실패: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 키의 남은 만료시간 조회 (초 단위)
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("Redis 만료시간 조회 실패: key={}, error={}", key, e.getMessage());
            return -1;
        }
    }
}