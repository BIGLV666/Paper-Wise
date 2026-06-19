/**
 * AI生成卡片控制器
 * <p>提供AI生成卡片、暂存区管理、历史记录查询等功能</p>
 *
 * @author PaperWise Team
 */
package org.example.paperwise.Controller;

import io.swagger.v3.oas.annotations.Operation;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.AiGeneratedCardService;
import org.example.paperwise.entry.AiGeneratedCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI生成卡片控制器
 * <p>处理AI生成卡片的创建、暂存和收藏功能</p>
 */
@RestController
@RequestMapping("/paperwise/aigeneratedcard")
public class AiGeneratedCardController {

    @Autowired
    private AiGeneratedCardService aiGeneratedCardService;

    /**
     * 获取AI生成的卡片列表（按会话）
     * @param userid 用户ID
     * @return 卡片映射表（会话ID -> 卡片列表）
     */
    @Operation(summary = "获取AI生成的卡片列表")
    @GetMapping("/getaigeneratedcardbysessionid")
    public Result<Map<String, List<AiGeneratedCard>>> getAiGeneratedCardBySessionId(@RequestAttribute Long userid) {
        return Result.success(aiGeneratedCardService.getAiGeneratedCardBySessionId(userid));
    }

    /**
     * 批量将暂存区卡片添加到个人卡片库
     * @param aiGeneratedCards AI生成的卡片列表
     * @param userid 用户ID
     * @return 添加数量
     */
    @PostMapping("/batchcards")
    @Operation(summary = "批量添加卡片到个人卡片库")
    public Result<Integer> batchCards(@RequestBody List<AiGeneratedCard> aiGeneratedCards, @RequestAttribute Long userid) {
        return Result.success(aiGeneratedCardService.batchCards(aiGeneratedCards, userid));
    }

    /**
     * 批量添加卡片到收藏夹
     * @param aiGeneratedCards AI生成的卡片列表
     * @param favoriteId 收藏夹ID
     * @param userid 用户ID
     * @return 添加数量
     */
    @Operation(summary = "批量添加卡片到收藏夹")
    @PostMapping("/batchcardstofavorites")
    public Result<Integer> batchCardsToFavorites(@RequestBody List<AiGeneratedCard> aiGeneratedCards,
                                                  @RequestParam Long favoriteId,
                                                  @RequestAttribute Long userid) {
        return Result.success(aiGeneratedCardService.batchCardsToFavorites(aiGeneratedCards, favoriteId, userid));
    }

    /**
     * 从文本生成卡片
     * @param text 输入文本
     * @param name 生成批次名称
     * @param userid 用户ID
     * @return 操作结果
     */
    @Operation(summary = "文本生成卡片")
    @PostMapping("/generatefromtext")
    public Result<String> generateFromText(@RequestParam String text,
                                             @RequestParam String name,
                                             @RequestAttribute Long userid) {
        aiGeneratedCardService.generateFromText(userid, text, name, UUID.randomUUID().toString());
        return Result.success("生成任务已提交");
    }

    /**
     * 获取所有历史生成记录
     * @param userid 用户ID
     * @return 历史记录映射表
     */
    @Operation(summary = "获取所有历史记录")
    @GetMapping("/getallhistoryrecord")
    public Result<Map<String, List<Long>>> getAllHistoryRecord(@RequestAttribute Long userid) {
        return Result.success(aiGeneratedCardService.getAllHistoryRecord(userid));
    }
}
