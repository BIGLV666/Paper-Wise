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

@RestController
@RequestMapping("/paperwise/favorites")
public class FavoritesController {
    @Autowired
    public FavoritesService favoritesService;

    @PostMapping("/createfavorites")
    @RateLimit(key="createFavorites",WindowsSeconds = 1,MaxRequests = 300)
    public Result<Favorites> createFavorites(@RequestParam String favoritesName, @RequestAttribute Long userid) {
        return Result.success(favoritesService.createFavorites(userid, favoritesName));}
    @PostMapping("/addcard")
    public Result<Favorites> addCard(@RequestParam Long favoritesId, @RequestParam Long cardId, @RequestAttribute Long userid) {
        return Result.success(favoritesService.addCard(favoritesId, userid,cardId));
    }
    @DeleteMapping("/deletefavorites")
    public Result<String> deleteFavorites(@RequestParam Long favoritesId, @RequestAttribute Long userid) {
        favoritesService.deleteFavorites(favoritesId, userid);
        return Result.success("success");
    }
    @DeleteMapping("/deletecard")
    public Result<Favorites>deleteCard(@RequestParam Long cardId, @RequestAttribute Long userid, @RequestParam Long favoritesId) {
        return Result.success(favoritesService.deleteCard(favoritesId, userid,cardId));
    }
    @GetMapping("/getallfavorites")
    @Operation(summary = "分页返回用户所有收藏，前端自定义页码和每页条数")
    public Result<Page<Favorites>>getAllFavorites(@RequestAttribute Long userid, @RequestParam int page, @RequestParam int size){
        return Result.success(favoritesService.getAllFavorites(userid,size,page));
    }
    //生成分享链接
    @PostMapping("/getshareid")
    public Result<String>getShareId(@RequestParam Long favoritesId,@RequestAttribute Long userid){
        return Result.success(favoritesService.getShareId(favoritesId, userid));
    }
    /**
     * 修改收藏夹是否可分享
     * &#064;Param  Long favoritesId
     * @param userid Long
     */
    @PostMapping("/updateispublic")
    public Result<Favorites> updateIsPublic(@RequestParam Long favoritesId,@RequestAttribute Long userid,@RequestParam Integer isPublic) {
        return Result.success(favoritesService.updateIsPublic(favoritesId,userid,isPublic));
    }

    //复制别人收藏夹
    @Operation(summary = "复制别人收藏夹并返回收藏夹对象")
    @PostMapping("/copyfavorites")
    @LookCount(value = "favoritesId")
    public Result<Favorites>CopyFavorites(@RequestParam Long favoritesId,@RequestAttribute Long userid) {
        return Result.success(favoritesService.CopyFavorites(favoritesId, userid));
    }
    //批量添加题目到收藏
    @Operation(summary = "批量添加题目到收藏，返回收藏对象，前端实时更新，前端用URL拼接的方式，不能传JSON列表")
    @PostMapping("/addAllCard")
    public Result<Favorites> addAllCard(@RequestParam Long favoritesId, @RequestAttribute Long userid, @RequestParam List<Long> cardIds) {
        return Result.success(favoritesService.addAllCard(favoritesId, userid,cardIds));
    }
    //返回收藏卡片
    @PostMapping("/getallcard")
    public Result<List<Card>>getAllCard(@RequestBody List<Long>cardIds){
        return Result.success(favoritesService.getAllCard(cardIds));
    }
    //单个查询公开收藏
    @GetMapping("/getpublicfavorites")
    @LookCount(value = "favoritesId")
    public Result<Favorites>getPublicFavorites(@RequestParam Long favoritesId){
        return Result.success(favoritesService.getFavoritesById(favoritesId));
    }
}
