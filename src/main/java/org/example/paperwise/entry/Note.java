package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("note")
public class Note {
    @TableId(type = IdType.AUTO, value = "note_id")
    private Long noteId;
    private Long userId;
    private String title;
    private String content;
    private String format;
    private String sourceName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
