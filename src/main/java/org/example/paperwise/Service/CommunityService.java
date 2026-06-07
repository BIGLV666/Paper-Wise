package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.Mapper.FavoritesMapper;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.Favorites;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class CommunityService {
    @Autowired
    private FavoritesMapper favoritesMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private CardMapper cardMapper;

    private static final String RANK_FAVORITES_KEY="rank_favorites_key";

    //返回用户收藏夹内容
    public List<Card> getAllCard(List<Long>cardIds){
        return cardMapper.selectList(new QueryWrapper<Card>().in("card_id",cardIds));
    }
    //分页返回所有的公开收藏
    public Page<Favorites>getAllFavorites(int page,int size,String category){
        if(page < 1) page = 1;
        if(size<1||size>30){
            size=30;
        }
        Page<Favorites> pageInfo = new Page<>(page,size);
        QueryWrapper<Favorites> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_public",1);
        if(category==null|| category.isEmpty() || category.equals("look_count")){
        queryWrapper.orderByDesc("look_count");}
        else if(category.equals("like_count")){
            queryWrapper.orderByDesc("like_count");
        }else{
            queryWrapper.orderByDesc("create_time");
        }
        return favoritesMapper.selectPage(pageInfo,queryWrapper);
    }

    //获取排行榜前10
    public List<Favorites> getAllFavoritesTop(){
        Set<Object>ids=redisTemplate.opsForZSet().reverseRange(RANK_FAVORITES_KEY,0,11);
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        List<Long>idList=ids.stream().map(id->Long.valueOf(id.toString())).toList();
        List<Favorites> favoritesList=favoritesMapper.selectBatchIds(idList);
        List<Long> favoritesIds = new ArrayList<>();
        Map<Long,Favorites> favoritesMap = new HashMap<>();
        for(Favorites favorites:favoritesList){
            favoritesMap.put(favorites.getFavoriteId(),favorites);
        }

        List<Favorites>result=new ArrayList<>();
        for(Long id:idList){
            result.add(favoritesMap.get(id));
        }

        return result;
    }

}
