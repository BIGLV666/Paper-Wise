package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.paperwise.entry.ChatSession;

import java.util.List;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
    List<ChatSession> getAllHistory(Long userId);
}
