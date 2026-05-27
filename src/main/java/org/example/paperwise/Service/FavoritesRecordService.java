package org.example.paperwise.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Dto.LikeFavoritesDto;
import org.example.paperwise.Mapper.FavoritesLikeRecordMapper;
import org.example.paperwise.Mapper.FavoritesMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class FavoritesRecordService {
    @Autowired
    private FavoritesLikeRecordMapper favoritesLikeRecordMapper;
    @Autowired
    private FavoritesMapper favoritesMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    private static final String FAVORITES_LIKE_COUNT_KEY = "favorites_like_count";


    /**
     * 自己的喜欢
     * @param userId Long
     * @return LikeFavoritesDto
     */
    public List<LikeFavoritesDto> getAllLikeFavoritesDto(Long userId) {
        return favoritesLikeRecordMapper.getLikeFavoritesDto(userId);
    }

    //取消点赞
    @Transactional
    public void deleteLike(Long userId, Long FavoritesId) {
        int r1 = favoritesLikeRecordMapper.deleteLike(userId,FavoritesId);
        int r2= favoritesMapper.decreaseLikeCount(FavoritesId);
        if(r1==0||r2==0){
            throw new RuntimeException("取消失败");
        }
    }


    //点赞
    public void upLikeCount(Long userId,Long favoritesId){
        if(favoritesLikeRecordMapper.findByUserId(userId,favoritesId)!=0){
            throw new RuntimeException("您已为该收藏夹点过赞了");
        }
        redisTemplate.opsForValue().increment(FAVORITES_LIKE_COUNT_KEY +favoritesId+":"+userId ,1);
    }

}
