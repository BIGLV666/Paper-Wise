package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.paperwise.entry.Card;

import java.util.List;
import java.util.Map;


@Mapper
public interface CardMapper extends BaseMapper<Card> {

    List<Map<String,Integer>> getAllQuestionType(Long userid);
    int batchAddCard(@Param("list") List<Card> cards);
    Long getCardCount(Long userid);
    List<Card>selectAll();
}
