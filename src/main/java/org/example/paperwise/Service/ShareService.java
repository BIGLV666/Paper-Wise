package org.example.paperwise.Service;


import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Dto.ShareFavoritesDto;
import org.example.paperwise.Mapper.ShareMapper;
import org.example.paperwise.Until.DataUtile;
import org.example.paperwise.entry.Share;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;



@Slf4j
@Service
public class ShareService {
    private static final String FAVORITES_LOOK_COUNT_KEY = "favorites_look_count";
    @Autowired
    private ShareMapper shareMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;


    //分页返回所有的share
    public List<Share> getAllShare(int size, int page) {
        if (size < 0 || size > 30) {
            size = 30;
        }
        int offset = (page - 1) * size;
        return shareMapper.getAllShare(offset, size);
    }
    //根据链接返回收藏夹
    public ShareFavoritesDto getShareFavorites(String shareId){
        ShareFavoritesDto shareFavoritesDto = shareMapper.getShareFavorites(shareId);
        if(shareFavoritesDto == null){
            throw new  RuntimeException("未找到该收藏夹，或该收藏夹已过期");
        }



        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        shareFavoritesDto.setExpireTime(DataUtile.formatDate(LocalDateTime.parse(shareFavoritesDto.getExpireTime(),formatter)));
        return shareFavoritesDto;
    }
    //添加浏览量
    public void upLookCount(Long favoritesId){

        redisTemplate.opsForValue().increment(FAVORITES_LOOK_COUNT_KEY+favoritesId ,1);
    }

}
