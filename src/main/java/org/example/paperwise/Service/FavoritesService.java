package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.paperwise.Mapper.CardInFavoritesRecordMapper;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.Mapper.FavoritesMapper;
import org.example.paperwise.Mapper.ShareMapper;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.CardInFavoritesRecord;
import org.example.paperwise.entry.Favorites;
import org.example.paperwise.entry.Share;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FavoritesService {
    @Autowired
    private FavoritesMapper favoritesMapper;
    @Autowired
    private ShareMapper shareMapper;
    @Autowired
    private CardMapper cardMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private CardInFavoritesRecordMapper cardInFavoritesRecordMapper;

    private static final String FAVORITES_LOOK_COUNT_KEY = "favorites_look_count";
    private static final String RANK_FAVORITES_KEY="rank_favorites_key";


    //返回用户收藏夹内容
    public List<Card>getAllCard(List<Long>cardIds){
        return cardMapper.selectList(new QueryWrapper<Card>().in("card_id",cardIds));
    }

    //初始化收藏夹
    @Transactional
    public Favorites createFavorites(Long userid,String favoritesName) {
        Favorites favorites = new Favorites();
        favorites.setUserid(userid);
        favorites.setFavoriteName(favoritesName);
        favorites.setIsPublic(0);
        favorites.setCardIds(new ArrayList<>());
        favorites.setLikeCount(0L);
        favorites.setLookCount(0L);
        int r=favoritesMapper.insert(favorites);
        return favoritesMapper.selectById(favorites.getFavoriteId());

    }

    //添加题目
    @Transactional
    public Favorites addCard(Long favoritesId,Long userid,Long cardId) {
        QueryWrapper<CardInFavoritesRecord> q=new QueryWrapper<>();
        q.eq("card_id",cardId);
        CardInFavoritesRecord cq=cardInFavoritesRecordMapper.selectOne(q);
        if(cq==null){
            CardInFavoritesRecord c=new CardInFavoritesRecord(cardId,favoritesId);
            cardInFavoritesRecordMapper.insert(c);
        }
        Favorites favorites=favoritesMapper.selectOne(new QueryWrapper<Favorites>().eq("favorite_id",favoritesId));
        if(favorites==null){
            throw new RuntimeException("未找到该收藏夹");
        }
        if(favorites.getCardIds().contains(cardId)){
            throw new RuntimeException("题目已经存在");
        }
        if(!(Objects.equals(favorites.getUserid(), userid))){
            throw new RuntimeException("不能修改他人收藏夹");
        }
        List<Long> cardIdList = new ArrayList<>(favorites.getCardIds());
        cardIdList.add(cardId);
        favorites.setCardIds(cardIdList);
        int r=favoritesMapper.updateById(favorites);
        if(r==0){
            throw new RuntimeException("添加失败");
        }
        return favorites;
    }
    //删除仓库
    @Transactional
    public void deleteFavorites(Long favoritesId, Long userid) {
        Favorites favorites=favoritesMapper.selectById(favoritesId);
        if(favorites==null){
            throw new RuntimeException("未找到该收藏夹");
        }
        if(!(Objects.equals(favorites.getUserid(), userid))){
            throw new RuntimeException("不能删除他人的收藏夹");
        }

        redisTemplate.opsForZSet().remove(RANK_FAVORITES_KEY,favorites.getFavoriteId());



        List<Long> cardIdList = favorites.getCardIds();
        QueryWrapper<CardInFavoritesRecord> q=new QueryWrapper<>();
        q.eq("favorites_id",favoritesId).in("card_id",cardIdList);
        int r1=cardInFavoritesRecordMapper.delete(q);

        int r=favoritesMapper.deleteById(favoritesId);

        if(r==0){
            throw new RuntimeException("删除失败");
        }
    }
    //删除卡片
    @Transactional
    public Favorites deleteCard(Long favoritesId,Long userid,Long cardId) {
        Favorites favorites=favoritesMapper.selectById(favoritesId);
        if(favorites==null){
            throw new RuntimeException("未找到该收藏夹");
        }
        if(!(Objects.equals(favorites.getUserid(), userid))){
            throw new RuntimeException("不能修改他人的收藏夹");
        }
        boolean f=favorites.getCardIds().remove(cardId);
        if(!f){
            throw new RuntimeException("未找到该题目");
        }
        List<Long> cardIdList = new ArrayList<>(favorites.getCardIds());
        cardIdList.remove(cardId);
        favorites.setCardIds(cardIdList);

        QueryWrapper<CardInFavoritesRecord> qw=new QueryWrapper<>();
        qw.eq("card_id",cardId);
        qw.eq("favorites_id",favoritesId);
        cardInFavoritesRecordMapper.delete(qw);

        favoritesMapper.updateById(favorites);
        return favorites;
    }

    //分页返回所有收藏
    public Page<Favorites>getAllFavorites(Long userid,int size,int page){
        if(size<0||size>50){
            size=30;
        }
        Page<Favorites> pageList=new Page<>(page,size);
        QueryWrapper<Favorites> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("user_id",userid);
        return favoritesMapper.selectPage(pageList,queryWrapper);

    }

    //修改收藏夹是否可分享
    public Favorites updateIsPublic(Long favoritesId,Long userid,Integer isPublic) {
        if(isPublic!=1&&isPublic!=0){
            throw new RuntimeException("状态不合法");
        }
        Favorites favorites=favoritesMapper.selectById(favoritesId);
        if(favorites==null){
            throw new RuntimeException("该收藏夹不存在");
        }
        if(!(Objects.equals(favorites.getUserid(), userid))){
            throw new RuntimeException("不能操作他人收藏夹");
        }
        favorites.setIsPublic(isPublic);
        int r=favoritesMapper.updateById(favorites);
        if(r==0){
            throw new RuntimeException("修改失败");
        }

        if(isPublic==1){
        redisTemplate.opsForZSet().remove(RANK_FAVORITES_KEY,favorites.getFavoriteId());
        redisTemplate.opsForZSet().add(RANK_FAVORITES_KEY,favorites.getFavoriteId(),favorites.getLikeCount());}
        else{
            redisTemplate.opsForZSet().remove(RANK_FAVORITES_KEY,favorites.getFavoriteId());
        }

        return favorites;
    }




    //生成分享链接
    @Transactional
    public String getShareId(Long favoritesId, Long userid){
        Favorites favorites=favoritesMapper.selectById(favoritesId);
        if(favorites==null){
            throw new RuntimeException("未找到该收藏夹");
        }
        if(!favorites.getUserid().equals(userid)){
            throw new RuntimeException("不能操作他人收藏");
        }
        if(favorites.getIsPublic()==0){
            throw new RuntimeException("该收藏为不可分享");
        }
        if(favorites.getShareId()!=null){
            Share share=shareMapper.selectById(favorites.getShareId());
            if(share.getExpireTime().isAfter(LocalDateTime.now())){
                return "http://localhost:8080/paperwise/share/getsharefavorites?shareId="+share.getUuid();
            }
        }
        String uuid= UUID.randomUUID().toString().replace("-","").substring(0,16);
        favorites.setShareId(uuid);

        //生成share记录
        Share share=new Share(uuid,userid, favorites.getFavoriteName(),favoritesId);

        int r1=favoritesMapper.updateById(favorites);
        int r2=shareMapper.insert(share);
        if(r1==0||r2==0){
            throw new RuntimeException("链接生成失败");
        }

        return "http://localhost:8080/paperwise/share/getsharefavorites?shareId="+uuid;
    }

    //添加浏览量
    public void upLookCount(Long favoritesId){

        redisTemplate.opsForValue().increment(FAVORITES_LOOK_COUNT_KEY+"--"+"favoritesId"+"--"+favoritesId,1);
    }

    //复制他人收藏夹
    @Transactional
    public Favorites CopyFavorites(Long favoritesId,Long userid){
        Favorites favorites=favoritesMapper.selectById(favoritesId);
        if(favorites==null){
            throw new RuntimeException("未找到该收藏夹");
        }
        if(favorites.getIsPublic()==0){
            throw new RuntimeException("该收藏夹是私有的");
        }


        List<CardInFavoritesRecord> records=cardInFavoritesRecordMapper.selectList(new QueryWrapper<CardInFavoritesRecord>().eq("favorites_id",favoritesId));




        Favorites copy=new Favorites();
        copy.setUserid(userid);
        copy.setFavoriteName(favorites.getFavoriteName());
        copy.setCardIds(favorites.getCardIds());
        copy.setLikeCount(0L);
        copy.setLookCount(0L);
        copy.setIsPublic(0);
        copy.setCreateTime(LocalDate.now());
        int r=favoritesMapper.insert(copy);

        for(CardInFavoritesRecord record:records){
            record.setFavoritesId(copy.getFavoriteId());
            record.setCardInFavoritesRecordId(null);
        }
        int r1=cardInFavoritesRecordMapper.batchInsert(records);
        if(r==0||r1==0){
            throw new RuntimeException("复制失败");
        }


        return copy;
    }
    //批量添加题目到收藏
    @Transactional
    public Favorites addAllCard(Long favoriteId, Long userid, List<Long> cardIds){
        Favorites favorites=favoritesMapper.selectById(favoriteId);

        if(favorites==null){
            throw new RuntimeException("未找到该收藏");
        }
        if(!favorites.getUserid().equals(userid)){
            throw new RuntimeException("不能操作他人收藏");
        }


        List<Long> cardIdList=new ArrayList<>(favorites.getCardIds());

        // 获取已存在的卡片ID
        Set<Long> existingIds = cardInFavoritesRecordMapper.selectList(
                new LambdaQueryWrapper<CardInFavoritesRecord>()
                        .eq(CardInFavoritesRecord::getFavoritesId, favoriteId)
                        .in(CardInFavoritesRecord::getCardId, cardIds)
        ).stream().map(CardInFavoritesRecord::getCardId).collect(Collectors.toSet());

        // 过滤出新卡片
        List<CardInFavoritesRecord> newRecords = cardIds.stream()
                .filter(id -> !existingIds.contains(id))
                .map(id -> new CardInFavoritesRecord(id, favoriteId))
                .collect(Collectors.toList());

        List<Long>newCardId=newRecords.stream().map(CardInFavoritesRecord::getCardId).toList();
        cardIdList.addAll(newCardId);
        favorites.setCardIds(cardIdList);

        int r1= cardInFavoritesRecordMapper.batchInsert(newRecords);
        int r=favoritesMapper.updateById(favorites);
        if(r==0||r1==0){
            throw new RuntimeException("添加失败");
        }
        return favorites;
    }
    //单个查询
    public Favorites getFavoritesById(Long favoritesId,Long userid){
        Favorites favorites=favoritesMapper.selectById(favoritesId);
        if(favorites==null||!favorites.getUserid().equals(userid)){
            throw new RuntimeException("未找到您的该收藏");
        }
        return favorites;
    }
    //查询公开的收藏
    public Favorites getFavoritesById(Long favoritesId){
        Favorites favorites=favoritesMapper.selectById(favoritesId);
        if(favorites==null||favorites.getIsPublic().equals(0)){
            throw new RuntimeException("未找到该公开的收藏");
        }
        return favorites;
    }

}
