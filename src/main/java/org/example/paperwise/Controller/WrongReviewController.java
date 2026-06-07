package org.example.paperwise.Controller;

import io.swagger.v3.oas.annotations.Operation;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Dto.WrongReviewDto;
import org.example.paperwise.Dto.WrongReviewUpdateRequest;
import org.example.paperwise.Service.WrongReviewService;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.WrongReviewStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/paperwise/wrongreview")
public class WrongReviewController {
    @Autowired
    private WrongReviewService wrongReviewService;
    @GetMapping("/getwrongreviewdto")
    @Operation(summary = "初始化复习模块")
    public Result<WrongReviewDto>getWrongReviewDto(@RequestAttribute Long userid){
        return Result.success(wrongReviewService.getWrongReviewDto(userid));
    }

    @GetMapping("/getcard")
    @Operation(summary = "用户点击展开答案后调用获取卡片")
    public Result<Card>getCard(@RequestParam Long cardId){
        return Result.success(wrongReviewService.getCardById(cardId));
    }
    @PostMapping("/updateReviewProgress")
    @Operation(summary = "用户点击下一题更新复习状态，需要用户选择感受，分为0到5")
    public void updateReviewProgress(@RequestAttribute Long userid, @RequestBody WrongReviewUpdateRequest  wrongReviewUpdateRequest){
        wrongReviewService.updateReviewProgress(userid,wrongReviewUpdateRequest);
    }
    @GetMapping("/getwrongreviewtats")
    public Result<WrongReviewStats> getWrongReviewStats(@RequestAttribute Long userid){
        return Result.success(wrongReviewService.getWrongReviewStats(userid));
    }

}
