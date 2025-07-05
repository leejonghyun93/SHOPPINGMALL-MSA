package org.kosa.commerceservice.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserWithdrawalEvent {
    private String eventId;
    private String userId;
    private String withdrawnId;
    private String name;
    private String email;
    private String phone;
    private String gradeId;
    private String withdrawalReason;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate originalCreatedDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate withdrawalDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate secessionDate;

    private LocalDateTime eventTimestamp;
    private String eventType;
}