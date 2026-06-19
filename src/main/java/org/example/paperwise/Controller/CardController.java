/**
 * 卡片管理控制器
 * <p>
 * 提供卡片（记忆卡/错题本）的增删改查等RESTful API接口
 * </p>
 *
 * @author PaperWise Team
 * @version 1.0
 * @since 2024-01-01
 */
package org.example.paperwise.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.CardService;
import org.example.paperwise.entry.Card;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 卡片（记忆卡/错题）控制器类
 * <p>
 * 处理所有与卡片相关的HTTP请求，包括：
 * <ul>
 *   <li>创建卡片（单个/批量）</li>
 *   <li>查询卡片（按ID/按类型/分页）</li>
 *   <li>更新卡片信息</li>
 *   <li>删除卡片</li>
 *   <li>获取所有卡片类型</li>
 * </ul>
 * </p>
 *
 * @RestController 组合@RestController和@ResponseBody，返回JSON格式数据
 * @RequestMapping("/paperwise/card") 统一前缀路径
 */
@RestController
@RequestMapping("/paperwise/card")
public class CardController {

    /** 卡片服务层依赖 */
    @Autowired
    private CardService cardService;

    /**
     * 获取当前用户的默认卡片
     * <p>
     * 获取当前用户关联的第一个卡片信息
     * </p>
     *
     * @param userid 用户ID（从Token中提取）
     * @return 卡片信息
     * @apiEndpoint POST /paperwise/card/getcardbyid
     */
    @PostMapping("/getcardbyid")
    public Result<Card> getCardById(@RequestAttribute Long userid) {
        try {
            Card card = cardService.getCardById(userid);
            if (card == null) {
                return Result.error("未找到卡片");
            }
            return Result.success(card);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 添加单个卡片
     * <p>
     * 为当前用户创建一个新的卡片记录
     * </p>
     *
     * @param card 卡片信息（JSON格式，包含题目、答案等）
     * @param userid 用户ID（从Token中提取）
     * @return 创建的卡片信息
     * @apiEndpoint POST /paperwise/card/addcard
     */
    @PostMapping("/addcard")
    public Result<Card> addCard(@RequestBody Card card, @RequestAttribute Long userid) {
        try {
            Card cardDto = cardService.addCard(card, userid);
            if (cardDto == null) {
                return Result.error("添加卡片失败");
            }
            return Result.success(cardDto);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新卡片信息
     * <p>
     * 修改指定卡片的题目、答案、类型等属性
     * </p>
     *
     * @param card 卡片信息（包含更新后的数据）
     * @param userid 用户ID（从Token中提取，用于权限验证）
     * @return 更新后的卡片信息
     * @apiEndpoint PUT /paperwise/card/updatecard
     */
    @PutMapping("/updatecard")
    public Result<Card> updateCard(@RequestBody Card card, @RequestAttribute Long userid) {
        try {
            Card cardDto = cardService.updateCard(card, userid);
            if (cardDto == null) {
                return Result.error("更新卡片失败");
            }
            return Result.success(cardDto);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除卡片
     * <p>
     * 根据卡片ID删除指定卡片（仅卡片所有者可删除）
     * </p>
     *
     * @param card_id 卡片ID
     * @param userid 用户ID（从Token中提取，用于权限验证）
     * @return 删除结果
     * @apiEndpoint DELETE /paperwise/card/deletecard
     */
    @DeleteMapping("/deletecard")
    public Result<String> deleteCard(@RequestParam Long card_id, @RequestAttribute Long userid) {
        try {
            boolean success = cardService.deleteCard(card_id, userid);
            if (success) {
                return Result.success("删除卡片成功");
            }
            return Result.error("删除卡片失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有卡片类型统计
     * <p>
     * 返回当前用户的所有卡片类型及每种类型的卡片数量
     * </p>
     *
     * @param userid 用户ID（从Token中提取）
     * @return 类型统计列表，格式如：[{typeName: "选择题", count: 10}, ...]
     * @apiEndpoint GET /paperwise/card/getallquestiontype
     */
    @GetMapping("/getallquestiontype")
    public Result<List<Map<String, Integer>>> getAllQuestionType(@RequestAttribute Long userid) {
        try {
            List<Map<String, Integer>> list = cardService.getAllQuestionType(userid);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 按类型分页查询卡片
     * <p>
     * 根据卡片类型进行分页查询，支持自定义每页数量和页码
     * </p>
     *
     * @param userid 用户ID（从Token中提取）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @param question_type 卡片类型（如：选择题、填空题等）
     * @return 分页卡片列表
     * @apiEndpoint POST /paperwise/card/getbyquestiontype
     */
    @PostMapping("/getbyquestiontype")
    public Result<Page<Card>> getByQuestionType(@RequestAttribute Long userid,
                                               @RequestParam int page,
                                               @RequestParam int size,
                                               @RequestParam String question_type) {
        try {
            Page<Card> pageCard = cardService.getByQuestionType(question_type, size, page, userid);
            return Result.success(pageCard);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量添加卡片
     * <p>
     * 一次性创建多条卡片记录，提高导入效率
     * </p>
     *
     * @param cards 卡片列表（JSON数组格式）
     * @param userid 用户ID（从Token中提取）
     * @return 成功添加的数量
     * @apiEndpoint POST /paperwise/card/addallcard
     */
    @PostMapping("/addallcard")
    public Result<String> addAllCard(@RequestBody List<Card> cards, @RequestAttribute Long userid) {
        try {
            int count = cardService.addAllCard(cards, userid);
            return Result.success("成功添加 " + count + " 条卡片");
        } catch (Exception e) {
            return Result.error("批量添加失败: " + e.getMessage());
        }
    }

    /**
     * 分页获取所有卡片
     * <p>
     * 获取当前用户的所有卡片，支持分页展示
     * </p>
     *
     * @param userid 用户ID（从Token中提取）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 分页卡片列表
     * @apiEndpoint GET /paperwise/card/getallcard
     */
    @GetMapping("/getallcard")
    public Result<Page<Card>> geAllCard(@RequestAttribute Long userid,
                                         @RequestParam int page,
                                         @RequestParam int size) {
        try {
            return Result.success(cardService.getAllCardsForPage(userid, size, page));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据卡片ID获取卡片详情
     * <p>
     * 通过卡片唯一标识获取单张卡片的完整信息
     * </p>
     *
     * @param cardId 卡片ID
     * @return 卡片信息
     * @apiEndpoint GET /paperwise/card/getcard
     */
    @GetMapping("/getcard")
    public Result<Card> getCardByCardId(@RequestParam Long cardId) {
        try {
            Card card = cardService.getCardById(cardId);
            return Result.success(card);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
