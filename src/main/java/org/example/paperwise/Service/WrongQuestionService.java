package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Dto.WrongCardDto;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.Mapper.WrongQuestionMapper;
import org.example.paperwise.Mapper.WrongReviewMapper;
import org.example.paperwise.Until.CopyUntil;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.WrongQuestion;
import org.example.paperwise.entry.WrongReview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class WrongQuestionService {
    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;
    @Autowired
    private CopyUntil copyUntil;
    @Autowired
    private CardMapper cardMapper;
    @Autowired
    private WrongReviewMapper  wrongReviewMapper;


    /**
     * 添加卡片到错题本
     * <p>核心逻辑：
     * 1. 检查卡片是否属于当前用户，如不是则复制卡片
     * 2. 检查错题记录是否已存在，不存在则新建，存在则累加错误次数
     * 3. 创建复习记录，使用SM-2算法初始化相关参数</p>
     *
     * @param cardId 卡片ID
     * @param userId 用户ID
     * @param wrongQuestionCategory 错题分类
     * @return 添加的卡片对象
     */
    @Transactional
    public Card addWrongQuestion(Long cardId, Long userId, String wrongQuestionCategory) throws NoSuchMethodException {
        Card card = cardMapper.selectById(cardId);

        // 如果卡片不属于当前用户，则复制卡片到用户名下
        if (card.getUserid() != userId) {
            card = copyUntil.copy(card, Card.class);
            card.setUserid(userId);
            card.setCardId(null);
            cardMapper.insert(card);
        }

        // 查询该卡片是否已在用户错题本中
        QueryWrapper<WrongQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("card_id", card.getCardId()).eq("user_id", userId);
        WrongQuestion wrongQuestion = wrongQuestionMapper.selectOne(queryWrapper);

        // 不存在则新建错题记录，存在则累加错误次数
        if (wrongQuestion == null) {
            wrongQuestion = new WrongQuestion(userId, card.getCardId(), 1, wrongQuestionCategory);
            wrongQuestionMapper.insert(wrongQuestion);
        } else {
            wrongQuestion.setWrongCount(wrongQuestion.getWrongCount() + 1);
            wrongQuestionMapper.updateById(wrongQuestion);
        }

        // 创建复习记录（SM-2算法初始化）
        WrongReview wrongReview = wrongReviewMapper.selectOne(
                new QueryWrapper<WrongReview>().eq("user_id", userId).eq("card_id", card.getCardId()));

        if (wrongReview == null) {
            // 初始化复习记录，下次复习时间设为今天
            wrongReview = new WrongReview(cardId, userId, wrongQuestion.getWrongQuestionId(),
                    LocalDate.now(), LocalDate.now(), 0, 1);
            wrongReview.setEasinessFactor(2.5); // SM-2算法默认难度因子
            wrongReviewMapper.insert(wrongReview);
        }

        return card;
    }
    //获取自己所有的错题
    public List<WrongCardDto> getWrongQuestionsDto(Long userId) {
        return wrongQuestionMapper.getWrongQuestions(userId);
    }

    /**
     * 删除错题
     * @param userId Long
     * @param wrongQuestionId Long
     */
    public void deleteWrongQuestion(Long wrongQuestionId,Long userId) {
        WrongQuestion wrongQuestion = wrongQuestionMapper.selectById(wrongQuestionId);
        System.out.println(wrongQuestion);
        System.out.println(userId);
        if(wrongQuestion==null){
            throw new RuntimeException("未找到该错题");
        }
        if(!wrongQuestion.getUserId().equals(userId)){
            throw new RuntimeException("不能操作他人错题");
        }
        wrongQuestionMapper.deleteById(wrongQuestionId);
    }


}
