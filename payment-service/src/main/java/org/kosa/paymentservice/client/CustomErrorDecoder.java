package org.kosa.paymentservice.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign Client Error: method={}, status={}", methodKey, response.status());

        switch (response.status()) {
            case 400:
                return new IllegalArgumentException("잘못된 요청입니다.");
            case 404:
                return new RuntimeException("요청한 리소스를 찾을 수 없습니다.");
            case 500:
                return new RuntimeException("서버 내부 오류가 발생했습니다.");
            default:
                return new RuntimeException("알 수 없는 오류가 발생했습니다.");
        }
    }
}