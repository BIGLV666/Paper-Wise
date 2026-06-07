package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import org.example.paperwise.enums.CardDifficulty;
import org.example.paperwise.enums.CardMastery;
import org.example.paperwise.enums.CardType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class AiGeneratedCard {
    @TableId(type = IdType.AUTO,value = "ai_generated_card_d")
    private Long aiGeneratedCardId;
    @TableField(value = "session_id")
    private String sessionId;

    private String status;
    private String name;

    @TableField(value = "user_id")
    private Long userId;
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

    private LocalDateTime createTime;

    public String getAnswerString() {
        if (answer == null) return null;
        if (answer instanceof List) {
            return ((List<?>) answer).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining("、"));
        }
        if (answer instanceof Object[]) {
            return Arrays.stream((Object[]) answer)
                    .map(String::valueOf)
                    .collect(Collectors.joining("、"));
        }
        return answer.toString();
    }

}
