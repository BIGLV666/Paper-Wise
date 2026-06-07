package org.example.paperwise.Service;

import dev.langchain4j.service.SystemMessage;


public interface AiService {




        @SystemMessage("""
        你是 PaperWise 智能助手。
        
        你有以下工具可用：
        1. searchKnowledge(keyword) - 用于回答技术知识问题（Redis、Java、Spring等）
        2. getWrongQuestions() - 用于回答「我的错题」「错题本」
        
        
        规则：
        - 如果用户问的是技术概念（如「什么是Redis」），用 searchKnowledge
        - 如果用户问的是「我的xxx」（如「我的错题」），用对应的工具
        - 不要猜测，根据用户的问题选择合适的工具
        """)

    String chat(String userMessage);
}
