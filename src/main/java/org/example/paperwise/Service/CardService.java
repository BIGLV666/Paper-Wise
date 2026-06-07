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

@Slf4j
@Service
public class CardService {
    @Autowired
    private CardMapper cardMapper;
    @Autowired
    private FavoritesMapper favoritesMapper;
    public Card getCardById(Long card_id) {
        return cardMapper.selectById(card_id);
    }
    @Autowired
    private CardInFavoritesRecordMapper cardInFavoritesRecordMapper;




    public Card addCard(Card card,Long userid) {
        card.setUserid(userid);
        if(card.getTitle()==null||card.getAnswer()==null||card.getCardType()==null||card.getCardDifficulty()==null||card.getQuestionType()==null){
            throw new RuntimeException("请完善卡片信息");
        }
        if(card.getCardMastery()==null){
            card.setCardMastery(CardMastery.NOT_STARTED);
        }
        int r=cardMapper.insert(card);
        if(r==0){
            throw new RuntimeException("addCard error");
        }
        return card;
    }
    public Card updateCard(Card card,Long userid) {
        Card existing=cardMapper.selectById(card.getCardId());
        if(existing==null){
            throw new RuntimeException("未找到该卡片");
        }
        if(!Objects.equals(existing.getUserid(), userid)){
            throw new RuntimeException("不能修改他人卡片");
        }
        int r=cardMapper.updateById(card);
        if(r==0){
            throw new RuntimeException("更新失败");
        }
        return card;
    }
    @Transactional
    public boolean deleteCard(Long card_id,Long userid) {

        Card card=cardMapper.selectById(card_id);
        if(card==null){
            throw new RuntimeException("not found your card");
        }
        if(!Objects.equals(card.getUserid(), userid)){
            throw new RuntimeException("不能操作他人卡片");
        }


        QueryWrapper<CardInFavoritesRecord> qw=new QueryWrapper<>();
        List<CardInFavoritesRecord> records=  cardInFavoritesRecordMapper.selectList(qw.eq("card_id",card_id));
        if(records!=null){
            List<Long> favoritesIds = records.stream()
                    .map(CardInFavoritesRecord::getFavoritesId)
                    .collect(Collectors.toList());

            List<Favorites>favorites=favoritesMapper.selectBatchIds(favoritesIds);
            for(Favorites f:favorites){
                List<Long>cardIds=new ArrayList<>(f.getCardIds());
                cardIds.remove(card_id);
                f.setCardIds(cardIds);
                favoritesMapper.updateById(f);
            }
        }
        if (records != null) {
            cardInFavoritesRecordMapper.deleteBatchIds(records.stream()
                    .map(CardInFavoritesRecord::getCardInFavoritesRecordId)
                    .collect(Collectors.toList()));
        }

        int r=cardMapper.deleteById(card_id);


        return r==1;
    }
    public List<Map<String,Integer>> getAllQuestionType(Long userid) {
        return cardMapper.getAllQuestionType(userid);
    }

    //按照类别分页
    public Page<Card> getByQuestionType(String question_type,int size,int page,Long userid) {
        if(size>30){
            size=30;
        }
        Page<Card> pageCard=new Page<>(page,size);
        LambdaQueryWrapper<Card> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(Card::getUserid,userid)
                .eq(Card::getQuestionType,question_type)
                .orderByDesc(Card::getCreateTime);
        return cardMapper.selectPage(pageCard,queryWrapper);

    }

    //批量添加
    public int addAllCard(List<Card> cards,Long userid) {
        for(Card card:cards){
            card.setUserid(userid);
        }
        return cardMapper.batchAddCard(cards);
    }
    //查看自己所有的卡片
    public Page<Card> getAllCardsForPage(Long userid, int size, int page) {
        if(size>30){
            size=30;
        }
        Page<Card> pageCard=new Page<>(page,size);
        LambdaQueryWrapper<Card> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(Card::getUserid,userid)
                .orderByDesc(Card::getCreateTime);

        return cardMapper.selectPage(pageCard,queryWrapper);
    }
    //返回卡片列表
    public List<Card> getAllCardsForCardIds(List<Long>cardIds) {
        return cardMapper.selectBatchIds(cardIds);
    }
    //获取单个卡片
    public Card getCardByCardId(Long card_id) {
        return cardMapper.selectById(card_id);
    }


}
