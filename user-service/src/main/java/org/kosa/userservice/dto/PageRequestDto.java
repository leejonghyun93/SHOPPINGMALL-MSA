package org.kosa.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDto {
    private int page = 1; // 1부터 시작
    private int size = 10;
    private String searchValue;
    private String sortBy;
}