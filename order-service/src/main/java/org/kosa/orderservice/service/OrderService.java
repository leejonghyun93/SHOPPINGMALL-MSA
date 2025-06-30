package org.kosa.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.client.PaymentCancelClient;
import org.kosa.orderservice.dto.*;
import org.kosa.orderservice.entity.Order;
import org.kosa.orderservice.entity.OrderCancel;
import org.kosa.orderservice.entity.OrderItem;
import org.kosa.orderservice.mapper.OrderCancelRepository;
import org.kosa.orderservice.mapper.OrderItemRepository;
import org.kosa.orderservice.mapper.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderCancelRepository orderCancelRepository;
    private final PaymentCancelClient paymentCancelClient;

    private static final List<String> CANCELLABLE_STATUSES = Arrays.asList(
            "PENDING", "ORDER_COMPLETED", "PREPARING", "PAYMENT_COMPLETED"
    );

    private static final String CANCELLED_STATUS = "CANCELLED";
    private static final String CANCELLED_BY_WITHDRAWAL_STATUS = "CANCELLED_BY_WITHDRAWAL";

    /**
     * 체크아웃 - 주문 생성
     */
    public OrderResponseDTO createOrder(CheckoutRequestDTO request) {
        try {
            log.info("주문 생성 시작: userId={}", request.getUserId());

            Integer totalItemPrice = request.getItems().stream()
                    .mapToInt(CheckoutItemDTO::getTotalPrice)
                    .sum();

            Integer deliveryFee = totalItemPrice >= 40000 ? 0 : 0;

            Integer finalTotalPrice = totalItemPrice + deliveryFee -
                    (request.getUsedPoint() != null ? request.getUsedPoint() : 0);

            Integer savedPoint = (int) (finalTotalPrice * 0.01);

            Order order = Order.builder()
                    .userId(request.getUserId())
                    .orderStatus(getDefaultValue(request.getOrderStatus(), "PENDING"))
                    .phone(getDefaultValue(request.getPhone(), ""))
                    .email(getDefaultValue(request.getEmail(), ""))
                    .recipientName(getDefaultValue(request.getRecipientName(), "수령인"))
                    .recipientPhone(getDefaultValue(request.getRecipientPhone(), "010-0000-0000"))
                    .orderZipcode(getDefaultValue(request.getOrderZipcode(), ""))
                    .orderAddressDetail(getDefaultValue(request.getOrderAddressDetail(), "주소 정보 없음"))
                    .deliveryMemo(getDefaultValue(request.getDeliveryMemo(), ""))
                    .totalPrice(finalTotalPrice)
                    .deliveryFee(deliveryFee)
                    .discountAmount(0)
                    .usedPoint(request.getUsedPoint() != null ? request.getUsedPoint() : 0)
                    .paymentMethod(getDefaultValue(request.getPaymentMethod(), "CARD"))
                    .paymentMethodName(getDefaultValue(request.getPaymentMethodName(), "신용카드"))
                    .savedPoint(savedPoint)
                    .estimatedDate(LocalDateTime.now().plusDays(1).withHour(7).withMinute(0).withSecond(0))
                    .build();

            Order savedOrder = orderRepository.save(order);

            for (CheckoutItemDTO item : request.getItems()) {
                OrderItem orderItem = OrderItem.builder()
                        .orderId(savedOrder.getOrderId())
                        .productId(item.getProductId())
                        .name(getDefaultValue(item.getProductName(), "상품명 없음"))
                        .quantity(item.getQuantity())
                        .status("PREPARING")
                        .totalPrice(item.getTotalPrice())
                        .deliveryFee(0)
                        .imageUrl(getDefaultValue(item.getImageUrl(), ""))
                        .build();

                orderItemRepository.save(orderItem);
            }

            return OrderResponseDTO.builder()
                    .orderId(savedOrder.getOrderId())
                    .orderStatus(savedOrder.getOrderStatus())
                    .totalAmount(savedOrder.getTotalPrice())
                    .message("주문이 성공적으로 완료되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("주문 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 주문 취소 처리 (결제 취소 포함)
     */
    @Transactional
    public OrderCancelResponseDTO cancelOrder(OrderCancelRequestDTO request) {
        try {
            log.info("주문 취소 시작: orderId={}, userId={}", request.getOrderId(), request.getUserId());

            Order order = orderRepository.findByOrderIdAndUserId(request.getOrderId(), request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

            Optional<OrderCancel> existingCancel = orderCancelRepository.findByOrderId(request.getOrderId());
            if (existingCancel.isPresent()) {
                throw new IllegalStateException("이미 취소된 주문입니다.");
            }

            if (!CANCELLABLE_STATUSES.contains(order.getOrderStatus())) {
                throw new IllegalStateException("현재 주문 상태에서는 취소할 수 없습니다. 상태: " + order.getOrderStatus());
            }

            String refundStatus = "PENDING";
            String paymentCancelId = null;

            if (request.getPaymentId() != null && !request.getPaymentId().isEmpty()) {
                try {
                    String cancelReason = getDefaultValue(request.getReason(), "사용자 요청");
                    PaymentCancelResult cancelResult = cancelPayment(
                            request.getPaymentId(),
                            request.getRefundAmount(),
                            cancelReason
                    );

                    if (cancelResult.isSuccess()) {
                        refundStatus = "COMPLETED";
                        paymentCancelId = cancelResult.getCancelId();
                        log.info("결제 취소 성공: paymentId={}, cancelId={}",
                                request.getPaymentId(), paymentCancelId);
                    } else {
                        log.error("PG 결제 취소 실패: {}", cancelResult.getMessage());
                        throw new RuntimeException("결제 취소 처리 실패: " + cancelResult.getMessage());
                    }
                } catch (Exception e) {
                    log.error("결제 취소 서비스 오류", e);
                    refundStatus = "FAILED";
                }
            }

            OrderCancel orderCancel = OrderCancel.builder()
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .reason(request.getReason())
                    .detail(request.getDetail())
                    .refundAmount(request.getRefundAmount())
                    .paymentId(request.getPaymentId())
                    .refundStatus(refundStatus)
                    .paymentCancelId(paymentCancelId)
                    .cancelDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            orderCancelRepository.save(orderCancel);
            updateOrderAndItemsStatus(order, CANCELLED_STATUS);

            log.info("주문 취소 완료: orderId={}, refundAmount={}",
                    request.getOrderId(), request.getRefundAmount());

            return OrderCancelResponseDTO.builder()
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .cancelReason(orderCancel.getReason())
                    .refundAmount(orderCancel.getRefundAmount())
                    .refundStatus(orderCancel.getRefundStatus())
                    .cancelDate(orderCancel.getCancelDate())
                    .paymentCancelId(orderCancel.getPaymentCancelId())
                    .message("주문이 성공적으로 취소되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("주문 취소 처리 중 오류", e);
            throw new RuntimeException("주문 취소 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 간단 주문 취소 (결제 취소 없이)
     */
    public void cancelOrder(String orderId, String userId) {
        try {
            log.info("간단 주문 취소: orderId={}, userId={}", orderId, userId);

            Order order = orderRepository.findByOrderIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

            if (!CANCELLABLE_STATUSES.contains(order.getOrderStatus())) {
                throw new RuntimeException("취소할 수 없는 주문 상태입니다: " + order.getOrderStatus());
            }

            updateOrderAndItemsStatus(order, CANCELLED_STATUS);
            log.info("간단 주문 취소 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("간단 주문 취소 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 취소 중 오류가 발생했습니다.");
        }
    }

    /**
     * 결제 취소 처리 (Payment Service 호출)
     */
    private PaymentCancelResult cancelPayment(String paymentId, Integer refundAmount, String cancelReason) {
        try {
            log.info("결제 취소 요청: paymentId={}", paymentId);

            PaymentCancelRequestDTO request = PaymentCancelRequestDTO.builder()
                    .paymentId(paymentId)
                    .refundAmount(refundAmount)
                    .cancelReason(cancelReason)
                    .build();

            PaymentCancelResponseDTO response = paymentCancelClient.cancelPayment(request);

            if (response.isSuccess()) {
                log.info("결제 취소 성공: cancelId={}", response.getCancelId());
                return PaymentCancelResult.builder()
                        .success(true)
                        .cancelId(response.getCancelId())
                        .message(response.getMessage())
                        .build();
            } else {
                log.error("결제 취소 실패: {}", response.getMessage());
                return PaymentCancelResult.builder()
                        .success(false)
                        .cancelId(null)
                        .message(response.getMessage())
                        .errorCode(response.getErrorCode())
                        .build();
            }

        } catch (Exception e) {
            log.error("Payment Service 호출 오류", e);
            return PaymentCancelResult.builder()
                    .success(false)
                    .cancelId(null)
                    .message("Payment Service 연동 오류: " + e.getMessage())
                    .errorCode("SERVICE_ERROR")
                    .build();
        }
    }

    /**
     * 주문 및 아이템 상태 일괄 업데이트
     */
    private void updateOrderAndItemsStatus(Order order, String newStatus) {
        order.setOrderStatus(newStatus);
        order.setUpdatedDate(LocalDateTime.now());
        orderRepository.save(order);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
        for (OrderItem item : orderItems) {
            item.setStatus(newStatus);
            item.setUpdatedDate(LocalDateTime.now());
            orderItemRepository.save(item);
        }
    }

    /**
     * 주문 상태 변경
     */
    public void updateOrderStatus(String orderId, String newStatus) {
        try {
            log.info("주문 상태 변경: orderId={}, newStatus={}", orderId, newStatus);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

            if ("SHIPPING".equals(newStatus)) {
                order.setShippingDate(LocalDateTime.now());
                order.setTrackingNumber("TRK" + System.currentTimeMillis());
                order.setDeliveryCompany("한진택배");
            }

            updateOrderAndItemsStatus(order, newStatus);
            log.info("주문 상태 변경 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("주문 상태 변경 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 상태 변경 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자 탈퇴 시 주문 데이터 처리
     */
    @Transactional
    public void processUserWithdrawalOrders(String userId) {
        try {
            log.info("사용자 탈퇴 주문 처리 시작: userId={}", userId);

            List<OrderDTO> activeOrders = getActiveUserOrders(userId);
            log.info("처리 대상 활성 주문 수: {}", activeOrders.size());

            for (OrderDTO orderDTO : activeOrders) {
                String orderId = orderDTO.getOrderId();
                String currentStatus = orderDTO.getOrderStatus();

                log.info("주문 처리: orderId={}, currentStatus={}", orderId, currentStatus);

                if (Arrays.asList("PENDING", "PREPARING", "ORDERED", "PAYMENT_COMPLETED").contains(currentStatus)) {
                    Order order = orderRepository.findById(orderId).orElse(null);
                    if (order != null) {
                        updateOrderAndItemsStatus(order, CANCELLED_BY_WITHDRAWAL_STATUS);
                        log.info("주문 자동 취소: orderId={} (탈퇴로 인한 취소)", orderId);
                    }
                } else if (Arrays.asList("SHIPPED", "DELIVERED").contains(currentStatus)) {
                    log.info("배송 중/완료 주문 유지: orderId={}, status={}", orderId, currentStatus);
                }
            }

            log.info("사용자 탈퇴 주문 처리 완료: userId={}", userId);

        } catch (Exception e) {
            log.error("사용자 탈퇴 주문 처리 실패: userId={}", userId, e);
            throw new RuntimeException("사용자 탈퇴 주문 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders(String userId) {
        try {
            List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
            return orders.stream()
                    .map(this::convertToOrderDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("주문 목록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 목록 조회 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getActiveUserOrders(String userId) {
        try {
            List<Order> orders = orderRepository.findActiveOrdersByUserId(userId);
            return orders.stream()
                    .map(this::convertToOrderDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("활성 주문 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("활성 주문 조회 중 오류가 발생했습니다.");
        }
    }

    public OrderDTO getOrderDetailByOrderId(String orderId) {
        log.info("주문 조회 시작: orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        log.info("주문 상품 조회 결과: {}개", orderItems.size());

        return convertToOrderDTOWithItems(order, orderItems);
    }

    public OrderDTO getOrderDetail(String orderId, String userId) {
        log.info("사용자별 주문 조회: orderId={}, userId={}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("주문 조회 권한이 없습니다");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return convertToOrderDTOWithItems(order, orderItems);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getUserOrdersPaged(String userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Order> orderPage = orderRepository.findByUserIdOrderByOrderDateDesc(userId, pageable);
            return orderPage.map(this::convertToOrderDTO);
        } catch (Exception e) {
            log.error("주문 목록 페이징 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 목록 조회 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public OrderCancelResponseDTO getCancelInfo(String orderId, String userId) {
        try {
            Order order = orderRepository.findByOrderIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

            OrderCancel orderCancel = orderCancelRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문 취소 정보를 찾을 수 없습니다."));

            return OrderCancelResponseDTO.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .cancelReason(orderCancel.getReason())
                    .refundAmount(orderCancel.getRefundAmount())
                    .refundStatus(orderCancel.getRefundStatus())
                    .cancelDate(orderCancel.getCancelDate())
                    .paymentCancelId(orderCancel.getPaymentCancelId())
                    .build();

        } catch (Exception e) {
            log.error("주문 취소 정보 조회 실패", e);
            throw new RuntimeException("주문 취소 정보 조회 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public boolean canCancelOrder(String orderId, String userId) {
        try {
            Order order = orderRepository.findByOrderIdAndUserId(orderId, userId).orElse(null);
            if (order == null) return false;

            if (orderCancelRepository.findByOrderId(orderId).isPresent()) return false;

            return CANCELLABLE_STATUSES.contains(order.getOrderStatus());
        } catch (Exception e) {
            log.error("취소 가능 여부 확인 중 오류", e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderCancelResponseDTO> getUserCancelledOrders(String userId) {
        try {
            List<OrderCancel> cancelledOrders = orderCancelRepository.findByUserId(userId);

            return cancelledOrders.stream()
                    .map(orderCancel -> {
                        Order order = orderRepository.findById(orderCancel.getOrderId()).orElse(null);
                        if (order != null) {
                            return OrderCancelResponseDTO.builder()
                                    .orderId(orderCancel.getOrderId())
                                    .userId(orderCancel.getUserId())
                                    .cancelReason(orderCancel.getReason())
                                    .refundAmount(orderCancel.getRefundAmount())
                                    .refundStatus(orderCancel.getRefundStatus())
                                    .cancelDate(orderCancel.getCancelDate())
                                    .paymentCancelId(orderCancel.getPaymentCancelId())
                                    .build();
                        }
                        return null;
                    })
                    .filter(response -> response != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("취소 주문 목록 조회 실패", e);
            throw new RuntimeException("취소 주문 목록 조회 중 오류가 발생했습니다.");
        }
    }

    // 🔥 추가된 메서드들 - OrderController에서 사용

    /**
     * 모든 주문 ID 목록 조회 (디버깅용)
     */
    @Transactional(readOnly = true)
    public List<String> getAllOrderIds() {
        try {
            return orderRepository.findAllOrderIds();
        } catch (Exception e) {
            log.error("모든 주문 ID 조회 실패: {}", e.getMessage(), e);
            return List.of(); // 빈 리스트 반환
        }
    }

    /**
     * 주문 존재 여부 확인 (디버깅용)
     */
    @Transactional(readOnly = true)
    public boolean orderExists(String orderId) {
        try {
            return orderRepository.existsById(orderId);
        } catch (Exception e) {
            log.error("주문 존재 여부 확인 실패: orderId={}, error={}", orderId, e.getMessage());
            return false;
        }
    }

    // 🔥 Helper Methods

    private String getDefaultValue(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    private OrderDTO convertToOrderDTO(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedDateAsc(order.getOrderId());
        return convertToOrderDTOWithItems(order, orderItems);
    }

    private OrderDTO convertToOrderDTOWithItems(Order order, List<OrderItem> orderItems) {
        List<OrderItemDTO> orderItemDTOs = orderItems.stream()
                .map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .phone(order.getPhone())
                .email(order.getEmail())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .orderZipcode(order.getOrderZipcode())
                .orderAddressDetail(order.getOrderAddressDetail())
                .deliveryMemo(order.getDeliveryMemo())
                .totalPrice(order.getTotalPrice())
                .deliveryFee(order.getDeliveryFee())
                .discountAmount(order.getDiscountAmount())
                .usedPoint(order.getUsedPoint())
                .paymentMethod(order.getPaymentMethod())
                .savedPoint(order.getSavedPoint())
                .paymentMethodName(order.getPaymentMethodName())
                .shippingDate(order.getShippingDate())
                .estimatedDate(order.getEstimatedDate())
                .trackingNumber(order.getTrackingNumber())
                .deliveryCompany(order.getDeliveryCompany())
                .createdDate(order.getCreatedDate())
                .updatedDate(order.getUpdatedDate())
                .items(orderItemDTOs)
                .build();
    }

    private OrderItemDTO convertToOrderItemDTO(OrderItem orderItem) {
        return OrderItemDTO.builder()
                .orderItemId(orderItem.getOrderItemId())
                .orderId(orderItem.getOrderId())
                .productId(orderItem.getProductId())
                .name(orderItem.getName())
                .quantity(orderItem.getQuantity())
                .status(orderItem.getStatus())
                .totalPrice(orderItem.getTotalPrice())
                .deliveryFee(orderItem.getDeliveryFee())
                .imageUrl(orderItem.getImageUrl())
                .createdDate(orderItem.getCreatedDate())
                .updatedDate(orderItem.getUpdatedDate())
                .build();
    }
}