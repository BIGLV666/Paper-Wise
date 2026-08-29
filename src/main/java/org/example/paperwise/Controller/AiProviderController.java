package org.example.paperwise.Controller;

import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.AiProviderService;
import org.example.paperwise.entry.AiProviderConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/paperwise/ai-provider")
public class AiProviderController {
    @Autowired
    private AiProviderService aiProviderService;

    @GetMapping
    public Result<AiProviderConfig> get(@RequestAttribute Long userid) {
        AiProviderConfig config = aiProviderService.getMaskedConfig(userid);
        return Result.success(config == null ? new AiProviderConfig() : config);
    }

    @PutMapping
    public Result<AiProviderConfig> save(@RequestAttribute Long userid, @RequestBody AiProviderConfig config) {
        return Result.success(aiProviderService.saveConfig(userid, config));
    }

    @PostMapping("/test")
    public Result<String> test(@RequestAttribute Long userid, @RequestBody AiProviderConfig config) {
        aiProviderService.saveConfig(userid, config);
        return Result.success(aiProviderService.chat(userid, "请只回复：连接成功"));
    }
}
