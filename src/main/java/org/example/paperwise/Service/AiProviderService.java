package org.example.paperwise.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.example.paperwise.Mapper.AiProviderConfigMapper;
import org.example.paperwise.entry.AiProviderConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

@Service
public class AiProviderService {
    @Autowired
    private AiProviderConfigMapper configMapper;

    @Value("${jwt.secret}")
    private String encryptionSecret;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .callTimeout(660, TimeUnit.SECONDS)
            .build();

    @PostConstruct
    public void migratePlaintextKeys() {
        for (AiProviderConfig config : configMapper.selectList(null)) {
            String key = config.getApiKey();
            if (key != null && !key.isBlank() && !key.startsWith("ENC[v1]:")) {
                config.setApiKey(encrypt(key));
                configMapper.updateById(config);
            }
        }
    }

    public AiProviderConfig getConfig(Long userId) {
        AiProviderConfig config = configMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiProviderConfig>()
                .eq("user_id", userId).last("LIMIT 1"));
        if (config != null) {
            String storedKey = config.getApiKey();
            if (storedKey != null && !storedKey.startsWith("ENC[v1]:")) {
                config.setApiKey(encrypt(storedKey));
                configMapper.updateById(config);
            }
            config.setApiKey(decryptIfNeeded(config.getApiKey()));
        }
        return config;
    }

    public AiProviderConfig getMaskedConfig(Long userId) {
        AiProviderConfig config = getConfig(userId);
        return config == null ? null : masked(config);
    }

    public AiProviderConfig saveConfig(Long userId, AiProviderConfig incoming) {
        AiProviderConfig config = getConfig(userId);
        if (config == null) {
            config = new AiProviderConfig();
            config.setUserId(userId);
        }
        config.setProviderName(incoming.getProviderName());
        config.setBaseUrl(incoming.getBaseUrl());
        if (incoming.getApiKey() != null && !incoming.getApiKey().isBlank()
                && !incoming.getApiKey().startsWith("****")) {
            config.setApiKey(incoming.getApiKey());
        }
        config.setModel(incoming.getModel());
        config.setTemperature(incoming.getTemperature() == null ? 0.7 : incoming.getTemperature());
        config.setMaxTokens(incoming.getMaxTokens() == null ? 4096 : incoming.getMaxTokens());
        config.setEnabled(incoming.getEnabled() == null ? 1 : incoming.getEnabled());
        config.setUpdatedAt(java.time.LocalDateTime.now());
        String plainApiKey = config.getApiKey();
        if (plainApiKey == null || plainApiKey.isBlank()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }
        config.setApiKey(encryptIfNeeded(plainApiKey));
        if (config.getAiProviderConfigId() == null) configMapper.insert(config);
        else configMapper.updateById(config);
        config.setApiKey(plainApiKey);
        return masked(config);
    }

    public String chat(Long userId, String message) {
        AiProviderConfig config = getConfig(userId);
        if (config == null || config.getEnabled() == null || config.getEnabled() == 0
                || config.getBaseUrl() == null || config.getBaseUrl().isBlank()
                || config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在 AI 设置中配置可用的 API");
        }
        String endpoint = config.getBaseUrl().replaceAll("/$", "");
        if (!endpoint.endsWith("/chat/completions")) endpoint += "/chat/completions";
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());
        body.put("temperature", config.getTemperature());
        body.put("max_tokens", config.getMaxTokens());
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", message));
        body.put("messages", messages);

        Request request = new Request.Builder().url(endpoint)
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(JSON.toJSONString(body), MediaType.parse("application/json")))
                .build();
        try (Response response = client.newCall(request).execute()) {
            String raw = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) throw new IllegalStateException("AI API 请求失败: " + response.code());
            JSONObject json = JSON.parseObject(raw);
            return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } catch (IOException e) {
            throw new IllegalStateException("AI API 请求失败: " + e.getMessage(), e);
        }
    }

    public void stream(Long userId, String message, Consumer<String> onDelta) {
        AiProviderConfig config = getConfig(userId);
        if (config == null || config.getEnabled() == null || config.getEnabled() == 0
                || config.getBaseUrl() == null || config.getBaseUrl().isBlank()
                || config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在 AI 设置中配置可用的 API");
        }
        String endpoint = config.getBaseUrl().replaceAll("/$", "");
        if (!endpoint.endsWith("/chat/completions")) endpoint += "/chat/completions";
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());
        body.put("temperature", config.getTemperature());
        body.put("max_tokens", config.getMaxTokens());
        body.put("stream", true);
        body.put("messages", List.of(Map.of("role", "user", "content", message)));

        Request request = new Request.Builder().url(endpoint)
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(JSON.toJSONString(body), MediaType.parse("application/json")))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String detail = response.body() == null ? "" : response.body().string();
                if (detail.length() > 500) detail = detail.substring(0, 500);
                throw new IllegalStateException("AI API 流式请求失败: " + response.code()
                        + (detail.isBlank() ? "" : " - " + detail));
            }
            try (java.io.BufferedReader reader = new java.io.BufferedReader(response.body().charStream())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    JSONObject json = JSON.parseObject(data);
                    if (json.getJSONArray("choices") == null || json.getJSONArray("choices").isEmpty()) continue;
                    JSONObject delta = json.getJSONArray("choices").getJSONObject(0).getJSONObject("delta");
                    if (delta != null && delta.getString("content") != null) onDelta.accept(delta.getString("content"));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("AI API 流式请求失败: " + e.getMessage(), e);
        }
    }

    private AiProviderConfig masked(AiProviderConfig source) {
        AiProviderConfig copy = new AiProviderConfig();
        org.springframework.beans.BeanUtils.copyProperties(source, copy);
        if (source.getApiKey() != null && source.getApiKey().length() > 8) {
            copy.setApiKey(source.getApiKey().substring(0, 4) + "****" + source.getApiKey().substring(source.getApiKey().length() - 4));
        } else if (source.getApiKey() != null) copy.setApiKey("****");
        return copy;
    }

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey(), "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return "ENC[v1]:" + java.util.Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 加密失败", e);
        }
    }

    private String encryptIfNeeded(String value) {
        return value == null || value.startsWith("ENC[v1]:") ? value : encrypt(value);
    }

    private String decryptIfNeeded(String value) {
        if (value == null || !value.startsWith("ENC[v1]:")) return value;
        try {
            byte[] payload = java.util.Base64.getDecoder().decode(value.substring("ENC[v1]:".length()));
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey(), "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 解密失败，请重新保存 AI 配置", e);
        }
    }

    private byte[] secretKey() throws Exception {
        return MessageDigest.getInstance("SHA-256")
                .digest(encryptionSecret.getBytes(StandardCharsets.UTF_8));
    }
}
