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
import java.util.function.Consumer;


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
    private AiProviderService aiProviderService;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    private final static String USER_HISTORY="user_history";

    /**
     * RAG问答
     * <p>核心逻辑：
     * 1. 验证用户登录状态和会话归属权
     * 2. 新会话则创建并异步生成标题
     * 3. 拼接历史上下文（从Redis或数据库获取最近90条）
     * 4. 调用AI服务获取回答
     * 5. 保存问答记录到数据库并更新Redis缓存</p>
     *
     * @param question 用户问题
     * @param sessionId 会话ID
     * @return AI回答
     */
    @Transactional
    public String ask(String question, String sessionId) {
        Long userId = UserContext.getUserId();
        String UUID;
        ChatSession chatSession;
        chatSession = chatSessionMapper.selectById(sessionId);

        if (userId == null) {
            throw new RuntimeException("请登录后尝试");
        }
        if (chatSession != null && !chatSession.getUserId().equals(userId)) {
            throw new RuntimeException("非本人对话");
        }

        // 新建会话
        if (chatSession == null) {
            chatSession = new ChatSession();
            UUID = java.util.UUID.randomUUID().toString();
            chatSession.setSessionId(UUID);
            sessionId = UUID;
            chatSession.setUserId(userId);
            chatSession.setTitle("新对话");
            chatSession.setCreateTime(LocalDateTime.now());
            chatSession.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.insert(chatSession);

            // 异步生成会话标题
            ChatSession finalChatSession = chatSession;
            CompletableFuture.runAsync(() -> {
                String title = chatForUser(userId, "给下述问题生成简短标题，10字以内,只包含题目，不要有多余的文字和任何标点" + question);
                finalChatSession.setTitle(title);
                chatSessionMapper.updateById(finalChatSession);
            });
        }

        // 获取历史上下文（Redis优先，缓存30分钟）
        String historyKey = USER_HISTORY + "--" + "sessionId " + sessionId + "--" + "userId" + userId;
        String history = (String) redisTemplate.opsForValue().get(historyKey);

        String fullPrompt;
        if (history == null || history.isEmpty()) {
            history = buildContextPrompt(chatMessageMapper.getRecentBySessionId(sessionId, userId, 90));
            redisTemplate.opsForValue().set(historyKey, history, 30, TimeUnit.MINUTES);
        }
        fullPrompt = history + "\nuser：" + question;

        String answer = chatForUser(userId, fullPrompt);

        // AI 成功后再保存用户消息，避免失败请求污染聊天记录
        ChatMessage userMessage = new ChatMessage();
        userMessage.setUserId(userId);
        userMessage.setSessionId(chatSession.getSessionId());
        userMessage.setContent(question);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setRole("user");
        chatMessageMapper.insert(userMessage);

        // 保存AI回答
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(chatSession.getSessionId());
        chatMessage.setUserId(chatSession.getUserId());
        chatMessage.setRole("ai");
        chatMessage.setContent(answer);
        chatMessage.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(chatMessage);

        chatSession.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(chatSession);

        // 更新Redis缓存
        updatePrompt(question, answer, sessionId);
        return answer;
    }

    @Transactional
    public void askStream(Long userId, String question, String sessionId, Consumer<String> onSession, Consumer<String> onDelta) {
        if (userId == null) throw new RuntimeException("请登录后尝试");
        ChatSession chatSession = chatSessionMapper.selectById(sessionId);
        if (chatSession != null && !userId.equals(chatSession.getUserId())) throw new RuntimeException("非法对话");
        if (chatSession == null) {
            chatSession = new ChatSession();
            sessionId = java.util.UUID.randomUUID().toString();
            chatSession.setSessionId(sessionId);
            chatSession.setUserId(userId);
            chatSession.setTitle("新对话");
            chatSession.setCreateTime(LocalDateTime.now());
            chatSession.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.insert(chatSession);
        }
        onSession.accept(chatSession.getSessionId());

        String historyKey = USER_HISTORY + "--sessionId " + chatSession.getSessionId() + "--userId" + userId;
        String history = (String) redisTemplate.opsForValue().get(historyKey);
        if (history == null || history.isEmpty()) {
            history = buildContextPrompt(chatMessageMapper.getRecentBySessionId(chatSession.getSessionId(), userId, 90));
        }
        String fullPrompt = history + "\nuser：" + question;
        StringBuilder answer = new StringBuilder();
        aiProviderService.stream(userId, fullPrompt, delta -> {
            answer.append(delta);
            onDelta.accept(delta);
        });

        // 流式 AI 完整返回后再落库，401、超时或中止都不会保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setUserId(userId);
        userMessage.setSessionId(chatSession.getSessionId());
        userMessage.setContent(question);
        userMessage.setRole("user");
        userMessage.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(userMessage);

        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setUserId(userId);
        aiMessage.setSessionId(chatSession.getSessionId());
        aiMessage.setRole("ai");
        aiMessage.setContent(answer.toString());
        aiMessage.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(aiMessage);
        chatSession.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(chatSession);
        updatePrompt(question, answer.toString(), chatSession.getSessionId(), userId);
    }

    private String chatForUser(Long userId, String prompt) {
        if (aiProviderService.getConfig(userId) != null) return aiProviderService.chat(userId, prompt);
        return aiService.chat(prompt);
    }

    /**
     * 构建上下文提示字符串
     * <p>将历史消息列表转换为AI对话格式的字符串</p>
     *
     * @param history 历史消息列表
     * @return 格式化的对话上下文
     */
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

    /**
     * 更新Redis中的对话历史缓存
     *
     * @param question 用户问题
     * @param answer AI回答
     * @param sessionId 会话ID
     */
    private void updatePrompt(String question, String answer, String sessionId) {
        Long userId = UserContext.getUserId();
        updatePrompt(question, answer, sessionId, userId);
    }

    private void updatePrompt(String question, String answer, String sessionId, Long userId) {
        String key = USER_HISTORY + "--" + "sessionId " + sessionId + "--" + "userId" + userId;

        String oldHistory = (String) redisTemplate.opsForValue().get(key);
        String newEntry = "user：" + question + "\nai：" + answer + "\n";

        String newHistory;
        if (oldHistory == null || oldHistory.isEmpty()) {
            newHistory = buildContextPrompt(chatMessageMapper.getRecentBySessionId(sessionId, userId, 90));
        } else {
            newHistory = oldHistory + newEntry;
        }

        redisTemplate.opsForValue().set(key, newHistory, 30, TimeUnit.MINUTES);
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
