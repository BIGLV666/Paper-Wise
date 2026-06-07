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

@Slf4j
@RestController
@RequestMapping("/paperwise/wrongquestion")
public class WrongQuestionController {
    @Autowired
    private WrongQuestionService wrongQuestionService;
    /**
     * 添加到错题集，返回卡片对象
     */
    @PostMapping("/addwrongquestion")
    @Operation(summary = "添加到错题集，返回卡片对象，看情况决定是否渲染，前端传卡片id就行")
    public Result<Card>addWrongQuestion(@RequestParam Long cardId, @RequestAttribute Long userid, @RequestParam String wrongQuestionCategory) throws NoSuchMethodException {
        return Result.success(wrongQuestionService.addWrongQuestion(cardId, userid,wrongQuestionCategory));
    }
    /**
     * 查看自己所有的错题，前端渲染加展开插看卡片详情
     */
    @GetMapping("/getwrongquestiondto")
    @Operation(summary = "查看自己所有的错题，前端渲染加展开插看卡片详情,返回的可能为空")
    public Result<List<WrongCardDto>>getWrongQuestionDto(@RequestAttribute Long userid)  {
        return Result.success(wrongQuestionService.getWrongQuestionsDto(userid));
    }

    /**
     * 删除自己的错题
     * @param userid Long
     * @param wrongQuestionId Long
     * @return success
     */
    @DeleteMapping("/deletewrongquestion")
    @Operation(summary = "删除自己的错题返回成功即可")
    public Result<String> deleteWrongQuestion(@RequestAttribute Long userid,@RequestParam Long wrongQuestionId) {
        wrongQuestionService.deleteWrongQuestion(wrongQuestionId,userid);
        return Result.success("success");
    }
}
