package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.paperwise.Dto.WrongReviewCardDto;
import org.example.paperwise.entry.WrongReview;

import java.util.List;

@Mapper
public interface WrongReviewMapper extends BaseMapper<WrongReview> {
    List<WrongReviewCardDto> getAllWrongReviewCardDto(Long userId);
    Long getMasteredCount(Long user_id);
}
