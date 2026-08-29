package org.example.paperwise.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.example.paperwise.Config.QianwenConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class QianwenService {
    @Autowired
    private AiProviderService aiProviderService;
    @Autowired
    private QianwenConfig qianwenConfig;

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS).build();

    /**
     * 调用阿里云通义千问API进行对话
     * <p>构建符合阿里云API规范的请求体，发送HTTP POST请求，获取AI生成文本</p>
     *
     * @param userMessage 用户消息
     * @return AI生成的文本回答
     */
    public String chat(String userMessage) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", qianwenConfig.getModel());

            Map<String, Object> input = new HashMap<>();
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            input.put("messages", messages);
            requestBody.put("input", input);

            // 设置API参数
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_tokens", 16384);
            parameters.put("result_format", "text");
            requestBody.put("parameters", parameters);

            String json = JSON.toJSONString(requestBody);

            Request request = new Request.Builder()
                    .url("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
                    .addHeader("Authorization", "Bearer " + qianwenConfig.getApikey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            String responseBody = response.body().string();

            JSONObject jsonObject = JSON.parseObject(responseBody);

            if (jsonObject.containsKey("code")) {
                throw new RuntimeException("API 错误: " + jsonObject.getString("message"));
            }

            return jsonObject.getJSONObject("output").getString("text");

        } catch (IOException e) {
            throw new RuntimeException("AI调用失败: " + e.getMessage());
        }
    }

    public String chat(Long userId, String userMessage) {
        if (userId != null && aiProviderService.getConfig(userId) != null) {
            return aiProviderService.chat(userId, userMessage);
        }
        return chat(userMessage);
    }




}
