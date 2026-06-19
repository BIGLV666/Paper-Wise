/**
 * 收藏夹服务层
 * <p>处理收藏夹的增删改查、分享、复制等业务逻辑</p>
 *
 * @author PaperWise Team
 */
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

/**
 * 收藏夹服务
 * <p>提供收藏夹的CRUD、卡片管理、分享复制、点赞浏览统计等功能</p>
 */
@Service
public class FavoritesService {

    @Autowired
    private FavoritesMapper favoritesMapper;

    @Autowired
    private ShareMapper shareMapper;

    @Autowired
    private CardMapper cardMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CardInFavoritesRecordMapper cardInFavoritesRecordMapper;

    /** 收藏夹浏览量Redis Key */
    private static final String FAVORITES_LOOK_COUNT_KEY = "favorites_look_count";

    /** 收藏夹排行榜Redis Key */
    private static final String RANK_FAVORITES_KEY = "rank_favorites_key";

    /**
     * 获取收藏夹中的所有卡片
     * @param cardIds 卡片ID列表
     * @return 卡片列表
     */
    public List<Card> getAllCard(List<Long> cardIds) {
        return cardMapper.selectList(new QueryWrapper<Card>().in("card_id", cardIds));
    }

    /**
     * 创建收藏夹
     * @param userid 用户ID
     * @param favoritesName 收藏夹名称
     * @return 创建的收藏夹
     */
    @Transactional
    public Favorites createFavorites(Long userid, String favoritesName) {
        Favorites favorites = new Favorites();
        favorites.setUserid(userid);
        favorites.setFavoriteName(favoritesName);
        favorites.setIsPublic(0);
        favorites.setCardIds(new ArrayList<>());
        favorites.setLikeCount(0L);
        favorites.setLookCount(0L);
        favoritesMapper.insert(favorites);
        return favoritesMapper.selectById(favorites.getFavoriteId());
    }

    /**
     * 向收藏夹添加卡片
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     * @param cardId 卡片ID
     * @return 更新后的收藏夹
     */
    @Transactional
    public Favorites addCard(Long favoritesId, Long userid, Long cardId) {
        // 创建收藏记录
        QueryWrapper<CardInFavoritesRecord> q = new QueryWrapper<>();
        q.eq("card_id", cardId);
        CardInFavoritesRecord cq = cardInFavoritesRecordMapper.selectOne(q);
        if (cq == null) {
            CardInFavoritesRecord c = new CardInFavoritesRecord(cardId, favoritesId);
            cardInFavoritesRecordMapper.insert(c);
        }

        Favorites favorites = favoritesMapper.selectOne(new QueryWrapper<Favorites>().eq("favorite_id", favoritesId));
        if (favorites == null) {
            throw new RuntimeException("未找到该收藏夹");
        }
        if (favorites.getCardIds().contains(cardId)) {
            throw new RuntimeException("题目已经存在");
        }
        if (!(Objects.equals(favorites.getUserid(), userid))) {
            throw new RuntimeException("不能修改他人收藏夹");
        }

        List<Long> cardIdList = new ArrayList<>(favorites.getCardIds());
        cardIdList.add(cardId);
        favorites.setCardIds(cardIdList);

        int r = favoritesMapper.updateById(favorites);
        if (r == 0) {
            throw new RuntimeException("添加失败");
        }
        return favorites;
    }

    /**
     * 删除收藏夹
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     */
    @Transactional
    public void deleteFavorites(Long favoritesId, Long userid) {
        Favorites favorites = favoritesMapper.selectById(favoritesId);
        if (favorites == null) {
            throw new RuntimeException("未找到该收藏夹");
        }
        if (!(Objects.equals(favorites.getUserid(), userid))) {
            throw new RuntimeException("不能删除他人的收藏夹");
        }

        redisTemplate.opsForZSet().remove(RANK_FAVORITES_KEY, favorites.getFavoriteId());

        List<Long> cardIdList = favorites.getCardIds();
        QueryWrapper<CardInFavoritesRecord> q = new QueryWrapper<>();
        q.eq("favorites_id", favoritesId).in("card_id", cardIdList);
        cardInFavoritesRecordMapper.delete(q);

        int r = favoritesMapper.deleteById(favoritesId);
        if (r == 0) {
            throw new RuntimeException("删除失败");
        }
    }

