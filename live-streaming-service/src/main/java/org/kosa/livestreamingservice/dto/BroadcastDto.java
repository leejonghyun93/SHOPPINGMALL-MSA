package org.kosa.livestreamingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastDto {
    private Long id;
    private String title;
    private String thumbnailUrl;
    private String broadcasterName;
    private String productName;
    private Integer salePrice;
    private String status;
    private Boolean isNotificationSet;
}