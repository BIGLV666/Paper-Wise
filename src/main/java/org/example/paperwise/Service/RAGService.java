package org.example.paperwise.Service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.example.paperwise.Mapper.ChatMessageMapper;
import org.example.paperwise.Mapper.ChatSessionMapper;
import org.example.paperwise.Until.UserContext;
import org.example.paperwise.entry.ChatMessage;
import org.example.paperwise.entry.ChatSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Service
public class RAGService {

    private static final Set<String> RAG_EXCLUDE_KEYWORDS = Set.of(
            "错题", "错题本", "我的错题", "错误次数","错"
    );


    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;
    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private ChatSessionMapper chatSessionMapper;
    @Autowired
    private AiService aiService;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    private final static String USER_HISTORY="user_history";

    @Transactional
    public String ask(String question,String sessionId) {

        Long userId= UserContext.getUserId();
        String UUID;
        ChatSession chatSession;
        chatSession=chatSessionMapper.selectById(sessionId);
        if(userId==null){
            throw new RuntimeException("请登录后尝试");
        }
        if(chatSession!=null&&!chatSession.getUserId().equals(userId)){
            throw new RuntimeException("非本人对话");
        }

        if(chatSession==null){
            chatSession=new ChatSession();
            UUID= java.util.UUID.randomUUID().toString();
            chatSession.setSessionId(UUID);
            sessionId=UUID;
            chatSession.setUserId(userId);
            chatSession.setTitle("新对话");
            chatSession.setCreateTime(LocalDateTime.now());
            chatSession.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.insert(chatSession);

            ChatSession finalChatSession = chatSession;
            CompletableFuture.runAsync(()->{
                String title=aiService.chat("给下述问题生成简短标题，10字以内,只包含题目，不要有多余的文字和任何标点"+question);
                finalChatSession.setTitle(title);
                chatSessionMapper.updateById(finalChatSession);
                    }
            );
        }


        //保存用户信息
        ChatMessage UserMessage=new ChatMessage();
        UserMessage.setUserId(userId);
        UserMessage.setSessionId(chatSession.getSessionId());
        UserMessage.setContent(question);
        UserMessage.setCreateTime(LocalDateTime.now());
        UserMessage.setRole("user");
        chatMessageMapper.insert(UserMessage);

        String historyKey = USER_HISTORY+"--"+"sessionId "+sessionId+"--"+ "userId"+userId;
        String history = (String) redisTemplate.opsForValue().get(historyKey);

        String fullPrompt;
        if (history == null || history.isEmpty()) {
            history=buildContextPrompt(chatMessageMapper.getRecentBySessionId(sessionId,userId,90));
            redisTemplate.opsForValue().set(historyKey, history,30, TimeUnit.MINUTES);
        }
        fullPrompt = history + "\nuser：" + question;


        System.out.println(fullPrompt);
        String answer= aiService.chat(fullPrompt);


        //保存ai回答
        ChatMessage chatMessage=new ChatMessage();
        chatMessage.setSessionId(chatSession.getSessionId());
        chatMessage.setUserId(chatSession.getUserId());
        chatMessage.setRole("ai");
        chatMessage.setContent(answer);
        chatMessage.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(chatMessage);

        chatSession.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(chatSession);

        //更新redis
        updatePrompt(question,answer,sessionId);
        return answer;
    }

    //构建上下文
    private String buildContextPrompt(List<ChatMessage> history) {

        if (history == null || history.isEmpty()) {
            return "";
        }
        Collections.reverse(history);
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                sb.append("user：").append(msg.getContent()).append("\n");
            } else {
                sb.append("ai：").append(msg.getContent()).append("\n");
            }
        }
        return sb.toString();
    }

    //更新redis历史对话
    private void updatePrompt(String question, String answer,String sessionId) {
        Long userId = UserContext.getUserId();
        String key = USER_HISTORY+"--"+"sessionId "+sessionId+"--"+ "userId"+userId;

        String oldHistory = (String) redisTemplate.opsForValue().get(key);
        String newEntry = "user：" + question + "\nai：" + answer + "\n";

        String newHistory;
        if (oldHistory == null || oldHistory.isEmpty()) {
           newHistory=buildContextPrompt(chatMessageMapper.getRecentBySessionId(sessionId,userId,90));
        } else {
            newHistory = oldHistory + newEntry;
        }



        redisTemplate.opsForValue().set(key, newHistory,30, TimeUnit.MINUTES);
    }

    //获取对话历史
    public List<ChatSession>getAllSessions(){
        return chatSessionMapper.getAllHistory(UserContext.getUserId());
    }
    //获取历史会话
    public List<ChatMessage> getAllHistoryMessages(String sessionId){
         List<ChatMessage>list= chatMessageMapper.getAllHistoryMessages(sessionId,UserContext.getUserId());
         Collections.reverse(list);
         return list;
    }
    //删除对话历史
    @Transactional
    public void deleteHistory(String sessionId){
        chatMessageMapper. deleteHistory(sessionId,UserContext.getUserId());
        chatSessionMapper.deleteById(sessionId);
    }

    //修改标题名称
    @Transactional
    public void updateTitle(String sessionId,String title){
        ChatSession chatSession=chatSessionMapper.selectById(sessionId);
        if(chatSession==null){
            throw new RuntimeException("未找到该对话");
        }
        if(!chatSession.getUserId().equals(UserContext.getUserId())){
            throw new RuntimeException("请勿操作他人收藏夹");
        }
        if(title==null||title.isEmpty()||title.length()>20){
            throw new RuntimeException("不合法标题");
        }
        chatSession.setTitle(title);
        chatSession.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(chatSession);
    }

}
