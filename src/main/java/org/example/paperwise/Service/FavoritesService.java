package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.Mapper.FavoritesMapper;
import org.example.paperwise.Mapper.ShareMapper;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.Favorites;
import org.example.paperwise.entry.Share;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    private static final String FAVORITES_LOOK_COUNT_KEY = "favorites_look_count";


    //返回用户收藏夹内容
    public List<Card>getAllCard(List<Long>cardIds){
        return cardMapper.selectList(new QueryWrapper<Card>().in("card_id",cardIds));
    }

    //初始化收藏夹
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
    public Favorites addCard(Long favoritesId,Long userid,Long cardId) {
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
    public void deleteFavorites(Long favoritesId, Long userid) {
        Favorites favorites=favoritesMapper.selectById(favoritesId);
        if(favorites==null){
            throw new RuntimeException("未找到该收藏夹");
        }
        if(!(Objects.equals(favorites.getUserid(), userid))){
            throw new RuntimeException("不能删除他人的收藏夹");
        }
        int r=favoritesMapper.deleteById(favoritesId);
        if(r==0){
            throw new RuntimeException("删除失败");
        }

    }
    //删除卡片
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

        redisTemplate.opsForValue().increment(FAVORITES_LOOK_COUNT_KEY+favoritesId,1);
    }

    //复制他人收藏夹
    public Favorites CopyFavorites(Long favoritesId,Long userid){
        Favorites favorites=favoritesMapper.selectById(favoritesId);
        if(favorites==null){
            throw new RuntimeException("未找到该收藏夹");
        }
        if(favorites.getIsPublic()==0){
            throw new RuntimeException("该收藏夹是私有的");
        }
        Favorites copy=new Favorites();
        copy.setUserid(userid);
        copy.setFavoriteName(favorites.getFavoriteName());
        copy.setCardIds(favorites.getCardIds());
        copy.setLikeCount(0L);
        copy.setLookCount(0L);
        copy.setIsPublic(0);
        copy.setCreateTime(LocalDate.now());
        int r=favoritesMapper.insert(copy);
        if(r==0){
            throw new RuntimeException("复制失败");
        }
        return copy;
    }
    //批量添加题目到收藏
    public Favorites addAllCard(Long favoriteId, Long userid, List<Long> cardIds){
        Favorites favorites=favoritesMapper.selectById(favoriteId);
        if(favorites==null){
            throw new RuntimeException("未找到该收藏");
        }
        if(!favorites.getUserid().equals(userid)){
            throw new RuntimeException("不能操作他人收藏");
        }
        List<Long> cardIdList=new ArrayList<>(favorites.getCardIds());
        for(Long cardId:cardIds){
            if(!cardIdList.contains(cardId)){
                cardIdList.add(cardId);
            }
        }
        favorites.setCardIds(cardIdList);
        int r=favoritesMapper.updateById(favorites);
        if(r==0){
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
