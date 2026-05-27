package org.example.paperwise.Until;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CopyUntil {
    @Autowired
    private ObjectMapper objectMapper;
    public  <T> T copy(T source, Class<T> targetClass) throws NoSuchMethodException {
        try {
            String json=objectMapper.writeValueAsString(source);
            return (T) objectMapper.readValue(json, targetClass);
        } catch (JsonProcessingException e) {
            log.error("拷贝失败{}",e.getMessage(),e);
            throw new RuntimeException("拷贝失败");
        }
    }
}
