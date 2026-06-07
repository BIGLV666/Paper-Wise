package org.example.paperwise.Controller;

import io.swagger.v3.oas.annotations.Operation;
import org.example.paperwise.Dto.ChatAskDto;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.AiService;
import org.example.paperwise.Service.RAGService;
import org.example.paperwise.entry.ChatMessage;
import org.example.paperwise.entry.ChatSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paperwise/ai")
public class AiController {
    @Autowired
    private AiService aiService;
    @Autowired
    private RAGService ragService;

    @PostMapping("/ask")
    public Result<String> ask(@RequestBody(required = false) ChatAskDto body) {

        String answer = ragService.ask(body.getQuestion(),body.getSessionId());
        return Result.success(answer);
    }

    @Operation(summary = "获取历史记录")
    @GetMapping("/getallhistory")
    public Result<List<ChatSession>>getAllHistory(){
        return Result.success(ragService.getAllSessions());
    }
    @GetMapping("/getallmessage")
    public Result<List<ChatMessage>> getAllMessage(@RequestParam String sessionId){
        return Result.success(ragService.getAllHistoryMessages(sessionId));
    }
    @DeleteMapping("/deletehistory")
    public Result<String> deleteHistory(@RequestParam String sessionId){
        ragService.deleteHistory(sessionId);
        return Result.success("success");
    }
    //修改标题
    @PostMapping("/updatetitle")
    public Result<String>updateTitle(@RequestParam String sessionId,@RequestParam String title){
        ragService.updateTitle(sessionId,title);
        return Result.success("success");
    }
}
