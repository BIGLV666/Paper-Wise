package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.paperwise.enums.WrongQuestionCategory;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class WrongQuestion {
    @TableId(type = IdType.AUTO)
    private  Long wrongQuestionId;
    private  Long userId;
    private Long cardId;
    private Integer wrongCount;
    private LocalDateTime lastWrongTime;
    private String wrongQuestionCategory;//为什么错
    private WrongQuestionCategory status;
    private LocalDateTime createTime;

    public WrongQuestion(Long userId,Long cardId,Integer wrongCount,String wrongQuestionCategory,WrongQuestionCategory status){
        this.userId = userId;
        this.cardId = cardId;
        this.wrongCount = wrongCount;
        this.wrongQuestionCategory = wrongQuestionCategory;
        this.status = status;
    }

}
