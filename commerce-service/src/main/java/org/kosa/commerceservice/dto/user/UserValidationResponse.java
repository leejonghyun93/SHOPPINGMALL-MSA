package org.kosa.commerceservice.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserValidationResponse {
    private boolean valid;
    private String userId;
    private String message;
    private String status; // ACTIVE, INACTIVE, DELETED 등
}