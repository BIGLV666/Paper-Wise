package org.example.paperwise.entry;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
public class WrongReview {
    private Long wrongReviewId;
    private Long cardId;
    private Long userId;
    private Long wrongQuestionId;
    private LocalDateTime nextReviewTime;
    private LocalDateTime lastReviewTime;
    private String status;//已掌握，未掌握
    private Integer stage;//当前复习多少次
    private Integer intervalDays;//下次复习间隔多少天(1,3,7,15)

    public WrongReview(Long cardId, Long userId, Long wrongQuestionId, LocalDateTime nextReviewTime, LocalDateTime lastReviewTime, String status, Integer stage, Integer intervalDays){
        this.cardId=cardId;
        this.userId=userId;
        this.wrongQuestionId=wrongQuestionId;
        this.nextReviewTime=nextReviewTime;
        this.lastReviewTime=lastReviewTime;
        this.status=status;
        this.stage=stage;
        this.intervalDays=intervalDays;

    }
}
