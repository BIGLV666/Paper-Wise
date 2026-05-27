package org.example.paperwise.Service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.entry.Card;
import org.example.paperwise.enums.CardDifficulty;
import org.example.paperwise.enums.CardMastery;
import org.example.paperwise.enums.CardType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PdfService {
    @Autowired
    private QianwenService qianwenService;
    @Autowired
    private CardMapper cardMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();


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

    private String getText(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text.trim();
        }
    }

    private List<Card> parseCards(String aiResponse, Long userId) {
        try {
            String json = extractJson(aiResponse);
            //System.out.println("解析的 JSON: " + json);
            log.info("解析的 JSON: {}", json);
            List<Card> cards = objectMapper.readValue(json, new TypeReference<List<Card>>() {});

            for (Card card : cards) {
                if (card.getUserid() == null) {
                    card.setUserid(userId);
                }
                if (card.getCreateTime() == null) {
                    card.setCreateTime(LocalDateTime.now());
                }
                // 设置默认值（如果 AI 没返回）
                if (card.getCardType() == null) {
                    card.setCardType(CardType.ESSAY);
                }
                if (card.getCardDifficulty() == null) {
                    card.setCardDifficulty(CardDifficulty.MEDIUM);
                }
                if (card.getCardMastery() == null) {
                    card.setCardMastery(CardMastery.NOT_STARTED);
                }
            }
            return cards;

        } catch (Exception e) {
            log.error("卡片解析失败: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String extractJson(String jsonText) {
        int start = jsonText.indexOf("[");
        int end = jsonText.lastIndexOf("]");
        if (start != -1 && end != -1 && end > start) {
            return jsonText.substring(start, end + 1);
        }
        throw new RuntimeException("AI 响应中没有找到 JSON 数组");
    }

    public List<Card> generateFromPdf(Long userId, MultipartFile file) throws IOException {
        String text = getText(file);
        log.info("PDF 文本长度: {}", text.length());

        String prompt = buildPrompt(text);
        String aiResponse = qianwenService.chat(prompt);
        log.info("AI 响应长度: {}", aiResponse.length());

        List<Card> cards = parseCards(aiResponse, userId);
        if (cards != null && !cards.isEmpty()) {
            cardMapper.batchAddCard(cards);
            log.info("成功生成 {} 张卡片", cards.size());
        }
        return cards;
    }
}