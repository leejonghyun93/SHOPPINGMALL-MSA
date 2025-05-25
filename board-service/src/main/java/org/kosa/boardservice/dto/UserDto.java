package org.kosa.boardservice.dto;


import lombok.Data;

@Data
public class UserDto {
    private Long userId;
    private String name;
    private String nickname;
}