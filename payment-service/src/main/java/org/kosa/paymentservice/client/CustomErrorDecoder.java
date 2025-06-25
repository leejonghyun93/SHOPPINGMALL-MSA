package org.kosa.paymentservice.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.kosa.paymentservice.exception.PaymentException;

@Slf4j
public class CustomErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign Client Error: method={}, status={}, reason={}",
                methodKey, response.status(), response.reason());

        switch (response.status()) {
            case 400:
                return new PaymentException("잘못된 요청입니다: " + response.reason());
            case 401:
                return new PaymentException("인증이 필요합니다: " + response.reason());
            case 403:
                return new PaymentException("접근이 거부되었습니다: " + response.reason());
            case 404:
                return new PaymentException("요청한 리소스를 찾을 수 없습니다: " + response.reason());
            case 500:
                return new PaymentException("서버 내부 오류가 발생했습니다: " + response.reason());
            default:
                return new PaymentException("알 수 없는 오류가 발생했습니다: " + response.status() + " " + response.reason());
        }
    }
}