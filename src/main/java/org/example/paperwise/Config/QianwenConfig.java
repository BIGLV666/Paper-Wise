package org.example.paperwise.Config;

import lombok.Data;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "qianwen")
@Data
public class QianwenConfig {
    private String apikey;
    private String model;
}
