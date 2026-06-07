package org.example.paperwise.Until;

import dev.langchain4j.agent.tool.Tool;
import org.example.paperwise.Dto.WrongCardDto;
import org.example.paperwise.Service.WrongQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AITools {


    @Autowired
    private WrongQuestionService wrongQuestionService;

    @Tool(name = "getWrongQuestions",value = "获取用户的错题列表。返回每个错题的题目、错题次数。只要记录存在就代表是错题")
    public List<WrongCardDto> getWrongQuestionDto() {

        Long userId = UserContext.getUserId();
        System.out.println(userId);
        if(userId == null) {
            throw new RuntimeException("未登录");
        }
        System.out.println(wrongQuestionService.getWrongQuestionsDto(userId));
        return  wrongQuestionService.getWrongQuestionsDto(userId);
    }
}
