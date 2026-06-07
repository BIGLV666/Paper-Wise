package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.paperwise.entry.AiGeneratedCard;

import java.util.List;

@Mapper
public interface AiGeneratedCardMapper extends BaseMapper<AiGeneratedCard> {
    void batchInsert(List<AiGeneratedCard> cards);
    int batchUpdateStatus(List<AiGeneratedCard>cards);
    List<AiGeneratedCard> getAllHistoryRecord(@Param("user_id")Long user_id);
}
