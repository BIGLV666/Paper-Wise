package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Mapper.AiGeneratedCardMapper;
import org.example.paperwise.Mapper.CardInFavoritesRecordMapper;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.Mapper.FavoritesMapper;
import org.example.paperwise.Until.BuildPromptUntil;
import org.example.paperwise.entry.AiGeneratedCard;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.CardInFavoritesRecord;
import org.example.paperwise.entry.Favorites;
import org.example.paperwise.enums.CardDifficulty;
import org.example.paperwise.enums.CardMastery;
import org.example.paperwise.enums.CardType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private CardInFavoritesRecordMapper cardInFavoritesRecordMapper;
    @Autowired
    private QianwenService  qianwenService;
    private final ObjectMapper objectMapper = new ObjectMapper();


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

        List<CardInFavoritesRecord> records=new ArrayList<>();
        for(Long cardId:cardIds){
            records.add(new CardInFavoritesRecord(cardId,favoriteId));
        }
        int r3=cardInFavoritesRecordMapper.batchInsert(records);

        int r1= aiGeneratedCardMapper.batchUpdateStatus(aiGeneratedCards);
        if(r2==0||0==r||r1==0||r3==0){
            throw new RuntimeException("添加失败");
        }
        return r2;


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

















    //ai提取









    public List<AiGeneratedCard> parseCards(String aiResponse, Long userId,String sessionId,String name) {
        try {
            String json = extractJson(aiResponse);

            System.out.println("解析的 JSON: " + json);
            log.info("解析的 JSON: {}", json);
            List<AiGeneratedCard> cards = objectMapper.readValue(json, new TypeReference<List<AiGeneratedCard>>() {});

            for (AiGeneratedCard card : cards) {
                if (card.getUserId() == null) {
                    card.setUserId(userId);
                }
                if (card.getCreateTime() == null) {
                    card.setCreateTime(LocalDateTime.now());
                }
                // 设置默认值（如果 AI 没返回）
                if (card.getCardType() == null) {
                    card.setCardType(CardType.ESSAY);
                }
                if (card.getCardDifficulty() == null) {
                    card.setCardDifficulty(CardDifficulty.MEDIUM);
                }
                if (card.getCardMastery() == null) {
                    card.setCardMastery(CardMastery.NOT_STARTED);
                }
                if (card.getAnswer() instanceof List) {
                    List<?> list = (List<?>) card.getAnswer();
                    card.setAnswer(list.stream().map(String::valueOf).collect(Collectors.joining("、")));
                }
                card.setSessionId(sessionId);
                card.setName(name);
                card.setStatus("PENDING");
            }
            return cards;

        } catch (Exception e) {

            log.error("卡片解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractJson(String jsonText) {
        int start = jsonText.indexOf("[");
        int end = jsonText.lastIndexOf("]");
        if (start != -1 && end != -1 && end > start) {
            return jsonText.substring(start, end + 1);
        }
        throw new RuntimeException("AI 响应中没有找到 JSON 数组");
    }






    @Async
    public void asyncProcessPdf(Long userId,  String sessionId,Object[]val) {
        try {
            // 使用 byte[] 处理 PDF

            String text = (String) val[0];
            String name = (String) val[1];

            log.info("用户ID为{}, PDF 文本长度: {}", userId, text.length());

            String prompt = BuildPromptUntil.buildPrompt(text);
            String aiResponse = qianwenService.chat(prompt);
            log.info("AI 响应长度: {}", aiResponse.length());

            List<AiGeneratedCard> cards = parseCards(aiResponse, userId, sessionId, name);
            if (cards != null && !cards.isEmpty()) {
                aiGeneratedCardMapper.batchInsert(cards);
                log.info("成功生成 {} 张卡片", cards.size());
            }
        } catch (Exception e) {
            log.error("PDF 处理失败", e);
        }
    }



    //文本提取
    @Async
    public void generateFromText(Long userId, String text,String sessionId,String name) {
        String prompt =  BuildPromptUntil.buildPrompt(text);
        String aiResponse = qianwenService.chat(prompt);
        log.info("AI 响应长度: {}", aiResponse.length());

        List<AiGeneratedCard> cards = parseCards(aiResponse, userId,sessionId,name);
        if (cards != null && !cards.isEmpty()) {
            aiGeneratedCardMapper.batchInsert(cards);
            log.info("成功生成 {} 张卡片", cards.size());
        }
    }




}
