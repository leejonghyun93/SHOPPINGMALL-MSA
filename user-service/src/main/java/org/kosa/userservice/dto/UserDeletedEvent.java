package org.kosa.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDeletedEvent {
    private String userid;
    private String email;

    private String  deletedAt;
    private String eventType;
}