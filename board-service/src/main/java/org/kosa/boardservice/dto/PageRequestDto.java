package org.kosa.boardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor  // 🔥 추가: 기본 생성자
@AllArgsConstructor // 🔥 추가: 전체 생성자
public class PageRequestDto {

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;

    private String searchValue;

    @Builder.Default
    private String sortBy = "createdDate"; // 🔥 수정: "latest" -> "createdDate"

    public int getStartRow() {
        return (page - 1) * size;
    }

    // 🔥 추가: 유효성 검사 메서드
    public void validate() {
        if (page < 1) {
            this.page = 1;
        }
        if (size < 1 || size > 100) {
            this.size = 10;
        }
        if (sortBy == null || sortBy.trim().isEmpty() || "latest".equals(sortBy)) {
            this.sortBy = "createdDate"; // latest를 createdDate로 변환
        }
        // searchValue null 체크
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