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
public class CheckoutPrepareRequestDTO {
    private String userId;
    private List<CheckoutItemDTO> items;
}
