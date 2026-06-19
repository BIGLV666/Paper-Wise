/**
 * 错题本控制器
 * <p>提供错题添加、查询、删除等功能</p>
 *
 * @author PaperWise Team
 */
package org.example.paperwise.Controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Dto.WrongCardDto;
import org.example.paperwise.Service.WrongQuestionService;
import org.example.paperwise.entry.Card;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 错题本控制器
 * <p>处理错题的添加、查询和删除</p>
 */
@Slf4j
@RestController
@RequestMapping("/paperwise/wrongquestion")
public class WrongQuestionController {

    @Autowired
    private WrongQuestionService wrongQuestionService;

    /**
     * 添加卡片到错题集
     * @param cardId 卡片ID
     * @param userid 用户ID
     * @param wrongQuestionCategory 错题分类
     * @return 卡片对象
     */
    @PostMapping("/addwrongquestion")
    @Operation(summary = "添加到错题集，返回卡片对象")
    
    public Result<Card> addWrongQuestion(@RequestParam Long cardId, @RequestAttribute Long userid, @RequestParam String wrongQuestionCategory) {
        try {
            return Result.success(wrongQuestionService.addWrongQuestion(cardId, userid, wrongQuestionCategory));
        } catch (NoSuchMethodException e) {
            log.error("添加错题失败，方法不存在: {}", e.getMessage());
            return Result.error("添加错题失败，服务异常");
        }
    }

    /**
     * 获取用户所有错题
     * @param userid 用户ID
     * @return 错题列表
     */
    @GetMapping("/getwrongquestiondto")
    @Operation(summary = "查看自己所有的错题")
    public Result<List<WrongCardDto>> getWrongQuestionDto(@RequestAttribute Long userid) {
        return Result.success(wrongQuestionService.getWrongQuestionsDto(userid));
    }

    /**
     * 删除错题
     * @param userid 用户ID
     * @param wrongQuestionId 错题记录ID
     * @return 操作结果
     */
    @DeleteMapping("/deletewrongquestion")
    @Operation(summary = "删除自己的错题")
    public Result<String> deleteWrongQuestion(@RequestAttribute Long userid, @RequestParam Long wrongQuestionId) {
        wrongQuestionService.deleteWrongQuestion(wrongQuestionId, userid);
        return Result.success("删除成功");
    }
}
