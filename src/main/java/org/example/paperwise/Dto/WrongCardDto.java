package org.example.paperwise.Dto;


import org.example.paperwise.enums.WrongQuestionCategory;

import java.time.LocalDateTime;


public class WrongCardDto {
    private Long wrongQuestionId;
    private Long cardId;
    private Integer wrongCount;
    private String wrongQuestionCategory;
    private WrongQuestionCategory status;
    private LocalDateTime lastWrongTime;
    private String title;
    private String questionType;
}
