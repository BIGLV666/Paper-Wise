package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@NoArgsConstructor
public class WrongReview {
    @TableId(type = IdType.AUTO,value = "wrong_review_id")
    private Long wrongReviewId;
    @TableField(value = "card_id")
    private Long cardId;
    @TableField(value = "user_id")
    private Long userId;
    private Long wrongQuestionId;
    private LocalDate nextReviewTime;
    private LocalDate lastReviewTime;
    private Integer stage;//当前复习多少次
    private Integer intervalDays;//下次复习间隔多少天(1,3,7,15)


    public WrongReview(Long cardId, Long userId, Long wrongQuestionId, LocalDate nextReviewTime, LocalDate lastReviewTime,  Integer stage, Integer intervalDays) {
        this.cardId=cardId;
        this.userId=userId;
        this.wrongQuestionId=wrongQuestionId;
        this.nextReviewTime=nextReviewTime;
        this.lastReviewTime=lastReviewTime;
        this.stage=stage;
        this.intervalDays=intervalDays;

    }
}
