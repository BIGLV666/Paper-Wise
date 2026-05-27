package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.paperwise.Dto.ShareFavoritesDto;
import org.example.paperwise.entry.Share;

import java.util.List;

@Mapper
public interface ShareMapper extends BaseMapper<Share> {
    List<Share> getAllShare( @Param("offset") int offest,@Param("size")int size);
    ShareFavoritesDto getShareFavorites( String shareId);
}
