package org.example.paperwise.Controller;

import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.AiService;
import org.example.paperwise.Service.RAGService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/paperwise/ai")
public class AiController {
    @Autowired
    private AiService aiService;
    @Autowired
    private RAGService ragService;

    @PostMapping("/ask")
    public Result<String> ask(@RequestBody(required = false) Map<String, String> body,
                              @RequestParam(required = false) String question) {
        String q = body != null && body.containsKey("question")
                ? body.get("question")
                : question;
        String answer = ragService.ask(q);
        return Result.success(answer);
    }
}
