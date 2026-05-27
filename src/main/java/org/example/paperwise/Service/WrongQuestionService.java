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
import org.example.paperwise.enums.WrongQuestionCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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


    //添加错题
    @Transactional
    public Card addWrongQuestion(Long cardId,Long userId,String wrongQuestionCategory) throws NoSuchMethodException {
        Card card = cardMapper.selectById(cardId);
        if(!Objects.equals(card.getUserid(), userId)){
            card=copyUntil.copy(card, Card.class);
        }

        card.setUserid(userId);
        card.setCardId(null);
        cardMapper.insert(card);
        QueryWrapper<WrongQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("card_id",card.getCardId()).eq("user_id",userId);
        WrongQuestion wrongQuestion= wrongQuestionMapper.selectOne(queryWrapper);
        if(wrongQuestion==null){
            wrongQuestion=new WrongQuestion(userId,card.getCardId(),0,wrongQuestionCategory, WrongQuestionCategory.PENDING_REVIEW);
        }else{
            wrongQuestion.setWrongCount(wrongQuestion.getWrongCount()+1);
            wrongQuestionMapper.updateById(wrongQuestion);
        }
        wrongQuestionMapper.insert(wrongQuestion);



        //生成复习记录
        WrongReview wrongReview=wrongReviewMapper.selectOne(new QueryWrapper<WrongReview>().eq("user_id",userId).eq("card_id",card.getCardId()));
        if(wrongReview==null){
         wrongReview=new WrongReview(cardId,userId,wrongQuestion.getWrongQuestionId(), LocalDateTime.now().plusDays(1),LocalDateTime.now(),"未掌握",1,1);
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
        if(wrongQuestion==null){
            throw new RuntimeException("未找到该错题");
        }
        if(!wrongQuestion.getUserId().equals(userId)){
            throw new RuntimeException("不能操作他人错题");
        }
        wrongQuestionMapper.deleteById(wrongQuestionId);
    }


}
