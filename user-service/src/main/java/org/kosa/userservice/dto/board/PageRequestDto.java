package org.kosa.userservice.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDto {

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;

    private String searchValue;

    @Builder.Default
    private String sortBy = "createdDate";

    public int getStartRow() {
        return (page - 1) * size;
    }

    public void validate() {
        if (page < 1) {
            this.page = 1;
        }
        if (size < 1 || size > 100) {
            this.size = 10;
        }
        if (sortBy == null || sortBy.trim().isEmpty() || "latest".equals(sortBy)) {
            this.sortBy = "createdDate";
        }
        if (searchValue != null && searchValue.trim().isEmpty()) {
            this.searchValue = null;
        }
    }

    @Override
    public String toString() {
        return String.format("PageRequestDto(page=%d, size=%d, searchValue=%s, sortBy=%s, startRow=%d)",
                page, size, searchValue, sortBy, getStartRow());
    }
}