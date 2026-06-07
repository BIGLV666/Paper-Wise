package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.paperwise.entry.Favorites;

@Mapper
public interface FavoritesMapper extends BaseMapper<Favorites> {
    int upLookCount(@Param("favoritesId")Long favoritesId,@Param("lookCount")Long lookCount);
    int upLikeCount(@Param("favoritesId")Long favoritesId,@Param("likeCount")Long likeCount);
    int decreaseLikeCount(@Param("favoriteId") Long favoriteId);
}
