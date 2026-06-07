package org.example.paperwise.Dto;

import lombok.Data;

@Data
public class UserStatusDto {
    Long userid;
    String username;
    String email;
    Long cardCount;
    Long reviewCount;
    Long masteredCount;

}
