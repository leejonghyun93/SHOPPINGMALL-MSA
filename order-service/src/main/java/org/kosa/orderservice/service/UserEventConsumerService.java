package org.kosa.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.dto.Order;
import org.kosa.orderservice.dto.UserWithdrawalEvent;
import org.kosa.orderservice.mapper.OrderRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumerService {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "user-withdrawal-topic", groupId = "order-service-group")
    @Transactional
    public void handleUserWithdrawal(
            @Payload UserWithdrawalEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("🔥 사용자 탈퇴 이벤트 수신 - userId: {}, eventId: {}",
                    event.getUserId(), event.getEventId());
            log.info("📍 Kafka 메타데이터 - topic: {}, partition: {}, offset: {}",
                    topic, partition, offset);

            // 1. 해당 사용자의 활성 주문 조회 (취소 가능한 상태들)
            List<Order> activeOrders = orderRepository.findActiveOrdersByUserId(event.getUserId());

            if (activeOrders.isEmpty()) {
                log.info("📝 해당 사용자의 활성 주문 없음 - userId: {}", event.getUserId());
            } else {
                log.info("📦 처리할 활성 주문 {} 건 발견 - userId: {}",
                        activeOrders.size(), event.getUserId());

                // 2. 활성 주문들을 취소 상태로 변경
                for (Order order : activeOrders) {
                    String originalStatus = order.getOrderStatus();
                    order.setOrderStatus("CANCELLED_BY_WITHDRAWAL");
                    order.setUpdatedDate(LocalDateTime.now());

                    // 배송 메모에 취소 사유 추가
                    String cancelReason = "회원 탈퇴로 인한 자동 취소 (원래 상태: " + originalStatus + ")";
                    order.setDeliveryMemo(
                            (order.getDeliveryMemo() != null ? order.getDeliveryMemo() + " | " : "") +
                                    cancelReason
                    );

                    orderRepository.save(order);

                    log.info("📋 주문 취소 처리 완료 - orderId: {}, userId: {}, 원래상태: {} → CANCELLED_BY_WITHDRAWAL",
                            order.getOrderId(), event.getUserId(), originalStatus);
                }
            }

            // 3. 모든 주문 이력 조회 및 개인정보 마스킹 처리 (GDPR 준수)
            List<Order> allOrders = orderRepository.findByUserId(event.getUserId());

            for (Order order : allOrders) {
                // 개인정보 마스킹
                order.setPhone("***-****-****");
                order.setEmail("***@***.com");
                order.setRecipientName("탈퇴회원");
                order.setRecipientPhone("***-****-****");
                order.setOrderAddressDetail("***마스킹된 주소***");
                order.setOrderZipcode("*****");
                order.setUpdatedDate(LocalDateTime.now());

                orderRepository.save(order);
            }

            log.info("🔒 개인정보 마스킹 처리 완료 - userId: {}, 총 주문: {} 건",
                    event.getUserId(), allOrders.size());

            log.info("✅ 사용자 탈퇴 처리 완료 - userId: {}, 취소된 주문: {} 건, 마스킹된 주문: {} 건",
                    event.getUserId(), activeOrders.size(), allOrders.size());

            // 4. 수동 커밋
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ 사용자 탈퇴 처리 중 오류 - userId: {}, error: {}",
                    event.getUserId(), e.getMessage(), e);
            // 에러 발생 시 재처리를 위해 acknowledge 하지 않음
        }
    }
}