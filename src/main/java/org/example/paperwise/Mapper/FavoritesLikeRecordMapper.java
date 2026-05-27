package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.paperwise.Dto.LikeFavoritesDto;
import org.example.paperwise.entry.FavoritesLikeRecord;

import java.util.List;

@Mapper
public  interface FavoritesLikeRecordMapper extends BaseMapper<FavoritesLikeRecord> {
    Integer findByUserId(@Param("userId") Long userId, @Param("favoritesId") Long favoritesId);
    List<LikeFavoritesDto> getLikeFavoritesDto(Long userid);
    int deleteLike(@Param("userId") Long userId,@Param("favoritesId") Long FavoritesId);
}
