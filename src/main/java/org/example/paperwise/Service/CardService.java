package org.example.paperwise.Service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.entry.Card;
import org.example.paperwise.enums.CardMastery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CardService {
    @Autowired
    private CardMapper cardMapper;
    public Card getCardById(Long card_id,Long userid) {
        return cardMapper.getCardById(card_id,userid);
    }
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
        if(!Objects.equals(card.getUserid(), userid)){
            throw new RuntimeException("cannot update card");
        }
        int r=cardMapper.updateById(card);
        if(r==0){
            throw new RuntimeException("update card error");
        }
        return card;
    }
    public boolean deleteCard(Long card_id,Long userid) {
        Card card=cardMapper.getCardById(card_id,userid);
        if(card==null){
            throw new RuntimeException("not found your card");
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
    //

}
