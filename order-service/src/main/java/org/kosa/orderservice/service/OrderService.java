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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 체크아웃 - 주문 생성 (🔥 기본값 설정 강화)
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

            // 🔥 5. 주문 엔티티 생성 - 기본값 설정 강화
            Order order = Order.builder()
                    .userId(request.getUserId())
                    // 🔥 ORDER_STATUS 기본값 설정
                    .orderStatus(request.getOrderStatus() != null && !request.getOrderStatus().trim().isEmpty()
                            ? request.getOrderStatus() : "PENDING")
                    // 🔥 연락처 정보 기본값 설정
                    .phone(request.getPhone() != null ? request.getPhone() : "")
                    .email(request.getEmail() != null ? request.getEmail() : "")
                    // 🔥 배송 정보 기본값 설정 (필수 필드들)
                    .recipientName(request.getRecipientName() != null && !request.getRecipientName().trim().isEmpty()
                            ? request.getRecipientName() : "수령인")
                    .recipientPhone(request.getRecipientPhone() != null && !request.getRecipientPhone().trim().isEmpty()
                            ? request.getRecipientPhone() : "010-0000-0000")
                    .orderZipcode(request.getOrderZipcode() != null ? request.getOrderZipcode() : "")
                    .orderAddressDetail(request.getOrderAddressDetail() != null && !request.getOrderAddressDetail().trim().isEmpty()
                            ? request.getOrderAddressDetail() : "주소 정보 없음")
                    .deliveryMemo(request.getDeliveryMemo() != null ? request.getDeliveryMemo() : "")
                    // 🔥 가격 정보
                    .totalPrice(finalTotalPrice)
                    .deliveryFee(deliveryFee)
                    .discountAmount(0)
                    .usedPoint(request.getUsedPoint() != null ? request.getUsedPoint() : 0)
                    // 🔥 결제 정보 기본값 설정
                    .paymentMethod(request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()
                            ? request.getPaymentMethod() : "CARD")
                    .paymentMethodName(request.getPaymentMethodName() != null && !request.getPaymentMethodName().trim().isEmpty()
                            ? request.getPaymentMethodName() : "신용카드")
                    .savedPoint(savedPoint)
                    // 🔥 배송 예상일 설정
                    .estimatedDate(LocalDateTime.now().plusDays(1).withHour(7).withMinute(0).withSecond(0))  // 익일 오전 7시
                    .build();

            log.info("주문 엔티티 생성 완료: orderId={}, status={}, totalPrice={}, recipientName={}, paymentMethod={}",
                    order.getOrderId(), order.getOrderStatus(), order.getTotalPrice(),
                    order.getRecipientName(), order.getPaymentMethod());

            // 6. 주문 저장
            Order savedOrder = orderRepository.save(order);
            log.info("주문 저장 완료: orderId={}", savedOrder.getOrderId());

            // 7. 주문 아이템 생성 및 저장
            for (CheckoutItemDTO item : request.getItems()) {
                OrderItem orderItem = OrderItem.builder()
                        .orderId(savedOrder.getOrderId())
                        .productId(item.getProductId())
                        // 🔥 상품명 기본값 설정
                        .name(item.getProductName() != null && !item.getProductName().trim().isEmpty()
                                ? item.getProductName() : "상품명 없음")
                        .quantity(item.getQuantity())
                        .status("PREPARING")  // 준비중
                        .totalPrice(item.getTotalPrice())
                        .deliveryFee(0)  // 개별 아이템 배송비는 0
                        .imageUrl(item.getImageUrl() != null ? item.getImageUrl() : "")
                        .build();

                orderItemRepository.save(orderItem);
                log.info("주문 아이템 저장: productId={}, name={}, quantity={}",
                        item.getProductId(), orderItem.getName(), item.getQuantity());
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
     * 🔥 사용자별 활성 주문 조회 (탈퇴 처리용)
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getActiveUserOrders(String userId) {
        try {
            log.info("사용자 활성 주문 조회: userId={}", userId);

            // 활성 상태의 주문들만 조회
            List<Order> orders = orderRepository.findActiveOrdersByUserId(userId);

            return orders.stream()
                    .map(this::convertToOrderDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("활성 주문 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("활성 주문 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 주문 조회 시작: orderId
     */
    public OrderDTO getOrderDetailByOrderId(String orderId) {
        log.info("주문 조회 시작: orderId={}", orderId);

        // 1. 주문 기본 정보 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));
        log.info("주문 정보 조회 성공: {}", order.getOrderId());

        // 2. 주문 상품들 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        log.info("주문 상품 조회 결과: {}개", orderItems.size());

        // 디버깅을 위한 로그 추가
        for (OrderItem item : orderItems) {
            log.info("주문 상품: id={}, name={}, quantity={}",
                    item.getOrderItemId(), item.getName(), item.getQuantity());
        }

        // 3. DTO 변환
        OrderDTO orderDTO = convertToDTO(order);
        List<OrderItemDTO> itemDTOs = orderItems.stream()
                .map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());

        orderDTO.setItems(itemDTOs);
        log.info("최종 OrderDTO 생성 완료: 상품 {}개", itemDTOs.size());

        return orderDTO;
    }

    /**
     * 사용자별 주문 상세 조회
     */
    public OrderDTO getOrderDetail(String orderId, String userId) {
        log.info("사용자별 주문 조회: orderId={}, userId={}", orderId, userId);

        // 1. 주문 조회 및 권한 검증
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        // 주문한 사용자가 맞는지 확인
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("주문 조회 권한이 없습니다");
        }

        // 2. 주문 상품들 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        log.info("사용자별 주문 상품 조회 결과: {}개", orderItems.size());

        // 3. DTO 변환
        OrderDTO orderDTO = convertToDTO(order);
        List<OrderItemDTO> itemDTOs = orderItems.stream()
                .map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());

        orderDTO.setItems(itemDTOs);

        return orderDTO;
    }

    /**
     * 주문 상태 변경 (🔥 탈퇴 처리 포함)
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

            // 🔥 탈퇴로 인한 취소인 경우 특별 처리
            if ("CANCELLED_BY_WITHDRAWAL".equals(newStatus)) {
                order.setUpdatedDate(LocalDateTime.now());
                log.info("탈퇴로 인한 주문 취소 처리: orderId={}", orderId);
            }

            orderRepository.save(order);
            log.info("주문 상태 변경 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("주문 상태 변경 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 상태 변경 중 오류가 발생했습니다.");
        }
    }

    /**
     * 🔥 사용자 탈퇴 시 주문 데이터 처리
     */
    @Transactional
    public void processUserWithdrawalOrders(String userId) {
        try {
            log.info("사용자 탈퇴 주문 처리 시작: userId={}", userId);

            // 1. 해당 사용자의 활성 주문 조회
            List<OrderDTO> activeOrders = getActiveUserOrders(userId);
            log.info("처리 대상 활성 주문 수: {}", activeOrders.size());

            // 2. 각 주문별 처리
            for (OrderDTO order : activeOrders) {
                String orderId = order.getOrderId();
                String currentStatus = order.getOrderStatus();

                log.info("주문 처리: orderId={}, currentStatus={}", orderId, currentStatus);

                // 배송 전 주문은 자동 취소
                if ("PENDING".equals(currentStatus) || "PREPARING".equals(currentStatus) ||
                        "ORDERED".equals(currentStatus) || "PAYMENT_COMPLETED".equals(currentStatus)) {
                    updateOrderStatus(orderId, "CANCELLED_BY_WITHDRAWAL");
                    log.info("주문 자동 취소: orderId={} (탈퇴로 인한 취소)", orderId);
                }
                // 배송 중/완료된 주문은 그대로 유지
                else if ("SHIPPED".equals(currentStatus) || "DELIVERED".equals(currentStatus)) {
                    log.info("배송 중/완료 주문 유지: orderId={}, status={}", orderId, currentStatus);
                }
            }

            log.info("사용자 탈퇴 주문 처리 완료: userId={}, 처리된 주문 수={}", userId, activeOrders.size());

        } catch (Exception e) {
            log.error("사용자 탈퇴 주문 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("사용자 탈퇴 주문 처리 중 오류가 발생했습니다: " + e.getMessage());
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
    private OrderDTO convertToDTO(Order order) {
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
                .build();
    }

    /**
     * Order 엔티티를 OrderDTO로 변환 (주문 아이템 포함)
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
                .items(orderItemDTOs)
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
                .name(orderItem.getName())  // DB의 NAME 컬럼 → productName으로 변환
                .quantity(orderItem.getQuantity())
                .status(orderItem.getStatus())
                .totalPrice(orderItem.getTotalPrice())
                .deliveryFee(orderItem.getDeliveryFee())
                .imageUrl(orderItem.getImageUrl())
                .createdDate(orderItem.getCreatedDate())
                .updatedDate(orderItem.getUpdatedDate())
                .build();
    }

    public List<String> getAllOrderIds() {
        return orderRepository.findAllOrderIds();
    }

    public boolean orderExists(String orderId) {
        return orderRepository.existsById(orderId);
    }
}