package org.kosa.boardservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageRequestDto {
    private int page = 1;
    private int size = 10;
    private String searchValue;
    private String sortBy = "latest";

    public int getStartRow() {
        return (page - 1) * size;
    }
}