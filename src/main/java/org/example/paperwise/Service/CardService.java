/**
 * 卡片服务层
 * <p>处理卡片的增删改查、收藏夹关联等业务逻辑</p>
 *
 * @author PaperWise Team
 */
package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Mapper.CardInFavoritesRecordMapper;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.Mapper.FavoritesMapper;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.CardInFavoritesRecord;
import org.example.paperwise.entry.Favorites;
import org.example.paperwise.enums.CardMastery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 卡片服务
 * <p>提供卡片的CRUD、批量操作、收藏夹关联等功能</p>
 */
@Slf4j
@Service
public class CardService {

    @Autowired
    private CardMapper cardMapper;

    @Autowired
    private FavoritesMapper favoritesMapper;

    @Autowired
    private CardInFavoritesRecordMapper cardInFavoritesRecordMapper;

    /**
     * 根据ID获取卡片
     * @param card_id 卡片ID
     * @return 卡片对象
     */
    public Card getCardById(Long card_id) {
        return cardMapper.selectById(card_id);
    }

    /**
     * 添加卡片
     * @param card 卡片对象
     * @param userid 用户ID
     * @return 创建的卡片
     */
    public Card addCard(Card card, Long userid) {
        card.setUserid(userid);

        // 验证必填字段
        if (card.getTitle() == null || card.getAnswer() == null || card.getCardType() == null
                || card.getCardDifficulty() == null || card.getQuestionType() == null) {
            throw new RuntimeException("请完善卡片信息");
        }

        // 默认掌握状态
        if (card.getCardMastery() == null) {
            card.setCardMastery(CardMastery.NOT_STARTED);
        }

        int r = cardMapper.insert(card);
        if (r == 0) {
            throw new RuntimeException("添加卡片失败");
        }
        return card;
    }

    /**
     * 更新卡片
     * @param card 卡片对象
     * @param userid 用户ID
     * @return 更新后的卡片
     */
    public Card updateCard(Card card, Long userid) {
        Card existing = cardMapper.selectById(card.getCardId());
        if (existing == null) {
            throw new RuntimeException("未找到该卡片");
        }
        if (!Objects.equals(existing.getUserid(), userid)) {
            throw new RuntimeException("不能修改他人卡片");
        }

        int r = cardMapper.updateById(card);
        if (r == 0) {
            throw new RuntimeException("更新失败");
        }
        return card;
    }

    /**
     * 删除卡片（同时处理收藏夹关联）
     * @param card_id 卡片ID
     * @param userid 用户ID
     * @return 是否成功
     */
    @Transactional
    public boolean deleteCard(Long card_id, Long userid) {
        Card card = cardMapper.selectById(card_id);
        if (card == null) {
            throw new RuntimeException("未找到卡片");
        }
        if (!Objects.equals(card.getUserid(), userid)) {
            throw new RuntimeException("不能操作他人卡片");
        }

        // 查找关联的收藏夹记录
        QueryWrapper<CardInFavoritesRecord> qw = new QueryWrapper<>();
        List<CardInFavoritesRecord> records = cardInFavoritesRecordMapper.selectList(qw.eq("card_id", card_id));

        if (records != null) {
            // 更新收藏夹中的卡片列表
            List<Long> favoritesIds = records.stream()
                    .map(CardInFavoritesRecord::getFavoritesId)
                    .collect(Collectors.toList());

            List<Favorites> favorites = favoritesMapper.selectBatchIds(favoritesIds);
            for (Favorites f : favorites) {
                List<Long> cardIds = new ArrayList<>(f.getCardIds());
                cardIds.remove(card_id);
                f.setCardIds(cardIds);
                favoritesMapper.updateById(f);
            }

            // 删除收藏夹关联记录
            cardInFavoritesRecordMapper.deleteBatchIds(records.stream()
                    .map(CardInFavoritesRecord::getCardInFavoritesRecordId)
                    .collect(Collectors.toList()));
        }

        int r = cardMapper.deleteById(card_id);
        return r == 1;
    }

    /**
     * 获取所有卡片类型及数量统计
     * @param userid 用户ID
     * @return 类型统计列表
     */
    public List<Map<String, Integer>> getAllQuestionType(Long userid) {
        return cardMapper.getAllQuestionType(userid);
    }

    /**
     * 按类型分页查询卡片
     * @param question_type 题目类型
     * @param size 每页数量（最大30）
     * @param page 页码
     * @param userid 用户ID
     * @return 分页卡片列表
     */
    public Page<Card> getByQuestionType(String question_type, int size, int page, Long userid) {
        if (size > 30) {
            size = 30;
        }
        Page<Card> pageCard = new Page<>(page, size);
        LambdaQueryWrapper<Card> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Card::getUserid, userid)
                .eq(Card::getQuestionType, question_type)
                .orderByDesc(Card::getCreateTime);
        return cardMapper.selectPage(pageCard, queryWrapper);
    }

    /**
     * 批量添加卡片
     * @param cards 卡片列表
     * @param userid 用户ID
     * @return 添加数量
     */
    public int addAllCard(List<Card> cards, Long userid) {
        for (Card card : cards) {
            card.setUserid(userid);
        }
        return cardMapper.batchAddCard(cards);
    }

    /**
     * 分页获取用户所有卡片
     * @param userid 用户ID
     * @param size 每页数量（最大30）
     * @param page 页码
     * @return 分页卡片列表
     */
    public Page<Card> getAllCardsForPage(Long userid, int size, int page) {
        if (size > 30) {
            size = 30;
        }
        Page<Card> pageCard = new Page<>(page, size);
        LambdaQueryWrapper<Card> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Card::getUserid, userid)
                .orderByDesc(Card::getCreateTime);

        return cardMapper.selectPage(pageCard, queryWrapper);
    }

    /**
     * 根据卡片ID列表批量获取卡片
     * @param cardIds 卡片ID列表
     * @return 卡片列表
     */
    public List<Card> getAllCardsForCardIds(List<Long> cardIds) {
        return cardMapper.selectBatchIds(cardIds);
    }

    /**
     * 根据ID获取卡片
     * @param card_id 卡片ID
     * @return 卡片对象
     */
    public Card getCardByCardId(Long card_id) {
        return cardMapper.selectById(card_id);
    }
}
