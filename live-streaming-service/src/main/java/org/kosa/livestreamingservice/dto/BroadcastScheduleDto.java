package org.kosa.livestreamingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastScheduleDto {
    private String time;
    private List<BroadcastDto.AlarmDto> broadcasts;
}