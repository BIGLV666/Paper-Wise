package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@TableName(value = "favorites",autoResultMap = true )
@NoArgsConstructor
public class Favorites {
    private Long favoriteId;
    private String favoriteName;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long>cardId;
    private Long userid;
    private Long likeCount;
    private Long lookCount;
    private String sharId;
    private Integer isPublic;//1-ok 0-私有
    private LocalDate createTime;
}
