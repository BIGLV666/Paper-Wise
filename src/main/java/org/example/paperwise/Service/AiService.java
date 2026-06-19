package org.example.paperwise.Service;

import dev.langchain4j.service.SystemMessage;

/**
 * AI问答服务接口
 * <p>定义AI对话的核心接口，使用LangChain4j框架实现</p>
 */
public interface AiService {

    /**
     * AI对话接口
     * @param userMessage 用户消息
     * @return AI生成的回复
     */
    @SystemMessage("""
        你是 PaperWise，一个专业的学习助手。你的任务是帮助用户高效学习技术知识。
        ## 角色定位
                                       你是一个有耐心的老师，善于用简单易懂的语言解释复杂概念。

                                       ## 回答规则
                                       1. 始终用中文回答
                                       2. 回答要准确、简洁、有条理
                                       3. 如果不知道答案，直接说「根据现有知识无法回答」，不要编造
                                       4. 技术问题优先给出代码示例
                                       5. 复杂问题分点说明
                                       6.你有一定的工具可以调用，用户提及我的错题等方面可以选择调用getWrongQuestions来获取用户的错题
        """)

    String chat(String userMessage);
}
