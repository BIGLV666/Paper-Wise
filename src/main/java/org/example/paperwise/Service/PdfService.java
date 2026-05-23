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
        return """
        你是一个学习卡片生成助手。请根据以下文本内容，生成 3-5 张学习卡片。
        
        每张卡片包含：
        - title：卡片标题/问题
        - type：类型（SINGLE=单选题、MULTIPLE=多选题、TRUE_FALSE=判断题、ESSAY=问答题）
        - options：选项数组（选择题必填，其他类型可为空数组）
        - answer：正确答案
        - difficulty：难度（EASY、MEDIUM、HARD）
        - explanation：解析说明
        
        输出必须是 JSON 数组格式，不要有其他文字。
        
        示例：
        [
                  {
                       "title": "Java 的特点是什么？",
                       "questionType": "JAVA",
                       "cardType": "SINGLE",
                       "options": ["跨平台", "面向对象", "指针操作", "自动内存管理"],
                       "answer": "跨平台",
                       "explanation": "Java 通过 JVM 实现跨平台",
                       "cardDifficulty": "EASY",
                       "cardMastery": "NOT_STARTED"
                     },
                     {
                       "title": "以下哪些是 JVM 的内存区域？",
                       "questionType": "JAVA",
                       "cardType": "MULTIPLE",
                       "options": ["堆", "栈", "方法区", "寄存器", "CPU缓存"],
                       "answer": ["堆", "栈", "方法区"],
                       "explanation": "JVM 内存区域包括堆、栈、方法区、程序计数器",
                       "cardDifficulty": "MEDIUM",
                       "cardMastery": "LEARNING"
                     }
                        ]
        
        文本内容：
        """ + text;
    }

    //提取pdf文字
    private String getText(MultipartFile multipartFile)throws IOException {
        try
            (PDDocument document=PDDocument.load(multipartFile.getInputStream())){
            PDFTextStripper stripper=new PDFTextStripper();
            String text=stripper.getText(document);
            return text.trim();
        }
    }
    //解析ai
    private List<Card> parseCards(String aiResponse,Long userid){
        try {
            String json=extractJson(aiResponse);
            List<Card> cards=objectMapper.readValue(json,new TypeReference<List<Card>>(){});
            for(Card card:cards){
                if(card.getUserid()==null){
                    card.setUserid(userid);
                }
                if(card.getCardType()==null){
                    card.setCardType(CardType.SINGLE);
                }
                if(card.getCardDifficulty()==null){
                    card.setCardDifficulty(CardDifficulty.MEDIUM);
                }
                if(card.getCardMastery()==null){
                    card.setCardMastery(CardMastery.NOT_STARTED);
                }
                if(card.getCreateTime()==null){
                    card.setCreateTime(LocalDateTime.now());
                }
            }
            return cards;
        }catch (Exception e){
            e.printStackTrace();
            log.info("卡片解析失败{}", e.getMessage());
            return null;
        }
    }
    //解析json
    private String extractJson(String jsonText){
        int start=jsonText.indexOf("[");
        int end=jsonText.indexOf("]");
        if(start!=-1&&end!=-1){
            return jsonText.substring(start,end+1);
        }
        throw new RuntimeException("ai相应不是json");
    }
    public List<Card> generateFromPdf(Long userid, MultipartFile file)throws IOException {
        String text =getText(file);
        String prompt = buildPrompt(text);
        String aiResponse= qianwenService.chat(prompt);
        List<Card>cards=parseCards(aiResponse,userid);
        if (cards != null ) {
            cardMapper.batchAddCard(cards);
        }
        return cards;
    }



}
