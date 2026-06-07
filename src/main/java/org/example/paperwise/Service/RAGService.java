package org.example.paperwise.Service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RAGService {

    private static final Set<String> RAG_EXCLUDE_KEYWORDS = Set.of(
            "错题", "错题本", "我的错题", "错误次数","错"
    );


    //@Autowired
    private EmbeddingStore<TextSegment> embeddingStore;
    //@Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private AiService aiService;

    public String ask(String question) {
        return aiService.chat(question);
    }
}
