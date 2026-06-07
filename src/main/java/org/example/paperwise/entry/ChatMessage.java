package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long chatMessageId;
    private Long userId;
    private String role;
    private String sessionId;
    private String content;
    private LocalDateTime createTime;

}
