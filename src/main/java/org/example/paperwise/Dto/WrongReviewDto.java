package org.example.paperwise.Dto;

import lombok.Data;

import java.util.List;

@Data
public class WrongReviewDto {
    private Integer total;
    private List<WrongReviewCardDto> wrongReviewCardDtos;

}
