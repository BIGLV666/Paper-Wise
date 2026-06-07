package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "chat_session")
public class ChatSession {
    @TableId
    private String sessionId;
    private Long userId;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
