package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.paperwise.entry.Note;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
