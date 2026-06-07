package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.paperwise.entry.CardInFavoritesRecord;

import java.util.List;

@Mapper
public interface CardInFavoritesRecordMapper extends BaseMapper<CardInFavoritesRecord> {
   int batchInsert(List<CardInFavoritesRecord>list);
}
