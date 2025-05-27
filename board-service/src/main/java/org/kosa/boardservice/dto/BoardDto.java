package org.kosa.boardservice.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class BoardDto {

    private Long bno;
    private String title;
    private String content;
    private String writer;
    private String guestWriter;
    private String passwd;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime regDate;
    private int viewCount;
    private String nickName;
    private String isPrivate;
    private String writerName;
    private Long writerId;
    private String name;


    public String getFormattedRegDate() {
        return regDate != null ? regDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }
    public String formatLocalDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime != null ? dateTime.format(formatter) : "";
    }
    // regDate null 처리 메서드
    public LocalDateTime getRegDate() {
        if (regDate == null) {
            return LocalDateTime.now(); // null일 경우 현재 시간으로 설정
        }
        return regDate;
    }

}
