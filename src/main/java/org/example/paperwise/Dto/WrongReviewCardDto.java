package org.example.paperwise.Dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.List;

@Data
public class WrongReviewCardDto {
    private Long wrongReviewId;
    private Long wrongQuestionId;
    private String wrongContent;
   private Long cardId;
   private String title;
   private String questionType;
   @TableField(typeHandler = JacksonTypeHandler.class)
   private List<String> options;
   @TableField(typeHandler = JacksonTypeHandler.class)
   private Object answer;
   private String explanation;
}
