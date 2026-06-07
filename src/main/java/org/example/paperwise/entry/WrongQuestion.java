package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class WrongQuestion {
    @TableId(type = IdType.AUTO)
    private  Long wrongQuestionId;
    @TableField(value = "user_id")
    private  Long userId;
    @TableField (value = "card_id")
    private Long cardId;
    private Integer wrongCount;
    private String wrongQuestionCategory;//为什么错
    private LocalDateTime createTime;

    public WrongQuestion(Long userId,Long cardId,Integer wrongCount,String wrongQuestionCategory){
        this.userId = userId;
        this.cardId = cardId;
        this.wrongCount = wrongCount;
        this.wrongQuestionCategory = wrongQuestionCategory;
    }

}
