package org.example.paperwise.Dto;

import lombok.Data;
import org.example.paperwise.entry.Card;

import java.util.List;

@Data
public class WrongReviewDto {
    private Long wrongReviewId;
    private List<Card>  cards;
    private Long wrongQuestionId;
    private String wrongReviewContent;
    private String status;

}
