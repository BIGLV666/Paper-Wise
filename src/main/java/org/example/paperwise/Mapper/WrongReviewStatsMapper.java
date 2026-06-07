package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.paperwise.entry.WrongReviewStats;
@Mapper
public interface WrongReviewStatsMapper extends BaseMapper<WrongReviewStats> {
    WrongReviewStats getWrongReviewStatsByDay(Long user_id);
    int updateReviewProgress(Long user_id);
    WrongReviewStats getLastRecord(Long user_id);
}
