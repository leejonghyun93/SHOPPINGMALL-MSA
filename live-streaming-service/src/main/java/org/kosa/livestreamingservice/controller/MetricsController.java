package org.kosa.livestreamingservice.controller;

// import io.micrometer.core.instrument.Gauge; // 이 import 제거
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class MetricsController {

    @Autowired
    private MeterRegistry meterRegistry;

    // 메트릭 값들을 저장할 Map
    private final ConcurrentHashMap<String, AtomicInteger> viewerCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> chatParticipants = new ConcurrentHashMap<>();
    private final Counter paymentCounter;
    private final Counter chatMessageCounter;

    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Counter 초기화
        this.paymentCounter = Counter.builder("payment_total_amount")
                .description("총 결제 금액")
                .register(meterRegistry);

        this.chatMessageCounter = Counter.builder("chat_messages_total")
                .description("채팅 메시지 수")
                .register(meterRegistry);
    }

    // 시청자 수 메트릭 업데이트
    public void updateViewerCount(String broadcastId, int count) {
        AtomicInteger viewerCount = viewerCounts.computeIfAbsent(broadcastId,
                k -> {
                    AtomicInteger atomicCount = new AtomicInteger(0);
                    // Gauge 대신 직접 등록
                    meterRegistry.gauge("live_broadcast_viewers",
                            io.micrometer.core.instrument.Tags.of("broadcast_id", broadcastId),
                            atomicCount, AtomicInteger::get);
                    return atomicCount;
                });

        viewerCount.set(count);
    }

    // 채팅 참여자 수 메트릭 업데이트
    public void updateChatParticipants(String broadcastId, int count) {
        AtomicInteger participantCount = chatParticipants.computeIfAbsent(broadcastId,
                k -> {
                    AtomicInteger atomicCount = new AtomicInteger(0);
                    // Gauge 대신 직접 등록
                    meterRegistry.gauge("chat_participants_active",
                            io.micrometer.core.instrument.Tags.of("broadcast_id", broadcastId),
                            atomicCount, AtomicInteger::get);
                    return atomicCount;
                });

        participantCount.set(count);
    }

    // 결제 메트릭 증가
    public void incrementPayment(double amount) {
        paymentCounter.increment(amount);
    }

    // 채팅 메시지 메트릭 증가
    public void incrementChatMessage() {
        chatMessageCounter.increment();
    }

    // 방송 상태 업데이트
    public void updateBroadcastStatus(String broadcastId, int status) {
        meterRegistry.gauge("broadcast_status",
                io.micrometer.core.instrument.Tags.of("broadcast_id", broadcastId),
                status);
    }

    // 테스트용 엔드포인트들
    @PostMapping("/metrics/viewer/{broadcastId}")
    @ResponseBody
    public String updateViewer(@PathVariable String broadcastId, @RequestParam int count) {
        updateViewerCount(broadcastId, count);
        return "시청자 수 업데이트: " + count;
    }

    @PostMapping("/metrics/chat/{broadcastId}")
    @ResponseBody
    public String updateChat(@PathVariable String broadcastId, @RequestParam int count) {
        updateChatParticipants(broadcastId, count);
        return "채팅 참여자 수 업데이트: " + count;
    }

    @GetMapping("/metrics/prometheus")
    @ResponseBody
    public String getMetrics() {
        return "Prometheus metrics available at /actuator/prometheus";
    }
}