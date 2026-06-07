package org.example.paperwise.Config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.example.paperwise.Service.AiService;
import org.example.paperwise.Until.AITools;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChainConfig {

    // 嵌入模型（用于向量化）
    //@Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text")  // embedding 模型
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean(name = "myChatModel")
    public ChatModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen2.5:7b")
                .temperature(0.7)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(0)
                .build();
    }


    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);  // 记住最近10条
    }

    // 向量存储（内存版）
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
    @Bean
    public AiService aiService(@Qualifier("myChatModel") ChatModel model, AITools tools) {
        System.out.println("工具已注册");
        return AiServices.builder(AiService.class)
                .chatModel(model)
                .chatMemory(chatMemory())
                .tools(tools)
                .build();
    }

}