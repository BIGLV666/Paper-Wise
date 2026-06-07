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


    @Tool(name = "getWrongQuestions",value = "这个可以获取用户所有的错题，因为他是用户的错题集，用户提及我的错题或者和错相关可以调用")
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
