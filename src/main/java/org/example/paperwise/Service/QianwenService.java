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

            // ✅ 关键：parameters 放在最外层，不在 userMsg 里
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



    private String buildPrompt(String text) {
        if(text.length()>5000){
            text=text.substring(0,5000);
        }
        return """
        你是一个学习卡片生成助手。请根据以下文本内容，将文本按照以下形式拆分
        
        每张卡片包含：
        - title：卡片标题/问题
        - questionType:题目所属大的类别
        - cardType：类型（SINGLE=单选题、MULTIPLE=多选题、TRUE_FALSE=判断题、ESSAY=问答题）
        - options：选项数组（选择题必填，其他类型可为空数组）
        - answer：正确答案
        - explanation：解析说明
        - cardDifficulty：难度（EASY、MEDIUM、HARD）
        
        
        输出必须是 JSON 数组格式，不要有其他文字。如果文本有截断只返回未截断部分.questionType要求中文如果是java或者mysql等等,可以直接写java等，
        
        
        示例：
        [
                  {
                       "title": "Java 的特点是什么？",
                       "questionType": "JAVA",
                       "cardType": "SINGLE",
                       "options": ["跨平台", "面向对象", "指针操作", "自动内存管理"],
                       "answer": "跨平台",
                       "explanation": "Java 通过 JVM 实现跨平台",
                       "cardDifficulty": "EASY"
                     },
                     {
                       "title": "以下哪些是 JVM 的内存区域？",
                       "questionType": "JAVA",
                       "cardType": "MULTIPLE",
                       "options": ["堆", "栈", "方法区", "寄存器", "CPU缓存"],
                       "answer": ["堆", "栈", "方法区"],
                       "explanation": "JVM 内存区域包括堆、栈、方法区、程序计数器",
                       "cardDifficulty": "MEDIUM"
                     }
                        ]
        
        文本内容：
        """ + text;
    }
}
