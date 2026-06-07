package org.example.paperwise.Dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;
@Data
public class ShareFavoritesDto {
    private String shareId;
    private Long userId;
    private String username;
    private String favoritesName;
    private Long favoritesId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object cardIds;
    private Long lookCount;
    private Long likeCount;
    private String expireTime;
    public List<Long> getCardIds() {
        if (cardIds == null) return null;
        List<?> list = (List<?>) cardIds;
        return list.stream()
                .map(o -> ((Number) o).longValue())
                .collect(Collectors.toList());
    }
    public void setCardIds(List<Long> cardIds) {
        this.cardIds = cardIds;  // Object 类型，可以接收 List<Long>
    }




}
