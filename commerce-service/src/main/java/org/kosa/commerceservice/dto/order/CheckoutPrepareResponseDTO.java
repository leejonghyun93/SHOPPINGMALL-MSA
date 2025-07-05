package org.kosa.commerceservice.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutPrepareResponseDTO {
    private String userId;
    private List<CheckoutItemDTO> items;
    private Integer totalItemPrice;
    private Integer deliveryFee;
    private Integer discountAmount;
    private Integer finalAmount;
    private Integer expectedPoint;
    private String deliveryInfo;

    public static class CheckoutPrepareResponseDTOBuilder {
        private String deliveryInfo = "23시 전 주문 시 내일 아침 7시 전 도착";
    }
}
