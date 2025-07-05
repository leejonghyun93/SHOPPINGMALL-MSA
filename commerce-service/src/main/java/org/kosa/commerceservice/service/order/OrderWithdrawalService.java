package org.kosa.commerceservice.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.order.OrderDTO;
import org.kosa.commerceservice.dto.order.UserWithdrawalEvent;
import org.kosa.commerceservice.service.notification.NotificationService;
import org.kosa.commerceservice.service.payment.PaymentRefundService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderWithdrawalService {

    private final OrderService orderService;
    private final PaymentRefundService paymentRefundService;
    private final NotificationService notificationService;

    public void processUserWithdrawalOrders(UserWithdrawalEvent event) {
        String userId = event.getUserId();

        try {
            log.info("📦 사용자 탈퇴 주문 처리 시작: userId={}", userId);

            List<OrderDTO> activeOrders = orderService.getActiveUserOrders(userId);
            log.info("📊 활성 주문 수: {}", activeOrders.size());

            if (activeOrders.isEmpty()) {
                log.info("📭 처리할 활성 주문이 없습니다: userId={}", userId);
                return;
            }

            Map<String, List<OrderDTO>> ordersByStatus = activeOrders.stream()
                    .collect(Collectors.groupingBy(OrderDTO::getOrderStatus));

            processPendingOrders(ordersByStatus.get("PENDING"), userId);
            processPreparingOrders(ordersByStatus.get("PREPARING"), userId);
            processPaymentCompletedOrders(ordersByStatus.get("PAYMENT_COMPLETED"), userId);
            processShippingOrders(ordersByStatus.get("SHIPPING"), userId);
            processDeliveredOrders(ordersByStatus.get("DELIVERED"), userId);

            processPersonalDataMasking(userId, event);
            notifyCustomerServiceIfNeeded(activeOrders, event);

            log.info("사용자 탈퇴 주문 처리 완료: userId={}, 총 처리 주문 수={}",
                    userId, activeOrders.size());

        } catch (Exception e) {
            log.error("사용자 탈퇴 주문 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("탈퇴 주문 처리 중 오류: " + e.getMessage(), e);
        }
    }

    private void processPendingOrders(List<OrderDTO> orders, String userId) {
        if (orders == null || orders.isEmpty()) return;

        log.info("결제 대기 주문 처리: {}건", orders.size());

        for (OrderDTO order : orders) {
            try {
                orderService.updateOrderStatus(order.getOrderId(), "CANCELLED_BY_WITHDRAWAL");
                log.info("결제 대기 주문 취소: orderId={}", order.getOrderId());
            } catch (Exception e) {
                log.error("결제 대기 주문 취소 실패: orderId={}, error={}",
                        order.getOrderId(), e.getMessage());
            }
        }
    }

    private void processPreparingOrders(List<OrderDTO> orders, String userId) {
        if (orders == null || orders.isEmpty()) return;

        log.info("준비 중 주문 처리: {}건", orders.size());

        for (OrderDTO order : orders) {
            try {
                orderService.updateOrderStatus(order.getOrderId(), "CANCELLED_BY_WITHDRAWAL");

                if (order.getTotalPrice() > 0) {
                    paymentRefundService.processRefund(
                            order.getOrderId(),
                            order.getTotalPrice(),
                            "회원탈퇴로 인한 자동 환불"
                    );
                }

                log.info("준비 중 주문 취소 및 환불: orderId={}, amount={}",
                        order.getOrderId(), order.getTotalPrice());

            } catch (Exception e) {
                log.error("준비 중 주문 처리 실패: orderId={}, error={}",
                        order.getOrderId(), e.getMessage());
            }
        }
    }

    private void processPaymentCompletedOrders(List<OrderDTO> orders, String userId) {
        if (orders == null || orders.isEmpty()) return;

        log.info("💰 결제 완료 주문 처리: {}건", orders.size());

        for (OrderDTO order : orders) {
            try {
                orderService.updateOrderStatus(order.getOrderId(), "CANCELLED_BY_WITHDRAWAL");

                paymentRefundService.processRefund(
                        order.getOrderId(),
                        order.getTotalPrice(),
                        "회원탈퇴로 인한 자동 환불"
                );

                log.info("✅ 결제 완료 주문 취소 및 환불: orderId={}, amount={}",
                        order.getOrderId(), order.getTotalPrice());

            } catch (Exception e) {
                log.error("❌ 결제 완료 주문 처리 실패: orderId={}, error={}",
                        order.getOrderId(), e.getMessage());
            }
        }
    }

    private void processShippingOrders(List<OrderDTO> orders, String userId) {
        if (orders == null || orders.isEmpty()) return;

        log.info("🚚 배송 중 주문 처리: {}건 (고객센터 알림)", orders.size());

        for (OrderDTO order : orders) {
            try {
                orderService.updateOrderStatus(order.getOrderId(), "SHIPPING_MEMBER_WITHDRAWN");

                notificationService.notifyCustomerService(
                        "탈퇴 회원 배송 중 주문",
                        String.format("주문번호: %s, 회원ID: %s, 탈퇴일: %s\n배송 완료 후 환불 처리 필요",
                                order.getOrderId(), userId, LocalDateTime.now())
                );

                log.info("📞 배송 중 주문 고객센터 알림: orderId={}", order.getOrderId());

            } catch (Exception e) {
                log.error("❌ 배송 중 주문 처리 실패: orderId={}, error={}",
                        order.getOrderId(), e.getMessage());
            }
        }
    }

    private void processDeliveredOrders(List<OrderDTO> orders, String userId) {
        if (orders == null || orders.isEmpty()) return;

        log.info("📦 배송 완료 주문 처리: {}건 (개인정보 마스킹)", orders.size());

        for (OrderDTO order : orders) {
            try {
                orderService.updateOrderStatus(order.getOrderId(), "DELIVERED_MEMBER_WITHDRAWN");

                log.info("🔒 배송 완료 주문 개인정보 마스킹: orderId={}", order.getOrderId());

            } catch (Exception e) {
                log.error("❌ 배송 완료 주문 처리 실패: orderId={}, error={}",
                        order.getOrderId(), e.getMessage());
            }
        }
    }

    private void processPersonalDataMasking(String userId, UserWithdrawalEvent event) {
        try {
            log.info("🔒 개인정보 마스킹 처리 시작: userId={}", userId);

            log.info("🔒 개인정보 마스킹 처리 완료: userId={}", userId);

        } catch (Exception e) {
            log.error("❌ 개인정보 마스킹 처리 실패: userId={}, error={}", userId, e.getMessage());
            notificationService.notifyDataProtectionOfficer(
                    "개인정보 마스킹 실패",
                    String.format("사용자 %s의 탈퇴 처리 중 개인정보 마스킹 실패: %s", userId, e.getMessage())
            );
        }
    }

    private void notifyCustomerServiceIfNeeded(List<OrderDTO> activeOrders, UserWithdrawalEvent event) {
        try {
            int totalAmount = activeOrders.stream()
                    .mapToInt(OrderDTO::getTotalPrice)
                    .sum();

            if (totalAmount > 500000) {
                notificationService.notifyCustomerService(
                        "고액 주문 탈퇴 회원",
                        String.format("회원ID: %s, 총 주문금액: %,d원, 주문 수: %d건\n수동 확인 필요",
                                event.getUserId(), totalAmount, activeOrders.size())
                );
            }

            long shippingOrDeliveredCount = activeOrders.stream()
                    .filter(o -> "SHIPPING".equals(o.getOrderStatus()) || "DELIVERED".equals(o.getOrderStatus()))
                    .count();

            if (shippingOrDeliveredCount > 0) {
                notificationService.notifyCustomerService(
                        "배송 관련 주문 보유 탈퇴 회원",
                        String.format("회원ID: %s, 배송 관련 주문: %d건\n배송 완료 후 처리 방법 검토 필요",
                                event.getUserId(), shippingOrDeliveredCount)
                );
            }

        } catch (Exception e) {
            log.error("❌ 고객센터 알림 처리 실패: error={}", e.getMessage());
        }
    }
}