package org.example.paperwise.Dto;

import lombok.Data;

@Data
public class WrongReviewUpdateRequest {
    private Integer quality;
    private Long wrongReviewId;
    private Long wrongQuestionId;

}
