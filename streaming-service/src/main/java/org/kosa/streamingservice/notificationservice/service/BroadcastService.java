package org.kosa.streamingservice.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.dto.BroadcastDto;
import org.kosa.notificationservice.dto.BroadcastScheduleDto;
import org.kosa.notificationservice.entity.BroadcastEntity;
import org.kosa.notificationservice.repository.BroadcastRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastService {

    private final BroadcastRepository broadcastRepository;

    /**
     * 날짜별 방송 스케줄 조회
     */
    public List<BroadcastScheduleDto> getBroadcastScheduleByDate(LocalDate date) {
        log.info("방송 스케줄 조회: {}", date);

        List<BroadcastEntity> broadcasts = broadcastRepository.findByScheduledDate(date);
        log.info("조회된 방송 수: {}", broadcasts.size());

        // 시간대별로 그룹핑
        Map<String, List<BroadcastDto>> groupedByTime = broadcasts.stream()
                .collect(Collectors.groupingBy(
                        broadcast -> broadcast.getScheduledStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        LinkedHashMap::new,
                        Collectors.mapping(this::convertToDto, Collectors.toList())
                ));

        return groupedByTime.entrySet().stream()
                .map(entry -> BroadcastScheduleDto.builder()
                        .time(entry.getKey())
                        .broadcasts(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private BroadcastDto convertToDto(BroadcastEntity entity) {
        return BroadcastDto.builder()
                .id(entity.getBroadcastId())
                .title(entity.getTitle())
                .thumbnailUrl(entity.getThumbnailUrl())
                .broadcasterName("방송자" + entity.getBroadcasterId())  // String으로 변경됨
                .productName(entity.getDescription())
                .salePrice(generateRandomPrice())
                .status(entity.getBroadcastStatus())
                .isNotificationSet(false)
                .build();
    }

    private Integer generateRandomPrice() {
        return (int) (Math.random() * 100000) + 10000;
    }
}