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

@RestController
@RequestMapping("/paperwise/aigeneratedcard")
public class AiGeneratedCardController {
    @Autowired
    private AiGeneratedCardService aiGeneratedCardService;

    @Operation(summary = "结果查询")
    @GetMapping("/getaigeneratedcardbysessionid")
    public Result<Map<String,List<AiGeneratedCard>>> getAiGeneratedCardBySessionId(@RequestAttribute Long userid) {
        return Result.success(aiGeneratedCardService.getAiGeneratedCardBySessionId(userid));
    }
    @PostMapping("/batchcards")
    @Operation(summary = "批量将暂存区卡片添加")
    public Result<Integer> batchCards(@RequestBody List<AiGeneratedCard> aiGeneratedCards,@RequestAttribute Long userid) {
        return Result.success(aiGeneratedCardService.batchCards(aiGeneratedCards,userid));
    }
    @Operation(summary = "批量添加卡片到收藏")
    @PostMapping("/batchcardstofavorites")
    public Result<Integer>batchCardsToFavorites(@RequestBody List<AiGeneratedCard> aiGeneratedCards, @RequestParam Long favoriteId,@RequestAttribute Long userid) {
        return Result.success(aiGeneratedCardService.batchCardsToFavorites(aiGeneratedCards,favoriteId,userid));
    }
    @Operation(summary = "文本生成")
    @PostMapping("/generatefromtext")
    public Result<String> generateFromText(@RequestParam String text,@RequestParam String name,@RequestAttribute Long userid){
        aiGeneratedCardService.generateFromText(userid,text,name, UUID.randomUUID().toString());
        return Result.success("success");
    }
    @Operation(summary = "获取所有历史记录，返回的id")
    @GetMapping("/getallhistoryrecord")
    public Result<Map<String,List<Long>>> getAllHistoryRecord(@RequestAttribute Long userid){
        return Result.success(aiGeneratedCardService.getAllHistoryRecord(userid));
    }

}
