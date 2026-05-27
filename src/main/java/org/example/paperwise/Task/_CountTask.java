package org.example.paperwise.Task;

import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Mapper.FavoritesLikeRecordMapper;
import org.example.paperwise.Mapper.FavoritesMapper;
import org.example.paperwise.entry.FavoritesLikeRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
                    String key= new String(cursor.next());
                    Long favoritesId= Long.valueOf(key.substring(FAVORITES_LOOK_COUNT_KEY.length()));

                    Long lookCount=(Long)redisTemplate.opsForValue().getAndDelete(key);
                    if(lookCount==null||lookCount<=0){
                        continue;
                    }


                    int update=favoritesMapper.upLookCount(favoritesId,lookCount);
                    if(update>0){
                        count++;
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

                    Long likeCount =(Long)redisTemplate.opsForValue().getAndDelete(key);
                    if(likeCount ==null|| likeCount <=0){
                        continue;
                    }


                    int update=favoritesMapper.upLikeCount(favoritesId, likeCount);
                    int record=favoritesLikeRecordMapper.insert(new FavoritesLikeRecord(userid,favoritesId));
                    if(update>0&&record>0){
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
