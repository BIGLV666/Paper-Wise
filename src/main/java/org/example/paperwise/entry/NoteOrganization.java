package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("note_ai_organization")
public class NoteOrganization {
    @TableId(type = IdType.AUTO, value = "organization_id")
    private Long organizationId;
    private Long noteId;
    private Long userId;
    private String status;
    private String sourceContent;
    private String organizedContent;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
