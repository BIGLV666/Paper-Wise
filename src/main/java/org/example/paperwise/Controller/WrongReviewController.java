/**
 * 错题复习控制器
 * <p>提供错题复习、进度跟踪等功能</p>
 *
 * @author PaperWise Team
 */
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

/**
 * 错题复习控制器
 * <p>处理错题的复习计划、进度更新和统计</p>
 */
@RestController
@RequestMapping("/paperwise/wrongreview")
public class WrongReviewController {

    @Autowired
    private WrongReviewService wrongReviewService;

    /**
     * 初始化复习模块
     * @param userid 用户ID
     * @return 复习任务列表
     */
    @GetMapping("/getwrongreviewdto")
    @Operation(summary = "初始化复习模块")
    public Result<WrongReviewDto> getWrongReviewDto(@RequestAttribute Long userid) {
        return Result.success(wrongReviewService.getWrongReviewDto(userid));
    }

    /**
     * 获取卡片详情（展开答案时调用）
     * @param cardId 卡片ID
     * @return 卡片信息
     */
    @GetMapping("/getcard")
    @Operation(summary = "获取卡片详情")
    public Result<Card> getCard(@RequestParam Long cardId) {
        return Result.success(wrongReviewService.getCardById(cardId));
    }

    /**
     * 更新复习进度
     * @param userid 用户ID
     * @param wrongReviewUpdateRequest 复习反馈(0-5)
     */
    @PostMapping("/updateReviewProgress")
    @Operation(summary = "更新复习状态")
    public void updateReviewProgress(@RequestAttribute Long userid, @RequestBody WrongReviewUpdateRequest wrongReviewUpdateRequest) {
        wrongReviewService.updateReviewProgress(userid, wrongReviewUpdateRequest);
    }

    /**
     * 获取复习统计信息
     * @param userid 用户ID
     * @return 复习统计数据
     */
    @GetMapping("/getwrongreviewtats")
    public Result<WrongReviewStats> getWrongReviewStats(@RequestAttribute Long userid) {
        return Result.success(wrongReviewService.getWrongReviewStats(userid));
    }
}
