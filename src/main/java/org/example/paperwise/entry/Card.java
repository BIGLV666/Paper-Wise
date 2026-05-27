package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.paperwise.enums.CardDifficulty;
import org.example.paperwise.enums.CardMastery;
import org.example.paperwise.enums.CardType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value="card",autoResultMap=true)
@NoArgsConstructor
public class Card {
    @TableId(type = IdType.AUTO)
    private Long cardId;
    private Long userid;
    private String title;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options;//选项
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object answer;
    private String explanation;
    private String questionType;

    private CardType cardType;
    private CardDifficulty cardDifficulty;
    private CardMastery cardMastery;

    private Integer reviewCount;

    private LocalDateTime createTime;
    private LocalDate nextReviewDate;

}
