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


    /** SM-2 默认难易系数 EF */
    private static final double SM2_DEFAULT_EF = 2.5;
    /** SM-2 难易系数下限 */
    private static final double SM2_MIN_EF = 1.3;
    /** 连续成功复习达到此次数视为掌握，移出错题本 */
    private static final int SM2_GRADUATION_REPS = 10;

    /**
     * 按 SM-2 (SuperMemo 2) 间隔重复算法更新单张卡片的复习参数。
     * <p>
     * 参数含义：
     * <ul>
     *   <li>n (stage) — 连续成功复习次数</li>
     *   <li>I (intervalDays) — 下次复习间隔（天）</li>
     *   <li>EF (easinessFactor) — 难易系数，初始 2.5，最小 1.3</li>
     *   <li>q (quality) — 回忆质量 0~5，q &lt; 3 视为失败</li>
     * </ul>
     * 算法步骤：
     * <ol>
     *   <li>EF' = EF + (0.1 - (5-q) × (0.08 + (5-q) × 0.02))，若 EF' &lt; 1.3 则取 1.3</li>
     *   <li>若 q &lt; 3：n = 0，I = 1</li>
     *   <li>若 q ≥ 3：n = n + 1；n = 1 时 I = 1，n = 2 时 I = 6，n &gt; 2 时 I = round(I × EF')</li>
     * </ol>
     */
    private void applySm2Algorithm(WrongReview wrongReview, int quality) {
        int q = Math.max(0, Math.min(5, quality));

        double ef = wrongReview.getEasinessFactor() != null ? wrongReview.getEasinessFactor() : SM2_DEFAULT_EF;
        // 步骤 1：根据本次回忆质量 q 更新难易系数 EF
        ef = ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        if (ef < SM2_MIN_EF) {
            ef = SM2_MIN_EF;
        }

        int n = wrongReview.getStage() != null ? wrongReview.getStage() : 0;
        // 兼容旧数据：stage=1 且 intervalDays=1 表示尚未完成首次成功复习，等价于 n=0
        if (n == 1 && Integer.valueOf(1).equals(wrongReview.getIntervalDays())) {
            n = 0;
        }

        int interval;
        if (q < 3) {
            // 步骤 2：回忆失败，重置连续成功次数，次日再复习
            n = 0;
            interval = 1;
        } else {
            // 步骤 3：回忆成功，递增 n 并按 SM-2 规则计算新间隔
            if (n == 0) {
                interval = 1;
            } else if (n == 1) {
                interval = 6;
            } else {
                interval = (int) Math.round(wrongReview.getIntervalDays() * ef);
                if (interval < 1) {
                    interval = 1;
                }
            }
            n = n + 1;
        }

        wrongReview.setStage(n);
        wrongReview.setIntervalDays(interval);
        wrongReview.setEasinessFactor(ef);
        wrongReview.setLastReviewTime(LocalDate.now());
        wrongReview.setNextReviewTime(LocalDate.now().plusDays(interval));
    }

    /**
     * 获取当日复习统计数据
     * <p>核心逻辑：
     * 1. 先从Redis缓存获取复习列表，缓存30分钟
     * 2. 查询今日复习统计记录是否存在
     * 3. 不存在则新建，需计算：总复习数、剩余数、连续学习天数
     * 4. 连续天数计算：昨天有记录则+1，否则从1开始</p>
     *
     * @param userId 用户ID
     * @return 当日复习统计数据
     */
    @Transactional
    public WrongReviewStats getWrongReviewStats(Long userId) {
        WrongReviewDto wrongReviewDto = (WrongReviewDto) redisTemplate.opsForValue()
                .get(WRONG_REVIEW_KEY + "--" + "userId" + userId);

        // 缓存不存在则查询数据库并缓存
        if (wrongReviewDto == null) {
            List<WrongReviewCardDto> wrongReviewCardDtos = wrongReviewMapper.getAllWrongReviewCardDto(userId);
            wrongReviewDto = new WrongReviewDto();
            wrongReviewDto.setWrongReviewCardDtos(wrongReviewCardDtos);
            wrongReviewDto.setTotal(wrongReviewCardDtos.size());
            redisTemplate.opsForValue().set(WRONG_REVIEW_KEY + "--" + "userId" + userId,
                    wrongReviewDto, 30, TimeUnit.MINUTES);
        }

        WrongReviewStats wrongReviewStats = wrongReviewStatsMapper.getWrongReviewStatsByDay(userId);
        WrongReviewStats lastRecord = wrongReviewStatsMapper.getLastRecord(userId);

        // 当日统计记录不存在则新建
        if (wrongReviewStats == null) {
            wrongReviewStats = new WrongReviewStats();
            wrongReviewStats.setTotal(Long.valueOf(wrongReviewDto.getTotal()));
            wrongReviewStats.setDay(LocalDate.now());
            wrongReviewStats.setUserId(userId);
            wrongReviewStats.setRemainCount(Long.valueOf(wrongReviewDto.getTotal()));
            wrongReviewStats.setCompletedCount(0L);

            // 计算连续学习天数
            if (lastRecord == null) {
                wrongReviewStats.setTotalReviewCount(0L);
                wrongReviewStats.setContinuousDays(1);
            } else {
                wrongReviewStats.setTotalReviewCount(lastRecord.getTotalReviewCount());
                if (lastRecord.getContinuousDays() == 0) {
                    wrongReviewStats.setContinuousDays(1);
                } else if (lastRecord.getDay().equals(LocalDate.now().minusDays(1))) {
                    // 昨天有学习，连续天数+1
                    wrongReviewStats.setContinuousDays(lastRecord.getContinuousDays() + 1);
                } else {
                    // 中间断开，连续天数重置为1
                    wrongReviewStats.setContinuousDays(1);
                }
            }
            wrongReviewStatsMapper.insert(wrongReviewStats);
        }

        return wrongReviewStats;
    }

    /**
     * 初始化复习模块数据
     * <p>从Redis缓存获取复习卡片列表，缓存有效期30分钟</p>
     *
     * @param userId 用户ID
     * @return 复习模块数据（包含卡片列表和总数）
     */
    public WrongReviewDto getWrongReviewDto(Long userId) {
        WrongReviewDto wrongReviewDto = (WrongReviewDto) redisTemplate.opsForValue()
                .get(WRONG_REVIEW_KEY + "--" + "userId" + userId);

        if (wrongReviewDto != null) {
            return wrongReviewDto;
        }

        List<WrongReviewCardDto> wrongReviewCardDtos = wrongReviewMapper.getAllWrongReviewCardDto(userId);
        wrongReviewDto = new WrongReviewDto();
        wrongReviewDto.setWrongReviewCardDtos(wrongReviewCardDtos);
        wrongReviewDto.setTotal(wrongReviewCardDtos.size());

        redisTemplate.opsForValue().set(WRONG_REVIEW_KEY + "--" + "userId" + userId,
                wrongReviewDto, 30, TimeUnit.MINUTES);

        return wrongReviewDto;
    }

    public Card getCardById(Long cardId) {
        return cardMapper.selectById(cardId);
    }

    /**
     * 从Redis缓存中移除已完成的复习卡片
     *
     * @param userId 用户ID
     * @param wrongReviewId 复习记录ID
     */
    private void removeFromRedisCache(Long userId, Long wrongReviewId) {
        String key = WRONG_REVIEW_KEY + "--" + "userId" + userId;
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

    /**
     * 更新复习进度
     * <p>核心逻辑：
     * 1. 验证复习记录归属权
     * 2. 应用SM-2算法计算下次复习参数
     * 3. 判断是否达到掌握标准（连续成功10次），是则移出错题本
     * 4. 更新复习统计数据</p>
     *
     * @param userId 用户ID
     * @param wrongReviewUpdateRequest 复习反馈请求（包含复习记录ID和回忆质量0-5）
     */
    @Transactional
    public void updateReviewProgress(Long userId, WrongReviewUpdateRequest wrongReviewUpdateRequest) {
        WrongReview wrongReview = wrongReviewMapper.selectById(wrongReviewUpdateRequest.getWrongReviewId());
        if (wrongReview == null) {
            throw new RuntimeException("未找到该复习卡片");
        }
        if (!Objects.equals(wrongReview.getUserId(), userId)) {
            throw new RuntimeException("请勿操作非本人的题目");
        }

        // 应用SM-2算法更新复习参数
        applySm2Algorithm(wrongReview, wrongReviewUpdateRequest.getQuality());

        // 判断是否达到掌握标准（连续成功复习10次），达到则移出错题本
        if (wrongReview.getStage() >= SM2_GRADUATION_REPS) {
            wrongReviewMapper.deleteById(wrongReviewUpdateRequest.getWrongReviewId());
            removeFromRedisCache(userId, wrongReviewUpdateRequest.getWrongReviewId());
            wrongQuestionMapper.deleteById(wrongReviewUpdateRequest.getWrongQuestionId());
        } else {
            removeFromRedisCache(userId, wrongReviewUpdateRequest.getWrongReviewId());
            int r = wrongReviewStatsMapper.updateReviewProgress(userId);
            if (r == 0) {
                throw new RuntimeException("更新复习进度失败");
            }
            wrongReviewMapper.updateById(wrongReview);
        }
    }
}
