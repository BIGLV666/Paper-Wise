/**
 * AI智能问答控制器
 * <p>提供基于RAG的AI问答、历史记录管理等功能</p>
 *
 * @author PaperWise Team
 */
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

/**
 * AI问答控制器
 * <p>处理AI智能问答、对话历史管理等功能</p>
 */
@RestController
@RequestMapping("/paperwise/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private RAGService ragService;

    /**
     * AI问答接口
     * @param body 包含问题内容和会话ID
     * @return AI回答内容
     */
    @PostMapping("/ask")
    public Result<String> ask(@RequestBody(required = false) ChatAskDto body) {
        String answer = ragService.ask(body.getQuestion(), body.getSessionId());
        return Result.success(answer);
    }

    /**
     * 获取所有对话历史会话
     * @return 会话列表
     */
    @Operation(summary = "获取历史会话列表")
    @GetMapping("/getallhistory")
    public Result<List<ChatSession>> getAllHistory() {
        return Result.success(ragService.getAllSessions());
    }

    /**
     * 获取指定会话的所有消息
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/getallmessage")
    public Result<List<ChatMessage>> getAllMessage(@RequestParam String sessionId) {
        return Result.success(ragService.getAllHistoryMessages(sessionId));
    }

    /**
     * 删除对话历史
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/deletehistory")
    public Result<String> deleteHistory(@RequestParam String sessionId) {
        ragService.deleteHistory(sessionId);
        return Result.success("删除成功");
    }

    /**
     * 修改会话标题
     * @param sessionId 会话ID
     * @param title 新标题
     * @return 操作结果
     */
    @PostMapping("/updatetitle")
    public Result<String> updateTitle(@RequestParam String sessionId, @RequestParam String title) {
        ragService.updateTitle(sessionId, title);
        return Result.success("修改成功");
    }
}
