package org.kosa.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDto implements Serializable {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String gradeId;
    private String status;
    private LocalDate birthDate;
    private LocalDateTime cachedAt;
}
