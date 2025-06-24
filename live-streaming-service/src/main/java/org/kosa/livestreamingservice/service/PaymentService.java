package org.kosa.livestreamingservice.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.request.PrepareData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Prepare;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.*;
import org.kosa.paymentservice.client.OrderServiceClient;
import org.kosa.paymentservice.entity.Payment;
import org.kosa.paymentservice.exception.PaymentException;
import org.kosa.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IamportClient iamportClient;
    private final OrderServiceClient orderServiceClient;

    /**
     * 🔥 핵심: 결제 검증
     */
    @Transactional
    public PaymentVerifyResponse verifyPayment(PaymentVerifyRequest request, String userId) {
        try {
            log.info("결제 검증 시작 - userId: {}, impUid: {}, merchantUid: {}",
                    userId, request.getImpUid(), request.getMerchantUid());

            // 1. 중복 검증 체크
            Optional<Payment> existingPayment = paymentRepository.findByInvoicePoId(request.getImpUid());
            if (existingPayment.isPresent()) {
                log.warn("이미 처리된 결제 - impUid: {}", request.getImpUid());
                throw new PaymentException("이미 처리된 결제입니다.");
            }

            // 2. 아임포트에서 결제 정보 조회
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                    iamportClient.paymentByImpUid(request.getImpUid());

            // 3. 응답 확인
            if (iamportResponse.getCode() != 0) {
                log.error("아임포트 결제 정보 조회 실패 - code: {}, message: {}",
                        iamportResponse.getCode(), iamportResponse.getMessage());
                throw new PaymentException("결제 정보 조회 실패: " + iamportResponse.getMessage());
            }

            com.siot.IamportRestClient.response.Payment iamportPayment = iamportResponse.getResponse();

            // 4. 결제 상태 검증
            if (!"paid".equals(iamportPayment.getStatus())) {
                log.error("결제 미완료 - status: {}, impUid: {}", iamportPayment.getStatus(), request.getImpUid());
                throw new PaymentException("결제가 완료되지 않았습니다. 상태: " + iamportPayment.getStatus());
            }

            // 5. 주문 정보 조회
            OrderDto orderInfo = orderServiceClient.getOrderByMerchantUid(request.getMerchantUid());
            if (orderInfo == null) {
                throw new PaymentException("주문 정보를 찾을 수 없습니다.");
            }

            // 6. 금액 검증
            BigDecimal orderAmount = orderInfo.getTotalAmount();
            BigDecimal paidAmount = iamportPayment.getAmount();

            if (orderAmount.compareTo(paidAmount) != 0) {
                log.error("금액 불일치 - 주문금액: {}, 결제금액: {}, impUid: {}",
                        orderAmount, paidAmount, request.getImpUid());

                // 자동 취소 처리
                cancelPaymentAuto(request.getImpUid(), "금액 불일치로 인한 자동 취소");
                throw new PaymentException("결제 금액이 주문 금액과 일치하지 않습니다.");
            }

            // 7. 결제 정보 저장
            Payment payment = createPayment(orderInfo, iamportPayment, request.getImpUid());
            paymentRepository.save(payment);

            // 8. 주문 상태 업데이트
            try {
                orderServiceClient.updateOrderPaymentCompleted(orderInfo.getOrderId(), payment.getPaymentId());
            } catch (Exception e) {
                log.warn("주문 상태 업데이트 실패 (결제는 완료됨): {}", e.getMessage());
            }

            log.info("결제 검증 완료 - paymentId: {}, orderId: {}",
                    payment.getPaymentId(), payment.getOrderId());

            return PaymentVerifyResponse.builder()
                    .success(true)
                    .paymentId(payment.getPaymentId())
                    .orderId(payment.getOrderId())
                    .amount(payment.getPaymentAmount())
                    .status(payment.getPaymentStatus())
                    .message("결제가 성공적으로 완료되었습니다.")
                    .build();

        } catch (PaymentException e) {
            throw e;
        } catch (IamportResponseException | IOException e) {
            log.error("아임포트 API 호출 실패", e);
            throw new PaymentException("결제 검증 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("결제 검증 중 오류 발생", e);
            throw new PaymentException("결제 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 준비
     */
    @Transactional
    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request) {
        try {
            log.info("결제 준비 - orderId: {}, amount: {}", request.getOrderId(), request.getAmount());

            // 1. 결제 정보 생성 (PENDING 상태)
            Payment payment = Payment.builder()
                    .paymentId(generatePaymentId())
                    .orderId(request.getOrderId())
                    .paymentAmount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();

            paymentRepository.save(payment);

            // 2. 아임포트 사전 등록 (선택사항)
            try {
                PrepareData prepareData = new PrepareData(
                        payment.getPaymentId(),  // merchant_uid
                        new BigDecimal(request.getAmount().toString())
                );

                IamportResponse<Prepare> prepareResponse = iamportClient.postPrepare(prepareData);

                if (prepareResponse.getCode() != 0) {
                    log.warn("아임포트 사전 등록 실패 - code: {}, message: {}",
                            prepareResponse.getCode(), prepareResponse.getMessage());
                }
            } catch (Exception e) {
                log.warn("아임포트 사전 등록 중 오류 (무시하고 진행): {}", e.getMessage());
            }

            // 3. 응답 생성
            return PaymentPrepareResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .merchantUid(payment.getPaymentId())
                    .amount(request.getAmount())
                    .buyerName(request.getBuyerName())
                    .buyerEmail(request.getBuyerEmail())
                    .buyerTel(request.getBuyerTel())
                    .name(request.getProductName())
                    .build();

        } catch (Exception e) {
            log.error("결제 준비 실패", e);
            throw new PaymentException("결제 준비 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 취소 (수정된 버전)
     */
    @Transactional
    public PaymentCancelResponse cancelPayment(String paymentId, PaymentCancelRequest request, String userId) {
        try {
            log.info("결제 취소 요청 - paymentId: {}, userId: {}", paymentId, userId);

            Payment payment = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new PaymentException("결제 정보를 찾을 수 없습니다."));

            // 결제 상태 검증
            if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
                throw new PaymentException("완료된 결제만 취소할 수 있습니다.");
            }

            // 🔥 CancelData 생성 방법 수정
            CancelData cancelData;

            if (request.getCancelAmount() != null) {
                // 부분 취소인 경우 - 금액을 포함한 생성자 사용
                BigDecimal cancelAmount = new BigDecimal(request.getCancelAmount().toString());
                cancelData = new CancelData(payment.getInvoicePoId(), true, cancelAmount);
            } else {
                // 전체 취소인 경우 - 기본 생성자 사용
                cancelData = new CancelData(payment.getInvoicePoId(), true);
            }

            // 취소 사유 설정
            if (request.getReason() != null) {
                cancelData.setReason(request.getReason());
            }

            // 아임포트 결제 취소 실행
            IamportResponse<com.siot.IamportRestClient.response.Payment> cancelResponse =
                    iamportClient.cancelPaymentByImpUid(cancelData);

            if (cancelResponse.getCode() != 0) {
                throw new PaymentException("PG사 결제 취소 실패: " + cancelResponse.getMessage());
            }

            // 상태 업데이트
            payment.updatePaymentCancelled();
            if (request.getCancelAmount() != null) {
                payment.setPaymentSecondAmount(request.getCancelAmount());
            }
            paymentRepository.save(payment);

            // 주문 상태 업데이트
            try {
                orderServiceClient.updateOrderCancelled(payment.getOrderId(), request.getReason());
            } catch (Exception e) {
                log.warn("주문 취소 알림 실패: {}", e.getMessage());
            }

            log.info("결제 취소 완료 - paymentId: {}", paymentId);

            return PaymentCancelResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .orderId(payment.getOrderId())
                    .status(payment.getPaymentStatus())
                    .cancelAmount(request.getCancelAmount() != null ?
                            request.getCancelAmount() : payment.getPaymentAmount())
                    .cancelReason(request.getReason())
                    .cancelledAt(LocalDateTime.now())
                    .build();

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("결제 취소 중 오류 발생", e);
            throw new PaymentException("결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 상태 조회
     */
    public PaymentStatusResponse getPaymentStatus(String paymentId, String userId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException("결제 정보를 찾을 수 없습니다."));

        return PaymentStatusResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .status(payment.getPaymentStatus())
                .amount(payment.getPaymentAmount())
                .paymentMethod(payment.getPaymentMethod())
                .cardName(payment.getCardName())
                .bankName(payment.getBankName())
                .approvalNumber(payment.getPaymentApprovalNumber())
                .installmentNumber(payment.getPaymentInstallmentNumber())
                .createdDate(payment.getCreatedDate())
                .updatedDate(payment.getUpdatedDate())
                .build();
    }

    /**
     * 웹훅 처리
     */
    @Transactional
    public void handleWebhook(String impUid, String merchantUid) {
        try {
            log.info("웹훅 처리 시작 - impUid: {}, merchantUid: {}", impUid, merchantUid);

            // 이미 처리된 결제인지 확인
            Optional<Payment> existingPayment = paymentRepository.findByInvoicePoId(impUid);
            if (existingPayment.isPresent()) {
                log.info("이미 처리된 웹훅 - impUid: {}", impUid);
                return;
            }

            // 주문 정보 조회
            OrderDto orderInfo = orderServiceClient.getOrderByMerchantUid(merchantUid);
            if (orderInfo == null) {
                log.warn("웹훅: 주문 정보 없음 - merchantUid: {}", merchantUid);
                return;
            }

            // 결제 검증 수행
            PaymentVerifyRequest verifyRequest = PaymentVerifyRequest.builder()
                    .impUid(impUid)
                    .merchantUid(merchantUid)
                    .build();

            verifyPayment(verifyRequest, orderInfo.getUserId());
            log.info("웹훅으로 결제 처리 완료 - impUid: {}", impUid);

        } catch (Exception e) {
            log.error("웹훅 처리 실패 - impUid: {}, error: {}", impUid, e.getMessage());
        }
    }

    // === 내부 메서드들 ===

    /**
     * 자동 취소 처리 (수정된 버전)
     */
    private void cancelPaymentAuto(String impUid, String reason) {
        try {
            CancelData cancelData = new CancelData(impUid, true);
            cancelData.setReason(reason);
            iamportClient.cancelPaymentByImpUid(cancelData);
            log.info("자동 취소 완료 - impUid: {}", impUid);
        } catch (Exception e) {
            log.error("자동 취소 실패 - impUid: {}", impUid, e);
        }
    }

    private Payment createPayment(OrderDto orderInfo, com.siot.IamportRestClient.response.Payment iamportPayment, String impUid) {
        return Payment.builder()
                .paymentId(generatePaymentId())
                .orderId(orderInfo.getOrderId())
                .invoicePoId(impUid)
                .paymentAmount(iamportPayment.getAmount().intValue())
                .paymentStatus(PaymentStatus.COMPLETED)
                .paymentMethod(iamportPayment.getPayMethod())
                .cardName(iamportPayment.getCardName())
                .bankName(iamportPayment.getBankName())
                .paymentApprovalNumber(iamportPayment.getApplyNum())
                .paymentInstallmentNumber(iamportPayment.getCardQuota())
                .build();
    }

    private String generatePaymentId() {
        return "PAY_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public PaymentPrepareResponse prepareTestPayment(PaymentPrepareRequest request) {
        // 테스트 모드 체크
        if (isTestMode(request)) {
            return prepareTestPaymentInternal(request);
        }

        // 기존 로직 그대로 사용
        return preparePayment(request);
    }

    /**
     * 🆓 NEW: PG사별 테스트 결제 (기존 코드에 추가)
     */
    public Map<String, Object> generateTestPaymentConfig(String pgProvider, PaymentPrepareRequest request) {
        Map<String, Object> config = new HashMap<>();

        // 기본 설정 (기존 로직 활용)
        config.put("merchant_uid", generatePaymentId());
        config.put("amount", request.getAmount());
        config.put("name", request.getProductName());
        config.put("buyer_name", request.getBuyerName());
        config.put("buyer_email", request.getBuyerEmail());

        // PG사별 추가 설정만 새로 추가
        switch (pgProvider) {
            case "html5_inicis":
                config.put("pg", "html5_inicis");
                config.put("pay_method", "card");
                break;
            case "kcp":
                config.put("pg", "kcp");
                config.put("pay_method", "card");
                break;
            case "toss":
                config.put("pg", "toss");
                config.put("pay_method", "card");
                break;
            default:
                // 기존 카카오페이 설정 유지
                config.put("pg", "kakaopay");
                config.put("pay_method", "kakaopay");
        }

        return config;
    }

    private boolean isTestMode(PaymentPrepareRequest request) {
        // 테스트 모드 판별 로직
        return request.getOrderId() != null && request.getOrderId().startsWith("TEST_");
    }

    private PaymentPrepareResponse prepareTestPaymentInternal(PaymentPrepareRequest request) {
        // 테스트용 간단 구현
        return PaymentPrepareResponse.builder()
                .paymentId("TEST_" + System.currentTimeMillis())
                .merchantUid("TEST_" + System.currentTimeMillis())
                .amount(request.getAmount())
                .buyerName(request.getBuyerName())
                .buyerEmail(request.getBuyerEmail())
                .buyerTel(request.getBuyerTel())
                .name("🆓 테스트: " + request.getProductName())
                .build();
    }
}