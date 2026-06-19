package org.example.paperwise.Service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.entry.Card;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class VectorStoreService {
    //@Autowired
    private AiService aiService;
    //@Autowired
    private EmbeddingStore<TextSegment> embeddingStore;
    //@Autowired
    private EmbeddingModel embeddingModel;
    //@Autowired
    private CardMapper  cardMapper;
    /**
     * 初始化向量知识库
     * <p>将所有卡片内容转换为向量并存储到向量数据库，用于RAG检索</p>
     */
    public void init() {
        List<Card> cards = cardMapper.selectAll();
        log.info("开始初始化向量库，共 {} 张卡片", cards.size());

        for (Card card : cards) {
            // 构建卡片文本
            String text = String.format(
                    "题目：%s\n选项：%s\n正确答案：%s\n解析：%s\n说明：%s",
                    card.getTitle(),
                    card.getOptions() != null ? String.join(", ", card.getOptions()) : "无",
                    card.getAnswer(),
                    card.getExplanation() != null ? card.getExplanation() : "",
                    "注意：如果题目是\"xxx的优点不包括\"，则正确答案不是优点。"
            );

            // 生成向量嵌入
            var embeddingResponse = embeddingModel.embed(text);

            if (embeddingResponse == null || embeddingResponse.content() == null) {
                log.error("卡片 {} 的 embedding 为 null", card.getCardId());
                continue;
            }

            // 存储到向量数据库
            Embedding embedding = embeddingResponse.content();
            TextSegment segment = TextSegment.from(text);
            embeddingStore.add(embedding, segment);
            log.info("卡片 {} 已添加到向量库", card.getCardId());
        }
        log.info("向量库初始化完成");
    }
}