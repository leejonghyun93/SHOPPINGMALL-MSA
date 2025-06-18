// OrderService.java
package org.kosa.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.dto.*;
import org.kosa.orderservice.mapper.OrderItemRepository;
import org.kosa.orderservice.mapper.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 체크아웃 - 주문 생성
     */
    public OrderResponseDTO createOrder(CheckoutRequestDTO request) {
        try {
            log.info("주문 생성 시작: userId={}", request.getUserId());

            // 1. 주문 총액 계산
            Integer totalItemPrice = request.getItems().stream()
                    .mapToInt(CheckoutItemDTO::getTotalPrice)
                    .sum();

            // 2. 배송비 계산 (4만원 이상 무료배송)
            Integer deliveryFee = totalItemPrice >= 40000 ? 0 : 3000;

            // 3. 최종 결제 금액 계산
            Integer finalTotalPrice = totalItemPrice + deliveryFee -
                    (request.getUsedPoint() != null ? request.getUsedPoint() : 0);

            // 4. 적립 포인트 계산 (결제 금액의 1%)
            Integer savedPoint = (int) (finalTotalPrice * 0.01);

            // 5. 주문 엔티티 생성
            Order order = Order.builder()
                    .userId(request.getUserId())
                    .orderStatus("ORDER_COMPLETED")  // 주문완료
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .recipientName(request.getRecipientName())
                    .recipientPhone(request.getRecipientPhone())
                    .orderZipcode(request.getOrderZipcode())
                    .orderAddressDetail(request.getOrderAddressDetail())
                    .deliveryMemo(request.getDeliveryMemo())
                    .totalPrice(finalTotalPrice)
                    .deliveryFee(deliveryFee)
                    .discountAmount(0)
                    .usedPoint(request.getUsedPoint() != null ? request.getUsedPoint() : 0)
                    .paymentMethod(request.getPaymentMethod())
                    .paymentMethodName(request.getPaymentMethodName())
                    .savedPoint(savedPoint)
                    .estimatedDate(LocalDateTime.now().plusDays(1))  // 익일 배송
                    .build();

            // 6. 주문 저장
            Order savedOrder = orderRepository.save(order);
            log.info("주문 저장 완료: orderId={}", savedOrder.getOrderId());

            // 7. 주문 아이템 생성 및 저장
            for (CheckoutItemDTO item : request.getItems()) {
                OrderItem orderItem = OrderItem.builder()
                        .orderId(savedOrder.getOrderId())
                        .productId(item.getProductId())
                        .name(item.getProductName())
                        .quantity(item.getQuantity())
                        .status("PREPARING")  // 준비중
                        .totalPrice(item.getTotalPrice())
                        .deliveryFee(0)  // 개별 아이템 배송비는 0
                        .imageUrl(item.getImageUrl())
                        .build();

                orderItemRepository.save(orderItem);
                log.info("주문 아이템 저장: productId={}, quantity={}",
                        item.getProductId(), item.getQuantity());
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
     * 사용자별 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders(String userId) {
        try {
            log.info("사용자 주문 목록 조회: userId={}", userId);

            List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);

            return orders.stream()
                    .map(this::convertToOrderDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("주문 목록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderDetail(String orderId, String userId) {
        try {
            log.info("주문 상세 조회: orderId={}, userId={}", orderId, userId);

            Order order = orderRepository.findByOrderIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

            return convertToOrderDTO(order);

        } catch (Exception e) {
            log.error("주문 상세 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 상세 조회 중 오류가 발생했습니다.");
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

            order.setOrderStatus(newStatus);

            // 배송 시작 시 배송일 설정
            if ("SHIPPING".equals(newStatus)) {
                order.setShippingDate(LocalDateTime.now());
                order.setTrackingNumber("TRK" + System.currentTimeMillis());
                order.setDeliveryCompany("한진택배");
            }

            orderRepository.save(order);
            log.info("주문 상태 변경 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("주문 상태 변경 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 상태 변경 중 오류가 발생했습니다.");
        }
    }

    /**
     * 주문 취소
     */
    public void cancelOrder(String orderId, String userId) {
        try {
            log.info("주문 취소: orderId={}, userId={}", orderId, userId);

            Order order = orderRepository.findByOrderIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

            // 취소 가능한 상태 체크
            if (!"ORDER_COMPLETED".equals(order.getOrderStatus()) &&
                    !"PREPARING".equals(order.getOrderStatus())) {
                throw new RuntimeException("취소할 수 없는 주문 상태입니다.");
            }

            order.setOrderStatus("CANCELLED");
            orderRepository.save(order);

            log.info("주문 취소 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("주문 취소 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 취소 중 오류가 발생했습니다.");
        }
    }

    /**
     * 주문 목록 페이징 조회
     */
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

    /**
     * Order 엔티티를 OrderDTO로 변환
     */
    private OrderDTO convertToOrderDTO(Order order) {
        // 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedDateAsc(order.getOrderId());

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
                .orderItems(orderItemDTOs)
                .build();
    }

    /**
     * OrderItem 엔티티를 OrderItemDTO로 변환
     */
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