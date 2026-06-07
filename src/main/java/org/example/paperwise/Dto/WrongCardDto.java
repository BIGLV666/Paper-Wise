package org.example.paperwise.Dto;



import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WrongCardDto {
    private Long wrongQuestionId;
    private Long cardId;
    private Integer wrongCount;
    private String wrongQuestionCategory;
    private LocalDateTime lastWrongTime;
    private String title;
    private String questionType;
}
