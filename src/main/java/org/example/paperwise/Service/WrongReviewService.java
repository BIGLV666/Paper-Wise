package org.example.paperwise.Service;

import org.example.paperwise.Dto.WrongReviewCardDto;
import org.example.paperwise.Dto.WrongReviewDto;
import org.example.paperwise.Dto.WrongReviewUpdateRequest;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.Mapper.WrongQuestionMapper;
import org.example.paperwise.Mapper.WrongReviewMapper;
import org.example.paperwise.Mapper.WrongReviewStatsMapper;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.WrongReview;
import org.example.paperwise.entry.WrongReviewStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class WrongReviewService {
    @Autowired
    private WrongReviewMapper wrongReviewMapper;
    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;
    @Autowired
    private CardMapper cardMapper;
    @Autowired
    private WrongReviewStatsMapper wrongReviewStatsMapper;

    private final static String WRONG_REVIEW_KEY = "wrong_review_key";
    private final static String WRONG_REVIEW_END_KEY = "wrong_review_end_key";//完成复习的列表
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;


    private int getNextDay(int day){
        switch (day){
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 4;
            case 4:
                return 7;
            case 5:
                return 15;
            case 6:
                return 30;
            case 7:
                return 60;
            case 8:
                return 90;
            case 9:
                return 180;
            case 10:
                return 365;
        }
        return 0;
    }


    //获取当日复习计划
    @Transactional
    public WrongReviewStats getWrongReviewStats(Long userId){
        WrongReviewDto  wrongReviewDto =(WrongReviewDto) redisTemplate.opsForValue().get(WRONG_REVIEW_KEY+userId);
        //WrongReviewDto  wrongReviewDto=new WrongReviewDto();
        if(wrongReviewDto==null){


            List<WrongReviewCardDto> wrongReviewCardDtos = wrongReviewMapper.getAllWrongReviewCardDto(userId);
            wrongReviewDto = new WrongReviewDto();
            wrongReviewDto.setWrongReviewCardDtos(wrongReviewCardDtos);
            wrongReviewDto.setTotal(wrongReviewCardDtos.size());
            redisTemplate.opsForValue().set(WRONG_REVIEW_KEY+userId,wrongReviewDto,30, TimeUnit.MINUTES);
        }
        System.out.println(wrongReviewDto);
        WrongReviewStats wrongReviewStats = wrongReviewStatsMapper.getWrongReviewStatsByDay(userId);
        WrongReviewStats lastRecord = wrongReviewStatsMapper.getLastRecord(userId);
        if(wrongReviewStats == null){
            wrongReviewStats = new WrongReviewStats();
            wrongReviewStats.setTotal(Long.valueOf(wrongReviewDto.getTotal()));
            wrongReviewStats.setDay(LocalDate.now());
            wrongReviewStats.setUserId(userId);
            wrongReviewStats.setRemainCount(Long.valueOf(wrongReviewDto.getTotal()));
            wrongReviewStats.setCompletedCount(0L);
            if(lastRecord==null){
                wrongReviewStats.setTotalReviewCount(0L);
                wrongReviewStats.setContinuousDays(1);
            }else{
                wrongReviewStats.setTotalReviewCount(lastRecord.getTotalReviewCount());
                if(lastRecord.getContinuousDays()==0){
                    wrongReviewStats.setContinuousDays(1);
                }else if(lastRecord.getDay().equals(LocalDate.now().minusDays(1))){
                    wrongReviewStats.setContinuousDays(lastRecord.getContinuousDays()+1);
                }

            }
            wrongReviewStatsMapper.insert(wrongReviewStats);
        }


        return wrongReviewStats;
    }




    /**
     * 初始化复习模块
     * @param userId Long
     * @return
     * WrongReviewDto{
     *   private Integer total;
     *   private List<WrongReviewCardDto> wrongReviewCardDtos;
     * }
     * WrongReviewCardDto{
     *    private Long wrongReviewId;
     *    private Long wrongQuestionId;
     *    private String wrongReviewContent;
     *    private Long cardId;
     *    private String title;
     *    private String questionType;
     *    private List<String> options;
     *    private Object answer;
     *    private String explanation;
     *}
     */
    public WrongReviewDto getWrongReviewDto(Long userId) {
        WrongReviewDto  wrongReviewDto =(WrongReviewDto) redisTemplate.opsForValue().get(WRONG_REVIEW_KEY+userId);
        if(wrongReviewDto!=null){
            return wrongReviewDto;
        }
        List<WrongReviewCardDto> wrongReviewCardDtos = wrongReviewMapper.getAllWrongReviewCardDto(userId);
        wrongReviewDto = new WrongReviewDto();
        wrongReviewDto.setWrongReviewCardDtos(wrongReviewCardDtos);
        wrongReviewDto.setTotal(wrongReviewCardDtos.size());
        redisTemplate.opsForValue().set(WRONG_REVIEW_KEY+userId,wrongReviewDto,30, TimeUnit.MINUTES);

        return wrongReviewDto;
    }

    public Card getCardById(Long cardId) {
        return cardMapper.selectById(cardId);
    }

    //从缓存移出
    private void removeFromRedisCache(Long userId, Long wrongReviewId) {
        String key = WRONG_REVIEW_KEY + userId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof WrongReviewDto) {
            WrongReviewDto dto = (WrongReviewDto) cached;
            List<WrongReviewCardDto> newList = dto.getWrongReviewCardDtos().stream()
                    .filter(card -> !card.getWrongReviewId().equals(wrongReviewId))
                    .collect(Collectors.toList());

            dto.setWrongReviewCardDtos(newList);
            dto.setTotal(newList.size());
            redisTemplate.opsForValue().set(key, dto);
        }
    }
    @Transactional
    public void updateReviewProgress(Long userId, WrongReviewUpdateRequest wrongReviewUpdateRequest) {
        System.out.println(wrongReviewUpdateRequest);
        WrongReview wrongReview = wrongReviewMapper.selectById(wrongReviewUpdateRequest.getWrongReviewId());
        if (wrongReview == null) {
            throw new RuntimeException("未找到该复习卡片");
        }
        if (!Objects.equals(wrongReview.getUserId(), userId)) {
            throw new RuntimeException("请勿操作非本人的题目");
        }
        if (wrongReviewUpdateRequest.getQuality() <= 2) {
            wrongReview.setStage(1);
            wrongReview.setIntervalDays(1);
            wrongReview.setNextReviewTime(LocalDate.now().plusDays(1));
            wrongReview.setLastReviewTime(LocalDate.now());
        } else {
            int d = getNextDay(wrongReview.getStage());
            wrongReview.setStage(wrongReview.getStage() + 1);
            wrongReview.setIntervalDays(d);
            wrongReview.setLastReviewTime(LocalDate.now());
            wrongReview.setNextReviewTime(LocalDate.now().plusDays(d));
        }

        if (wrongReview.getStage() >= 10) {
            wrongReviewMapper.deleteById(wrongReviewUpdateRequest.getWrongReviewId());
            removeFromRedisCache(userId, wrongReviewUpdateRequest.getWrongReviewId());
            wrongQuestionMapper.deleteById(wrongReviewUpdateRequest.getWrongQuestionId());
        } else {
            removeFromRedisCache(userId, wrongReviewUpdateRequest.getWrongReviewId());
            int r= wrongReviewStatsMapper.updateReviewProgress(userId);
            if(r==0){
                throw new RuntimeException();
            }
            wrongReviewMapper.updateById(wrongReview);
        }
    }
}
