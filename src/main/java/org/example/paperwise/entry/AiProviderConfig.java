package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_provider_config")
public class AiProviderConfig {
    @TableId(type = IdType.AUTO, value = "ai_provider_config_id")
    private Long aiProviderConfigId;
    private Long userId;
    private String providerName;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Integer enabled;
    private LocalDateTime updatedAt;
}
