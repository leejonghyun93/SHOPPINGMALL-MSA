package org.kosa.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageDto<T> {
    private List<T> content;  // 페이징된 데이터 리스트
    private long totalElements; // 총 데이터 수
    private int totalPages; // 총 페이지 수
    private int page;  // 현재 페이지 번호
    private int size;  // 페이지 당 데이터 수
}