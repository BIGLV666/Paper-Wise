package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.paperwise.Dto.WrongCardDto;
import org.example.paperwise.entry.WrongQuestion;

import java.util.List;

@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {
    List<WrongCardDto>getWrongQuestions(Long userId);
}
