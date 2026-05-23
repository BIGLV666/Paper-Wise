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

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS).build();

    public String chat(String userMessage){
        try {
            Map<String,Object>requestBody = new HashMap<>();
            requestBody.put("model",qianwenConfig.getModel());

            //构建消息列表
            List<Map<String,String>> messages = new ArrayList<>();
            Map<String,String> userMsg = new HashMap<>();
            userMsg.put("role","user");
            userMsg.put("content",userMessage);
            messages.add(userMsg);
            requestBody.put("messages",messages);

            //TO JSON
            String json = JSON.toJSONString(requestBody);

            Request request=new Request.Builder()
                    .url("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
                    .addHeader("Authorization","Bearer"+qianwenConfig.getApikey())
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            Response response=okHttpClient.newCall(request).execute();
            String responseBody=response.body().string();

            JSONObject jsonObject = JSON.parseObject(responseBody);
            String text=jsonObject.getJSONObject("output")
                    .getString("text");
            return text;

        }catch (IOException e){
            log.error(e.getMessage(),e);
            throw  new RuntimeException("ai调用失败"+e.getMessage());
        }
    }


}