    /**
     * 从收藏夹删除卡片
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     * @param cardId 卡片ID
     * @return 更新后的收藏夹
     */
    @Transactional
    public Favorites deleteCard(Long favoritesId, Long userid, Long cardId) {
        Favorites favorites = favoritesMapper.selectById(favoritesId);
        if (favorites == null) {
            throw new RuntimeException("未找到该收藏夹");
        }
        if (!(Objects.equals(favorites.getUserid(), userid))) {
            throw new RuntimeException("不能修改他人的收藏夹");
        }

        boolean f = favorites.getCardIds().remove(cardId);
        if (!f) {
            throw new RuntimeException("未找到该题目");
        }

        List<Long> cardIdList = new ArrayList<>(favorites.getCardIds());
        cardIdList.remove(cardId);
        favorites.setCardIds(cardIdList);

        QueryWrapper<CardInFavoritesRecord> qw = new QueryWrapper<>();
        qw.eq("card_id", cardId);
        qw.eq("favorites_id", favoritesId);
        cardInFavoritesRecordMapper.delete(qw);

        favoritesMapper.updateById(favorites);
        return favorites;
    }

    /**
     * 分页获取用户收藏夹
     * @param userid 用户ID
     * @param size 每页数量
     * @param page 页码
     * @return 分页收藏夹列表
     */
    public Page<Favorites> getAllFavorites(Long userid, int size, int page) {
        if (size < 0 || size > 50) {
            size = 30;
        }
        Page<Favorites> pageList = new Page<>(page, size);
        QueryWrapper<Favorites> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userid);
        return favoritesMapper.selectPage(pageList, queryWrapper);
    }

    /**
     * 修改收藏夹是否公开
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     * @param isPublic 是否公开(0-私有, 1-公开)
     * @return 更新后的收藏夹
     */
    public Favorites updateIsPublic(Long favoritesId, Long userid, Integer isPublic) {
        if (isPublic != 1 && isPublic != 0) {
            throw new RuntimeException("状态不合法");
        }

        Favorites favorites = favoritesMapper.selectById(favoritesId);
        if (favorites == null) {
            throw new RuntimeException("该收藏夹不存在");
        }
        if (!(Objects.equals(favorites.getUserid(), userid))) {
            throw new RuntimeException("不能操作他人收藏夹");
        }

        favorites.setIsPublic(isPublic);
        int r = favoritesMapper.updateById(favorites);
        if (r == 0) {
            throw new RuntimeException("修改失败");
        }

        // 更新排行榜
        if (isPublic == 1) {
            redisTemplate.opsForZSet().remove(RANK_FAVORITES_KEY, favorites.getFavoriteId());
            redisTemplate.opsForZSet().add(RANK_FAVORITES_KEY, favorites.getFavoriteId(), favorites.getLikeCount());
        } else {
            redisTemplate.opsForZSet().remove(RANK_FAVORITES_KEY, favorites.getFavoriteId());
        }

        return favorites;
    }

    /**
     * 生成分享链接
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     * @return 分享链接
     */
    @Transactional
    public String getShareId(Long favoritesId, Long userid) {
        Favorites favorites = favoritesMapper.selectById(favoritesId);
        if (favorites == null) {
            throw new RuntimeException("未找到该收藏夹");
        }
        if (!favorites.getUserid().equals(userid)) {
            throw new RuntimeException("不能操作他人收藏");
        }
        if (favorites.getIsPublic() == 0) {
            throw new RuntimeException("该收藏为不可分享");
        }

        if (favorites.getShareId() != null) {
            Share share = shareMapper.selectById(favorites.getShareId());
            if (share.getExpireTime().isAfter(LocalDateTime.now())) {
                return "http://localhost:8080/paperwise/share/getsharefavorites?shareId=" + share.getUuid();
            }
        }

        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        favorites.setShareId(uuid);

        Share share = new Share(uuid, userid, favorites.getFavoriteName(), favoritesId);
        favoritesMapper.updateById(favorites);
        shareMapper.insert(share);

        return "http://localhost:8080/paperwise/share/getsharefavorites?shareId=" + uuid;
    }

    /**
     * 增加浏览量
     * @param favoritesId 收藏夹ID
     */
    public void upLookCount(Long favoritesId) {
        redisTemplate.opsForValue().increment(FAVORITES_LOOK_COUNT_KEY + "--" + "favoritesId" + "--" + favoritesId, 1);
    }

    /**
     * 复制他人公开收藏夹
     * @param favoritesId 被复制的收藏夹ID
     * @param userid 当前用户ID
     * @return 复制后的收藏夹
     */
    @Transactional
    public Favorites CopyFavorites(Long favoritesId, Long userid) {
        Favorites favorites = favoritesMapper.selectById(favoritesId);
        if (favorites == null) {
            throw new RuntimeException("未找到该收藏夹");
        }
        if (favorites.getIsPublic() == 0) {
            throw new RuntimeException("该收藏夹是私有的");
        }

        List<CardInFavoritesRecord> records = cardInFavoritesRecordMapper.selectList(
                new QueryWrapper<CardInFavoritesRecord>().eq("favorites_id", favoritesId));

        Favorites copy = new Favorites();
        copy.setUserid(userid);
        copy.setFavoriteName(favorites.getFavoriteName());
        copy.setCardIds(favorites.getCardIds());
        copy.setLikeCount(0L);
        copy.setLookCount(0L);
        copy.setIsPublic(0);
        copy.setCreateTime(LocalDate.now());
        favoritesMapper.insert(copy);

        for (CardInFavoritesRecord record : records) {
            record.setFavoritesId(copy.getFavoriteId());
            record.setCardInFavoritesRecordId(null);
        }
        cardInFavoritesRecordMapper.batchInsert(records);

        return copy;
    }

    /**
     * 批量添加卡片到收藏夹
     * @param favoriteId 收藏夹ID
     * @param userid 用户ID
     * @param cardIds 卡片ID列表
     * @return 更新后的收藏夹
     */
    @Transactional
    public Favorites addAllCard(Long favoriteId, Long userid, List<Long> cardIds) {
        Favorites favorites = favoritesMapper.selectById(favoriteId);
        if (favorites == null) {
            throw new RuntimeException("未找到该收藏");
        }
        if (!favorites.getUserid().equals(userid)) {
            throw new RuntimeException("不能操作他人收藏");
        }

        List<Long> cardIdList = new ArrayList<>(favorites.getCardIds());

        Set<Long> existingIds = cardInFavoritesRecordMapper.selectList(
                new LambdaQueryWrapper<CardInFavoritesRecord>()
                        .eq(CardInFavoritesRecord::getFavoritesId, favoriteId)
                        .in(CardInFavoritesRecord::getCardId, cardIds)
        ).stream().map(CardInFavoritesRecord::getCardId).collect(Collectors.toSet());

        List<CardInFavoritesRecord> newRecords = cardIds.stream()
                .filter(id -> !existingIds.contains(id))
                .map(id -> new CardInFavoritesRecord(id, favoriteId))
                .collect(Collectors.toList());

        List<Long> newCardId = newRecords.stream().map(CardInFavoritesRecord::getCardId).toList();
        cardIdList.addAll(newCardId);
        favorites.setCardIds(cardIdList);

        cardInFavoritesRecordMapper.batchInsert(newRecords);
        favoritesMapper.updateById(favorites);

        return favorites;
    }

    /**
     * 获取用户自己的收藏夹
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     * @return 收藏夹
     */
    public Favorites getFavoritesById(Long favoritesId, Long userid) {
        Favorites favorites = favoritesMapper.selectById(favoritesId);
        if (favorites == null || !favorites.getUserid().equals(userid)) {
            throw new RuntimeException("未找到您的该收藏");
        }
        return favorites;
    }

    /**
     * 获取公开收藏夹
     * @param favoritesId 收藏夹ID
     * @return 收藏夹
     */
    public Favorites getFavoritesById(Long favoritesId) {
        Favorites favorites = favoritesMapper.selectById(favoritesId);
        if (favorites == null || favorites.getIsPublic().equals(0)) {
            throw new RuntimeException("未找到该公开的收藏");
        }
        return favorites;
    }
}
