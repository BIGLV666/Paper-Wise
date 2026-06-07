package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Mapper.AiGeneratedCardMapper;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.Mapper.FavoritesMapper;
import org.example.paperwise.entry.AiGeneratedCard;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.Favorites;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiGeneratedCardService {
    @Autowired
    private AiGeneratedCardMapper  aiGeneratedCardMapper;
    @Autowired
    private CardMapper cardMapper;
    @Autowired
    private FavoritesMapper favoritesMapper;
    @Autowired
    private PdfService pdfService;


    public Map<String,List<AiGeneratedCard> >getAiGeneratedCardBySessionId( Long userId) {
        QueryWrapper<AiGeneratedCard> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("user_id",userId).eq("status","PENDING").orderByAsc("create_time");
        List<AiGeneratedCard> list=aiGeneratedCardMapper.selectList(queryWrapper);
        return list.stream().collect(Collectors.groupingBy(AiGeneratedCard::getName));

    }
    @Transactional
    public Integer batchCards(List<AiGeneratedCard> aiGeneratedCards,Long userId) {
        System.out.println("userid:"+userId);
        List<Card>cards=new ArrayList<>();
        System.out.println(aiGeneratedCards);
        for (AiGeneratedCard aiGeneratedCard:aiGeneratedCards){
            if(!aiGeneratedCard.getUserId().equals(userId)){continue;}
            cards.add(new Card(aiGeneratedCard));
            aiGeneratedCard.setStatus("HANDLED");
        }
        int r= cardMapper.batchAddCard(cards);
        aiGeneratedCardMapper.batchUpdateStatus(aiGeneratedCards);
        return 1;
    }
    //批量添加卡片到收藏夹
    @Transactional
    public Integer batchCardsToFavorites(List<AiGeneratedCard>aiGeneratedCards,Long favoriteId,Long userId) {
        QueryWrapper<Favorites> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId).eq("favorite_id",favoriteId);
        Favorites favorites=favoritesMapper.selectOne(queryWrapper);
        if(favorites==null){throw new RuntimeException("未找到该收藏夹");}
        List<Card>cards=new ArrayList<>();
        List<Long>cardIds=new ArrayList<>(favorites.getCardIds());
        for (AiGeneratedCard aiGeneratedCard:aiGeneratedCards){
            if(!aiGeneratedCard.getUserId().equals(userId)){continue;}
            cards.add(new Card(aiGeneratedCard));
            aiGeneratedCard.setStatus("HANDLED");
        }
        int r2= cardMapper.batchAddCard(cards);
        for(Card card:cards){
            if(!cardIds.contains(card.getCardId())){
                cardIds.add(card.getCardId());
            }
        }
        favorites.setCardIds(cardIds);
        int r=favoritesMapper.updateById(favorites);
        int r1= aiGeneratedCardMapper.batchUpdateStatus(aiGeneratedCards);
        if(r2==0||0==r||r1==0){
            throw new RuntimeException("添加失败");
        }
        return r2;


    }
    public void generateFromText(Long userId,String tset,String name,String sessionId){
        pdfService.generateFromText(userId, tset, sessionId, name);
    }
    //获取所有历史记录返回列表id和项目名字
    public Map<String, List<Long>> getAllHistoryRecord(Long userId) {
        // 查询所有记录（只查 id 和 name）
        List<AiGeneratedCard> list = aiGeneratedCardMapper.getAllHistoryRecord(userId);
        System.out.println(list);
        // 组装成 Map<String, List<Long>>
        Map<String, List<Long>> result = new LinkedHashMap<>();
        for (AiGeneratedCard card : list) {
            String name = card.getName();
            Long id = card.getAiGeneratedCardId();

            result.computeIfAbsent(name, k -> new ArrayList<>()).add(id);
        }
        System.out.println(result);
        return result;
    }

}
