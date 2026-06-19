/**
 * 收藏夹点赞控制器
 * <p>提供收藏夹点赞、取消点赞、查询点赞状态等功能</p>
 *
 * @author PaperWise Team
 */
package org.example.paperwise.Controller;

import io.swagger.v3.oas.annotations.Operation;
import org.example.paperwise.Dto.LikeFavoritesDto;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.FavoritesRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏夹点赞控制器
 * <p>处理收藏夹的点赞功能</p>
 */
@RestController
@RequestMapping("/paperwise/favoriteslikerecord")
public class FavoritesLikeRecordController {

    @Autowired
    private FavoritesRecordService favoritesRecordService;

    /**
     * 获取用户所有点赞的收藏夹
     * @param userid 用户ID
     * @return 点赞收藏夹列表
     */
    @Operation(summary = "获得用户所有的喜欢列表")
    @GetMapping("/getalllikefavoritesdto")
    public Result<List<LikeFavoritesDto>> getAllLikeFavoritesDto(@RequestAttribute Long userid) {
        return Result.success(favoritesRecordService.getAllLikeFavoritesDto(userid));
    }

    /**
     * 取消收藏夹点赞
     * @param userid 用户ID
     * @param favoritesId 收藏夹ID
     * @return 操作结果
     */
    @Operation(summary = "取消点赞")
    @PostMapping("/deletelike")
    public Result<String> deleteLike(@RequestAttribute Long userid, @RequestParam Long favoritesId) {
        favoritesRecordService.deleteLike(userid, favoritesId);
        return Result.success("取消成功");
    }

    /**
     * 给收藏夹点赞
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     * @return 操作结果
     */
    @PostMapping("/uplikecount")
    public Result<String> upLikeCount(@RequestParam Long favoritesId, @RequestAttribute Long userid) {
        favoritesRecordService.upLikeCount(userid, favoritesId);
        return Result.success("点赞成功");
    }

    /**
     * 检查用户是否已点赞
     * @param favoritesId 收藏夹ID
     * @param userid 用户ID
     * @return 是否已点赞
     */
    @GetMapping("/checklikestatus")
    public Result<Boolean> checkLikeStatus(@RequestParam Long favoritesId, @RequestAttribute Long userid) {
        boolean checkLikeStatus = favoritesRecordService.checkLikeStatus(userid, favoritesId);
        System.out.println(checkLikeStatus);
        return Result.success(!checkLikeStatus);
    }
}
