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
    private QianwenConfig qianwenConfig;

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS).build();

    public String chat(String userMessage) {
        try {
            // 构建正确的请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", qianwenConfig.getModel());

            // input 对象（存放 messages）
            Map<String, Object> input = new HashMap<>();

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            input.put("messages", messages);

            requestBody.put("input", input);

            // parameters 放在最外层，不在 userMsg 里
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_tokens",  16384);      // 输出最大 token 数
            parameters.put("result_format", "text"); // 返回格式
            requestBody.put("parameters", parameters);

            String json = JSON.toJSONString(requestBody);
            System.out.println("请求体: " + json);

            Request request = new Request.Builder()
                    .url("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
                    .addHeader("Authorization", "Bearer " + qianwenConfig.getApikey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            String responseBody = response.body().string();
            System.out.println("响应码: " + response.code());
            System.out.println("响应体: " + responseBody);

            JSONObject jsonObject = JSON.parseObject(responseBody);

            if (jsonObject.containsKey("code")) {
                throw new RuntimeException("API 错误: " + jsonObject.getString("message"));
            }

            String text = jsonObject.getJSONObject("output").getString("text");
            return text;

        } catch (IOException e) {
            throw new RuntimeException("AI调用失败: " + e.getMessage());
        }
    }




}
