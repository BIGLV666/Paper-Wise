package org.example.paperwise.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Interface.RateLimit;
import org.example.paperwise.Service.FavoritesService;
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
    @RateLimit(key="createFavorites",WindowsSeconds = 10,MaxRequests = 3)
    public Result<String> createFavorites(@RequestParam String favoritesName, @RequestAttribute Long userId) {
        boolean r= favoritesService.createFavorites(userId,favoritesName);
        if(r){
            throw new RuntimeException("创建收藏夹失败");
        }
        return Result.success("success");
    }
    @PostMapping("/addcard")
    public Result<Favorites> addCard(@RequestParam Long favoritesId, @RequestParam Long cardId, @RequestAttribute Long userId) {
        return Result.success(favoritesService.addCard(favoritesId,userId,cardId));
    }
    @DeleteMapping("deletefavorites")
    public Result<String> deleteFavorites(@RequestParam Long favoritesId, @RequestAttribute Long userId) {
        favoritesService.deleteFavorites(favoritesId,userId);
        return Result.success("success");
    }
    @DeleteMapping("deletecard")
    public Result<Favorites>deleteCard(@RequestParam Long cardId, @RequestAttribute Long userId,@RequestParam Long favoritesId) {
        return Result.success(favoritesService.deleteCard(favoritesId,userId,cardId));
    }
    @GetMapping("getallfavorites")
    //@Operation(summary = "分页返回用户所有收藏，前端自定义页码和每页条数")
    public Result<Page<Favorites>>getAllFavorites(@RequestAttribute Long userId,@RequestParam int page,@RequestParam int size){
        return Result.success(favoritesService.getAllFavorites(userId,size,page));
    }
    //生成分享链接
    @PostMapping("getshareid")
    public Result<String>getShareId(@RequestParam Long favoritesId,@RequestAttribute Long userId){
        return Result.success(favoritesService.getShareId(favoritesId,userId));
    }
    /**
     * 修改收藏夹是否可分享
     * &#064;Param  Long favoritesId
     * @param userId Long
     */
    @PostMapping("updateispublic")
    public Result<Favorites> updateIsPublic(@RequestParam Long favoritesId,@RequestAttribute Long userId) {
        return Result.success(favoritesService.updateIsPublic(favoritesId,userId));
    }

    //复制别人收藏夹
    //@Operation(summary = "复制别人收藏夹并返回收藏夹对象")
    @PostMapping("copyfavorites")
    public Result<Favorites>CopyFavorites(@RequestParam Long favoritesId,@RequestAttribute Long userId) {
        return Result.success(favoritesService.CopyFavorites(favoritesId,userId));
    }
    //批量添加题目到收藏
    //@Operation(summary = "批量添加题目到收藏，返回收藏对象，前端实时更新，前端用URL拼接的方式，不能传JSON列表")
    @PostMapping("addAllCard")
    public Result<Favorites> addAllCard(@RequestParam Long favoritesId, @RequestAttribute Long userId,@RequestParam List<Long> cardIds) {
        return Result.success(favoritesService.addAllCard(favoritesId,userId,cardIds));
    }


}
