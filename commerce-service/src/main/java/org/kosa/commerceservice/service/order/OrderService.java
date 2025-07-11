package org.kosa.commerceservice.service.order;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.commerceservice.dto.order.*;
import org.kosa.commerceservice.dto.payment.PaymentCancelRequestDTO;
import org.kosa.commerceservice.dto.payment.PaymentCancelResponseDTO;
import org.kosa.commerceservice.entity.order.Order;
import org.kosa.commerceservice.entity.order.OrderCancel;
import org.kosa.commerceservice.entity.order.OrderItem;
import org.kosa.commerceservice.repository.order.OrderCancelRepository;
import org.kosa.commerceservice.repository.order.OrderItemRepository;
import org.kosa.commerceservice.repository.order.OrderRepository;
import org.kosa.commerceservice.service.payment.PaymentService;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderCancelRepository orderCancelRepository;
    private final ApplicationContext applicationContext;

    private static final List<String> CANCELLABLE_STATUSES = Arrays.asList(
            "PENDING", "ORDER_COMPLETED", "PREPARING", "PAYMENT_COMPLETED"
    );

    private static final String CANCELLED_STATUS = "CANCELLED";
    private static final String CANCELLED_BY_WITHDRAWAL_STATUS = "CANCELLED_BY_WITHDRAWAL";

    @Transactional(readOnly = true)
    public int getOrderCount(String userId) {
        try {
            log.info("사용자 주문 개수 조회: userId={}", userId);

            List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);

            int count = orders.size();
            log.info("주문 개수 조회 결과: userId={}, count={}", userId, count);

            return count;

        } catch (Exception e) {
            log.error("주문 개수 조회 실패: userId={}, error={}", userId, e.getMessage());
            return 0;
        }
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    @Retry(name = "orderService")
    @TimeLimiter(name = "orderService")
    @Transactional
    public CompletableFuture<OrderResponseDTO> createOrderAsync(CheckoutRequestDTO request) {
        return CompletableFuture.supplyAsync(() -> createOrder(request));
    }

    @Transactional
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

    @CircuitBreaker(name = "paymentService", fallbackMethod = "cancelOrderFallback")
    @Retry(name = "paymentService")
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
                    PaymentCancelRequestDTO paymentCancelRequest = PaymentCancelRequestDTO.builder()
                            .paymentId(request.getPaymentId())
                            .refundAmount(request.getRefundAmount())
                            .cancelReason(getDefaultValue(request.getReason(), "사용자 요청"))
                            .orderId(request.getOrderId())
                            .userId(request.getUserId())
                            .build();

                    PaymentService paymentService = applicationContext.getBean(PaymentService.class);
                    PaymentCancelResponseDTO cancelResult = paymentService.cancelPayment(paymentCancelRequest);

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

    // Fallback Methods
    public CompletableFuture<OrderResponseDTO> createOrderFallback(CheckoutRequestDTO request, Exception ex) {
        log.error("주문 생성 서킷브레이커 동작 - userId: {}, error: {}", request.getUserId(), ex.getMessage());

        OrderResponseDTO fallbackResponse = OrderResponseDTO.builder()
                .orderId("TEMP_" + System.currentTimeMillis())
                .orderStatus("PENDING_RETRY")
                .totalAmount(0)
                .message("주문이 임시로 접수되었습니다. 처리 상황을 확인해주세요.")
                .build();

        return CompletableFuture.completedFuture(fallbackResponse);
    }

    public OrderCancelResponseDTO cancelOrderFallback(OrderCancelRequestDTO request, Exception ex) {
        log.error("주문 취소 서킷브레이커 동작 - orderId: {}, error: {}", request.getOrderId(), ex.getMessage());

        return OrderCancelResponseDTO.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .message("주문 취소 요청이 접수되었습니다. 고객센터에서 처리해드리겠습니다.")
                .build();
    }

    // Private Helper Methods
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