/**
 * 收藏夹管理控制器
 * <p>提供收藏夹的创建、卡片管理、分享等RESTful API</p>
 *
 * @author PaperWise Team
 */
package org.example.paperwise.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Interface.LookCount;
import org.example.paperwise.Interface.RateLimit;
import org.example.paperwise.Service.FavoritesService;
import org.example.paperwise.entry.Card;
import org.example.paperwise.entry.Favorites;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏夹控制器
 * <p>处理收藏夹的CRUD操作、卡片管理、分享复制等功能</p>
 */
@RestController
@RequestMapping("/paperwise/favorites")
public class FavoritesController {

    @Autowired
    public FavoritesService favoritesService;

    /**
     * 创建收藏夹
     * @param favoritesName 收藏夹名称
     * @param userid 用户ID
     * @return 创建的收藏夹
     */
    @PostMapping("/createfavorites")
    @RateLimit(key="createFavorites", WindowsSeconds = 1, MaxRequests = 300)
    public Result<Favorites> createFavorites(@RequestParam String favoritesName, @RequestAttribute Long userid) {
        return Result.success(favoritesService.createFavorites(userid, favoritesName));
    }

    /**
     * 添加卡片到收藏夹
     * @param favoritesId 收藏夹ID
     * @param cardId 卡片ID
     * @param userid 用户ID
     * @return 更新后的收藏夹
     */
    @PostMapping("/addcard")
    public Result<Favorites> addCard(@RequestParam Long favoritesId, @RequestParam Long cardId, @RequestAttribute Long userid) {
        return Result.success(favoritesService.addCard(favoritesId, userid, cardId));
    }

    /**
     * 删除收藏夹
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/deletefavorites")
    public Result<String> deleteFavorites(@RequestParam Long favoritesId, @RequestAttribute Long userid) {
        favoritesService.deleteFavorites(favoritesId, userid);
        return Result.success("删除成功");
    }

    /**
     * 从收藏夹移除卡片
     * @param cardId 卡片ID
     * @param userid 用户ID
     * @param favoritesId 收藏夹ID
     * @return 更新后的收藏夹
     */
    @DeleteMapping("/deletecard")
    public Result<Favorites> deleteCard(@RequestParam Long cardId, @RequestAttribute Long userid, @RequestParam Long favoritesId) {
        return Result.success(favoritesService.deleteCard(favoritesId, userid, cardId));
    }

    /**
     * 分页获取用户所有收藏夹
     * @param userid 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 分页收藏夹列表
     */
    @GetMapping("/getallfavorites")
    @Operation(summary = "分页返回用户所有收藏")
    public Result<Page<Favorites>> getAllFavorites(@RequestAttribute Long userid, @RequestParam int page, @RequestParam int size) {
        return Result.success(favoritesService.getAllFavorites(userid, size, page));
    }
    //生成分享链接
    @PostMapping("/getshareid")
    public Result<String> getShareId(@RequestParam Long favoritesId, @RequestAttribute Long userid) {
        return Result.success(favoritesService.getShareId(favoritesId, userid));
    }

    /**
     * 修改收藏夹是否公开
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     * @param isPublic 是否公开(0-私有, 1-公开)
     * @return 更新后的收藏夹
     */
    @PostMapping("/updateispublic")
    public Result<Favorites> updateIsPublic(@RequestParam Long favoritesId, @RequestAttribute Long userid, @RequestParam Integer isPublic) {
        return Result.success(favoritesService.updateIsPublic(favoritesId, userid, isPublic));
    }

    /**
     * 复制他人收藏夹
     * @param favoritesId 被复制的收藏夹ID
     * @param userid 当前用户ID
     * @return 复制后的收藏夹
     */
    @Operation(summary = "复制别人收藏夹")
    @PostMapping("/copyfavorites")
    @LookCount(value = "favoritesId")
    public Result<Favorites> CopyFavorites(@RequestParam Long favoritesId, @RequestAttribute Long userid) {
        return Result.success(favoritesService.CopyFavorites(favoritesId, userid));
    }

    /**
     * 批量添加卡片到收藏夹
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     * @param cardIds 卡片ID列表
     * @return 更新后的收藏夹
     */
    @Operation(summary = "批量添加题目到收藏")
    @PostMapping("/addAllCard")
    public Result<Favorites> addAllCard(@RequestParam Long favoritesId, @RequestAttribute Long userid, @RequestParam List<Long> cardIds) {
        return Result.success(favoritesService.addAllCard(favoritesId, userid, cardIds));
    }

    /**
     * 获取收藏夹中的所有卡片
     * @param cardIds 卡片ID列表
     * @return 卡片列表
     */
    @PostMapping("/getallcard")
    public Result<List<Card>> getAllCard(@RequestBody List<Long> cardIds) {
        return Result.success(favoritesService.getAllCard(cardIds));
    }

    /**
     * 获取公开收藏夹
     * @param favoritesId 收藏夹ID
     * @return 收藏夹信息
     */
    @GetMapping("/getpublicfavorites")
    @LookCount(value = "favoritesId")
    public Result<Favorites> getPublicFavorites(@RequestParam Long favoritesId) {
        return Result.success(favoritesService.getFavoritesById(favoritesId));
    }
}
