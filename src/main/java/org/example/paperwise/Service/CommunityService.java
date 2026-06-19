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

    /**
     * 获取收藏夹排行榜前10
     * <p>核心逻辑：
     * 1. 从Redis ZSet获取按点赞数降序排列的前10个收藏夹ID
     * 2. 缓存为空时触发排行榜初始化重建
     * 3. 批量查询收藏夹详情并保持原排序顺序返回</p>
     *
     * @return 排行榜前10的收藏夹列表
     */
    public List<Favorites> getAllFavoritesTop() {
        // 从Redis获取排行榜前10（按点赞数降序）
        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(RANK_FAVORITES_KEY, 0, 9);

        // 缓存为空则触发初始化
        if (ids == null || ids.isEmpty()) {
            rank();
            ids = redisTemplate.opsForZSet().reverseRange(RANK_FAVORITES_KEY, 0, 9);
            if (ids == null || ids.isEmpty()) {
                return new ArrayList<>();
            }
        }

        // 批量查询收藏夹详情
        List<Long> idList = ids.stream().map(id -> Long.valueOf(id.toString())).toList();
        List<Favorites> favoritesList = favoritesMapper.selectBatchIds(idList);

        // 使用Map保持收藏夹ID与对象映射关系
        Map<Long, Favorites> favoritesMap = new HashMap<>();
        for (Favorites favorites : favoritesList) {
            favoritesMap.put(favorites.getFavoriteId(), favorites);
        }

        // 按排行榜顺序构建返回结果
        List<Favorites> result = new ArrayList<>();
        for (Long id : idList) {
            result.add(favoritesMap.get(id));
        }

        return result;
    }

    /**
     * 排行榜初始化/重建
     * <p>将所有公开收藏夹按点赞数存入Redis ZSet，用于排行榜查询</p>
     */
    public void rank() {
        // 查询所有公开收藏夹
        QueryWrapper<Favorites> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_public", 1);
        List<Favorites> all = favoritesMapper.selectList(queryWrapper);

        // 删除旧排行榜数据
        redisTemplate.delete(RANK_FAVORITES_KEY);

        // 重建排行榜（以点赞数作为排序分数）
        for (Favorites favorites : all) {
            redisTemplate.opsForZSet().add(
                    RANK_FAVORITES_KEY,
                    favorites.getFavoriteId().toString(),
                    favorites.getLikeCount()
            );
        }
    }


}
