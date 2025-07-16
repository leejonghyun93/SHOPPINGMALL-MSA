package org.kosa.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImmediateResetRequest {
    private String userid;
    private String email;
    private String name;
    private String newPassword;
}
