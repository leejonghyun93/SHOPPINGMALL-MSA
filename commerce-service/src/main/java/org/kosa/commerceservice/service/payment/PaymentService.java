package org.kosa.commerceservice.service.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.request.PrepareData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Prepare;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.commerceservice.dto.payment.*;
import org.kosa.commerceservice.entity.order.Order;
import org.kosa.commerceservice.entity.payment.Payment;
import org.kosa.commerceservice.repository.order.OrderRepository;
import org.kosa.commerceservice.repository.payment.PaymentRepository;
import org.kosa.commerceservice.service.order.OrderService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final IamportClient iamportClient;
    private final ApplicationContext applicationContext;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "cancelPaymentByImpUidFallback")
    @Retry(name = "paymentService")
    @Transactional
    public PaymentCancelResponseDTO cancelPaymentByImpUid(String impUid, String userId, String reason) {
        try {
            log.info("impUid로 결제 취소 - impUid: {}, userId: {}", impUid, userId);

            Payment payment = paymentRepository.findByInvoicePoId(impUid)
                    .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));

            if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
                throw new RuntimeException("완료된 결제만 취소할 수 있습니다.");
            }

            PaymentCancelRequestDTO cancelRequest = PaymentCancelRequestDTO.builder()
                    .paymentId(payment.getPaymentId())
                    .orderId(payment.getOrderId())
                    .userId(userId)
                    .cancelReason(reason)
                    .build();

            return cancelPayment(cancelRequest);

        } catch (Exception e) {
            log.error("impUid 결제 취소 실패", e);
            throw new RuntimeException("결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "verifyPaymentFallback")
    @Retry(name = "paymentService")
    @TimeLimiter(name = "paymentService")
    @Transactional
    public CompletableFuture<PaymentVerifyResponse> verifyPaymentAsync(PaymentVerifyRequest request, String userId) {
        return CompletableFuture.supplyAsync(() -> verifyPayment(request, userId));
    }

    @Transactional
    public PaymentVerifyResponse verifyPayment(PaymentVerifyRequest request, String userId) {
        try {
            log.info("결제 검증 시작 - userId: {}, impUid: {}, merchantUid: {}",
                    userId, request.getImpUid(), request.getMerchantUid());

            Optional<Payment> existingPayment = paymentRepository.findByInvoicePoId(request.getImpUid());
            if (existingPayment.isPresent()) {
                log.warn("이미 처리된 결제 - impUid: {}", request.getImpUid());
                throw new RuntimeException("이미 처리된 결제입니다.");
            }

            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                    iamportClient.paymentByImpUid(request.getImpUid());

            if (iamportResponse.getCode() != 0) {
                log.error("아임포트 결제 정보 조회 실패 - code: {}, message: {}",
                        iamportResponse.getCode(), iamportResponse.getMessage());
                throw new RuntimeException("결제 정보 조회 실패: " + iamportResponse.getMessage());
            }

            com.siot.IamportRestClient.response.Payment iamportPayment = iamportResponse.getResponse();

            if (!"paid".equals(iamportPayment.getStatus())) {
                log.error("결제 미완료 - status: {}, impUid: {}", iamportPayment.getStatus(), request.getImpUid());
                throw new RuntimeException("결제가 완료되지 않았습니다. 상태: " + iamportPayment.getStatus());
            }

            Order orderInfo = getOrderByMerchantUid(request.getMerchantUid());
            if (orderInfo == null) {
                throw new RuntimeException("주문 정보를 찾을 수 없습니다.");
            }

            BigDecimal orderAmount = new BigDecimal(orderInfo.getTotalPrice().toString());
            BigDecimal paidAmount = iamportPayment.getAmount();

            if (orderAmount.compareTo(paidAmount) != 0) {
                log.error("금액 불일치 - 주문금액: {}, 결제금액: {}, impUid: {}",
                        orderAmount, paidAmount, request.getImpUid());

                cancelPaymentAuto(request.getImpUid(), "금액 불일치로 인한 자동 취소");
                throw new RuntimeException("결제 금액이 주문 금액과 일치하지 않습니다.");
            }

            Payment payment = createPayment(orderInfo, iamportPayment, request.getImpUid());
            paymentRepository.save(payment);

            try {
                OrderService orderService = applicationContext.getBean(OrderService.class);
                orderService.updateOrderStatus(orderInfo.getOrderId(), "PAYMENT_COMPLETED");
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

        } catch (IamportResponseException | IOException e) {
            log.error("아임포트 API 호출 실패", e);
            throw new RuntimeException("결제 검증 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("결제 검증 중 오류 발생", e);
            throw new RuntimeException("결제 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "preparePaymentFallback")
    @Retry(name = "paymentService")
    @Transactional
    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request) {
        try {
            log.info("결제 준비 - orderId: {}, amount: {}", request.getOrderId(), request.getAmount());

            Payment payment = Payment.builder()
                    .paymentId(generatePaymentId())
                    .orderId(request.getOrderId())
                    .paymentAmount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();

            paymentRepository.save(payment);

            try {
                PrepareData prepareData = new PrepareData(
                        payment.getPaymentId(),
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
            throw new RuntimeException("결제 준비 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "cancelPaymentFallback")
    @Retry(name = "paymentService")
    @Transactional
    public PaymentCancelResponseDTO cancelPayment(PaymentCancelRequestDTO request) {
        try {
            log.info("결제 취소 요청 - paymentId: {}", request.getPaymentId());

            Payment payment = paymentRepository.findByPaymentId(request.getPaymentId())
                    .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));

            if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
                throw new RuntimeException("완료된 결제만 취소할 수 있습니다.");
            }

            CancelData cancelData;

            if (request.getRefundAmount() != null) {
                BigDecimal cancelAmount = new BigDecimal(request.getRefundAmount().toString());
                cancelData = new CancelData(payment.getInvoicePoId(), true, cancelAmount);
            } else {
                cancelData = new CancelData(payment.getInvoicePoId(), true);
            }

            if (request.getCancelReason() != null) {
                cancelData.setReason(request.getCancelReason());
            }

            IamportResponse<com.siot.IamportRestClient.response.Payment> cancelResponse =
                    iamportClient.cancelPaymentByImpUid(cancelData);

            if (cancelResponse.getCode() != 0) {
                log.error("PG사 결제 취소 실패: {}", cancelResponse.getMessage());
                return PaymentCancelResponseDTO.builder()
                        .success(false)
                        .message("PG사 결제 취소 실패: " + cancelResponse.getMessage())
                        .errorCode("PG_CANCEL_FAILED")
                        .build();
            }

            payment.updatePaymentCancelled();
            if (request.getRefundAmount() != null) {
                payment.setPaymentSecondAmount(request.getRefundAmount());
            }
            paymentRepository.save(payment);

            log.info("결제 취소 완료 - paymentId: {}", request.getPaymentId());

            return PaymentCancelResponseDTO.builder()
                    .success(true)
                    .cancelId(cancelResponse.getResponse().getImpUid())
                    .message("결제가 성공적으로 취소되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("결제 취소 중 오류 발생", e);
            throw new RuntimeException("결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public PaymentStatusResponse getPaymentStatus(String paymentId, String userId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));

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

    @Transactional
    public void handleWebhook(String impUid, String merchantUid) {
        try {
            log.info("웹훅 처리 시작 - impUid: {}, merchantUid: {}", impUid, merchantUid);

            Optional<Payment> existingPayment = paymentRepository.findByInvoicePoId(impUid);
            if (existingPayment.isPresent()) {
                log.info("이미 처리된 웹훅 - impUid: {}", impUid);
                return;
            }

            Order orderInfo = getOrderByMerchantUid(merchantUid);
            if (orderInfo == null) {
                log.warn("웹훅: 주문 정보 없음 - merchantUid: {}", merchantUid);
                return;
            }

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

    // Fallback Methods
    public PaymentCancelResponseDTO cancelPaymentByImpUidFallback(String impUid, String userId, String reason, Exception ex) {
        log.error("impUid 결제 취소 서킷브레이커 동작 - impUid: {}, error: {}", impUid, ex.getMessage());

        return PaymentCancelResponseDTO.builder()
                .success(false)
                .message("결제 취소 시스템에 일시적인 문제가 발생했습니다. 고객센터로 문의해주세요.")
                .errorCode("PAYMENT_CANCEL_CIRCUIT_OPEN")
                .build();
    }

    public CompletableFuture<PaymentVerifyResponse> verifyPaymentFallback(PaymentVerifyRequest request, String userId, Exception ex) {
        log.error("결제 검증 서킷브레이커 동작 - impUid: {}, error: {}", request.getImpUid(), ex.getMessage());

        PaymentVerifyResponse fallbackResponse = PaymentVerifyResponse.builder()
                .success(false)
                .message("결제 처리 시스템에 일시적인 문제가 발생했습니다. 고객센터로 문의해주세요.")
                .build();

        return CompletableFuture.completedFuture(fallbackResponse);
    }

    public PaymentPrepareResponse preparePaymentFallback(PaymentPrepareRequest request, Exception ex) {
        log.error("결제 준비 서킷브레이커 동작 - orderId: {}, error: {}", request.getOrderId(), ex.getMessage());

        return PaymentPrepareResponse.builder()
                .paymentId("TEMP_" + System.currentTimeMillis())
                .merchantUid("TEMP_" + System.currentTimeMillis())
                .amount(request.getAmount())
                .buyerName(request.getBuyerName())
                .buyerEmail(request.getBuyerEmail())
                .buyerTel(request.getBuyerTel())
                .name("결제 준비 중 오류 발생")
                .build();
    }

    public PaymentCancelResponseDTO cancelPaymentFallback(PaymentCancelRequestDTO request, Exception ex) {
        log.error("결제 취소 서킷브레이커 동작 - paymentId: {}, error: {}", request.getPaymentId(), ex.getMessage());

        return PaymentCancelResponseDTO.builder()
                .success(false)
                .message("결제 취소 요청이 접수되었습니다. 고객센터에서 처리해드리겠습니다.")
                .errorCode("PAYMENT_CANCEL_DELAYED")
                .build();
    }

    // Private Helper Methods
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

    private Order getOrderByMerchantUid(String merchantUid) {
        return orderRepository.findById(merchantUid).orElse(null);
    }

    private Payment createPayment(Order orderInfo, com.siot.IamportRestClient.response.Payment iamportPayment, String impUid) {
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
}