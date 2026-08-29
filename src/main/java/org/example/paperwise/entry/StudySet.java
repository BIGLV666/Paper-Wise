package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "study_set", autoResultMap = true)
public class StudySet {
    @TableId(type = IdType.AUTO, value = "study_set_id")
    private Long studySetId;
    private Long userId;
    private String name;
    private String description;
    @TableField(value = "card_ids", typeHandler = JacksonTypeHandler.class)
    private List<Integer> cardIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
