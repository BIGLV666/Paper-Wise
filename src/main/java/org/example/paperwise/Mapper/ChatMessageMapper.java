package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.paperwise.entry.ChatMessage;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    List<ChatMessage> getRecentBySessionId(@Param("sessionId") String sessionId,@Param("userId")Long userId,@Param("limit")int limit);

    List<ChatMessage> getAllHistoryMessages(@Param("sessionId")String sessionId,@Param("userId") Long userId);

    void deleteHistory(@Param(("sessionId")) String sessionId, @Param("userId") Long userId);
}
