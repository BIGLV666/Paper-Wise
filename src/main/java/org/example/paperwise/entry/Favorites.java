package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@TableName(value = "favorites",autoResultMap = true )
@NoArgsConstructor
public class Favorites {
    @TableId(value = "favorite_id",type = IdType.AUTO)
    private Long favoriteId;
    private String favoriteName;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object cardIds;
    @TableField(value = "user_id")
    private Long userid;
    private Long likeCount;
    private Long lookCount;
    private String shareId;
    private Integer isPublic;//1-ok 0-私有
    private LocalDate createTime;
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
