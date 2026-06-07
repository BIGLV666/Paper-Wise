package org.example.paperwise.Task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Mapper.FavoritesLikeRecordMapper;
import org.example.paperwise.Mapper.FavoritesMapper;
import org.example.paperwise.entry.Favorites;
import org.example.paperwise.entry.FavoritesLikeRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class _CountTask {
    @Autowired
    private FavoritesMapper favoritesMapper;
    @Autowired
    private FavoritesLikeRecordMapper favoritesLikeRecordMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    private static final String FAVORITES_LOOK_COUNT_KEY = "favorites_look_count";
    private static final String FAVORITES_LIKE_COUNT_KEY = "favorites_like_count";
    private static final String RANK_FAVORITES_KEY="rank_favorites_key";

    @PostConstruct
    public void init() {
        QueryWrapper<Favorites> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_public",1);
        List<Favorites>all=favoritesMapper.selectList(queryWrapper);
        for(Favorites favorites:all){
        redisTemplate.opsForZSet().add(RANK_FAVORITES_KEY,favorites.getFavoriteId().toString(),favorites.getLikeCount());}
    }


    //排行榜定时修正
    @Scheduled(cron = "0 0 * * * *")
    public void rank(){
        QueryWrapper<Favorites> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_public",1);
        List<Favorites>all=favoritesMapper.selectList(queryWrapper);
        for(Favorites favorites:all){
            Long score = Objects.requireNonNull(redisTemplate.opsForZSet().score(RANK_FAVORITES_KEY, favorites.getFavoriteId().toString())).longValue();
            if(!score.equals(favorites.getLikeCount())){
                redisTemplate.opsForZSet().remove(RANK_FAVORITES_KEY,favorites.getFavoriteId().toString());
                redisTemplate.opsForZSet().incrementScore(RANK_FAVORITES_KEY,favorites.getFavoriteId().toString(),favorites.getLikeCount());
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void upLookCount(){
        int count = 0;
        log.info("upLookCount");
        ScanOptions options=ScanOptions.scanOptions().match(FAVORITES_LOOK_COUNT_KEY+"*")
                        .count(100)
                        .build();
        try(Cursor<byte[]>cursor=redisTemplate.execute(
                (RedisCallback<Cursor<byte[]>>) connection->connection.scan(options)
        )){

            if (cursor != null) {

                while(cursor.hasNext()){
                    String key = new String(cursor.next());
                    String suffix = key.substring(FAVORITES_LOOK_COUNT_KEY.length());

                    // ✅ 校验后缀是否为有效数字
                    if (suffix == null || suffix.isEmpty() || "null".equals(suffix)) {
                        log.warn("无效的key后缀，跳过: key={}, suffix={}", key, suffix);
                        continue;
                    }

                    // 检查是否为数字
                    boolean isNumeric = suffix.matches("\\d+");
                    if (!isNumeric) {
                        log.warn("后缀不是数字，跳过: key={}, suffix={}", key, suffix);
                        continue;
                    }

                    Long favoritesId = Long.valueOf(suffix);
                    Object val = redisTemplate.opsForValue().getAndSet(key, 0);
                    if(val==null){
                        continue;
                    }
                    long lookCount = Long.parseLong(val.toString());
                    if(lookCount==0){
                        redisTemplate.delete(key);
                    }

                    int update=favoritesMapper.upLookCount(favoritesId,lookCount);
                    if(update>0){
                        count++;
                        redisTemplate.delete(key);
                        log.info("更新成功: shareId={}, addCount={}", favoritesId, lookCount);
                    }else {
                        redisTemplate.opsForValue().increment(key, lookCount);
                        log.warn("更新失败: shareId={}, addCount={}redis回滚", favoritesId, lookCount);
                    }
                }
            }
        }catch(Exception e){
            log.error(e.getMessage(),e);
        }finally {
            log.info("upLookCount一共添加{}条数据",count);
        }
    }





    //点赞
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void upLikeCount(){
        int count = 0;
        log.info("upLikeCount");
        ScanOptions options=ScanOptions.scanOptions().match(FAVORITES_LIKE_COUNT_KEY +"*")
                .count(100)
                .build();
        try(Cursor<byte[]>cursor=redisTemplate.execute(
                (RedisCallback<Cursor<byte[]>>) connection->connection.scan(options)
        )){

            if (cursor != null) {

                while(cursor.hasNext()){
                    String key=new String(cursor.next());
                    String[]ids=(key.substring(FAVORITES_LIKE_COUNT_KEY.length()).split(":"));
                    Long favoritesId= Long.valueOf(ids[0]);
                    Long userid= Long.valueOf(ids[1]);

                    Object val = redisTemplate.opsForValue().getAndSet(key, 0);
                    if(val==null){
                        continue;
                    }
                    long likeCount = Long.parseLong(val.toString());
                    if(likeCount==0){
                        redisTemplate.delete(key);
                    }


                    int update=favoritesMapper.upLikeCount(favoritesId, likeCount);
                    int record=favoritesLikeRecordMapper.insert(new FavoritesLikeRecord(userid,favoritesId));
                    if(update>0&&record>0){
                        redisTemplate.delete(key);
                        count++;
                        log.info("更新成功: shareId={}, addCount={}", favoritesId, likeCount);
                    }else {
                        redisTemplate.opsForValue().increment(key, likeCount);
                        log.warn("更新失败: shareId={}, addCount={}redis回滚", favoritesId, likeCount);
                    }
                }
            }
        }catch(Exception e){
            log.error(e.getMessage(),e);
        }finally {
            log.info("upLookCount一共添加{}条数据",count);
        }
    }

}
