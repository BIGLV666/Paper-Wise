package org.example.paperwise.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.CardService;
import org.example.paperwise.entry.Card;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/paperwise/card")
public class CardController {
    @Autowired
    private CardService cardService;

    @PostMapping("/getcardbyid")
    public Result<Card>getCardById(@RequestAttribute Long userid){
        Card card = cardService.getCardById(userid);
        if(card==null){
            return Result.error("not found card");
        }
        return Result.success(card);
    }
    @PostMapping("addcard")
    public Result<Card>addCard(@RequestBody Card card,@RequestAttribute Long userid){
        try{
        Card cardDto=cardService.addCard(card, userid);
        if(cardDto==null){
            return Result.error("addcard error");
        }
        return Result.success(cardDto);
        }catch (Exception e){
            return Result.error(e.getMessage());
        }
    }
    @PutMapping("/updatecard")
    public Result<Card>updateCard(@RequestBody Card card,@RequestAttribute Long userid){
        try{
        Card cardDto=cardService.updateCard(card, userid);
        if(cardDto==null){
            return Result.error("update card error");
        }
        return Result.success(cardDto);
        }catch (Exception e){
            return Result.error(e.getMessage());
        }
    }
    @DeleteMapping("deletecard")
    public Result<String> deleteCard(@RequestParam Long card_id,@RequestAttribute Long userid){
        try {
            boolean r=cardService.deleteCard(card_id, userid);
            if(r){
                return Result.success("delete card success");
            }
            return Result.error("delete card error");
        }
        catch (Exception e){
            return Result.error(e.getMessage());
        }
    }
    //返回所有类别
    @GetMapping("/getallquestiontype")
    public Result<List<Map<String,Integer>>> getAllQuestionType(@RequestAttribute Long userid){
        List<Map<String,Integer>> list=cardService.getAllQuestionType(userid);
        return Result.success(list);
    }

    //分页查询按照类
    @PostMapping("getbyquestiontype")
    public Result<Page<Card>>getByQuestionType(@RequestAttribute Long userid, @RequestParam int page, @RequestParam int size, @RequestParam String question_type){
        Page<Card> pageCard=cardService.getByQuestionType(question_type,size,page, userid);
        return Result.success(pageCard);
    }

    //批量添加
    @PostMapping("/addallcard")
    public Result<String> addAllCard(@RequestBody List<Card>cards, @RequestAttribute Long userid) {

        try {
            int r = cardService.addAllCard(cards, userid);
            return Result.success("成功添加 " + r + " 条");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("解析失败: " + e.getMessage());
        }
    }
    @GetMapping("/getallcard")
    public Result<Page<Card>>geAllCard(@RequestAttribute Long userid, @RequestParam int page, @RequestParam int size){
        return Result.success(cardService.getAllCardsForPage(userid, size,page));
    }
    @GetMapping("/getcard")
    public Result<Card>getCardByCardId(@RequestParam Long cardId){
        Card card = cardService.getCardById(cardId);
        return Result.success(card);
    }
}
